/*
 *  MailReaderTest.java
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
import io.nut.base.security.SecureChars;
import static io.nut.core.net.mail.SMTPTest.ALICE;
import static io.nut.core.net.mail.SMTPTest.ALICE_LOCALHOST;
import static io.nut.core.net.mail.SMTPTest.ALICE_PASS;
import static io.nut.core.net.mail.SMTPTest.BOB;
import static io.nut.core.net.mail.SMTPTest.BOB_LOCALHOST;
import static io.nut.core.net.mail.SMTPTest.BOB_PASS;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 *
 * @author franci
 */
public class MailReaderTest
{
    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP_POP3_IMAP)
            .withConfiguration(GreenMailConfiguration.aConfig()
            .withUser(ALICE_LOCALHOST, ALICE, ALICE_PASS)
            .withUser(BOB_LOCALHOST, BOB, BOB_PASS));

    @Test
    void testSendEmail() throws MessagingException, Exception 
    {
        String host = greenMail.getSmtp().getBindTo();
        int port = greenMail.getSmtp().getPort();

        try( SMTP smtp = new SMTP(host, port, false, false, ALICE, new SecureChars(ALICE_PASS.toCharArray()), ALICE_LOCALHOST).connect() )
        {
            smtp.send(TEST_SUBJECT, THIS_IS_THE_EMAIL_BODY, BOB_LOCALHOST);
        }

        host = greenMail.getImap().getBindTo();
        port = greenMail.getImap().getPort();
        
        try( MailReader mr = new IMAP(host, port, false, false, false, BOB, new SecureChars(BOB_PASS.toCharArray())) )
        {
            mr.connect();
            Message[] messages = mr.getMessages();
            assertEquals(1, messages.length);
            assertEquals(TEST_SUBJECT, messages[0].getSubject());
            assertEquals(BOB_LOCALHOST, messages[0].getAllRecipients()[0].toString());

        }

        host = greenMail.getPop3().getBindTo();
        port = greenMail.getPop3().getPort();
        
        try( MailReader mr = new POP3(host, port, false, false, false, BOB, new SecureChars(BOB_PASS.toCharArray())) )
        {
            mr.connect();
            Message[] messages = mr.getMessages();
            assertEquals(1, messages.length);
            assertEquals(TEST_SUBJECT, messages[0].getSubject());
            assertEquals(BOB_LOCALHOST, messages[0].getAllRecipients()[0].toString());
        }
    }
    
    private static final String TEST_SUBJECT = "Test Subject";
    private static final String THIS_IS_THE_EMAIL_BODY = "This is the email body!";
}
