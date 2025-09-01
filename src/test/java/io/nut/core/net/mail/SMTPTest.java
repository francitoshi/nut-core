/*
 *  SMTPTest.java
 *
 *  Copyright (C) 2025 francitoshi@gmail.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Report bugs or new features to: francitoshi@gmail.com
 *
 */
package io.nut.core.net.mail;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import io.nut.base.crypto.Kripto;
import io.nut.base.crypto.Rand;
import io.nut.base.security.SecureChars;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 *
 * @author franci
 */
public class SMTPTest
{
    static final Rand RAND = Kripto.getRand();
    
    static final String ALICE = "alice";
    static final String BOB = "bob";
    
    static final String ALICE_PASS = "alice-pass"+RAND.nextLong();
    static final String BOB_PASS = "bob-pass"+RAND.nextLong();

    static final String ALICE_LOCALHOST = "alice@localhost";
    static final String BOB_LOCALHOST = "bob@localhost";
    
    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP_IMAP)
            .withConfiguration(GreenMailConfiguration.aConfig().withUser(ALICE_LOCALHOST, ALICE, ALICE_PASS).withUser(BOB_LOCALHOST, BOB, BOB_PASS));
    
    @Test
    void testSendEmail() throws MessagingException 
    {
        String host = greenMail.getSmtp().getBindTo();
        int port = greenMail.getSmtp().getPort();

        try( SMTP smtp = new SMTP(host, port, false, false, ALICE, new SecureChars(ALICE_PASS.toCharArray()), ALICE_LOCALHOST).connect() )
        {
            smtp.send(TEST_SUBJECT, THIS_IS_THE_EMAIL_BODY, BOB_LOCALHOST);
        }
        
        assertTrue(greenMail.waitForIncomingEmail(5000, 1));

        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        
        assertEquals(1, receivedMessages.length);

        MimeMessage receivedMessage = receivedMessages[0];
        assertEquals(TEST_SUBJECT, receivedMessage.getSubject());
        assertEquals(BOB_LOCALHOST, receivedMessage.getAllRecipients()[0].toString());
    }
    
    private static final String TEST_SUBJECT = "Test Subject";
    private static final String THIS_IS_THE_EMAIL_BODY = "This is the email body!";
    
}
