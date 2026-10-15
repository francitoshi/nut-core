/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
-* See LICENSE file in the project root for full license text.
 */
package io.nut.core.net.mail;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import io.nut.base.commons.security.SecureChars;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 *
 * @author franci
 */
public class SMTPTest
{
    static final Random RANDOM = ThreadLocalRandom.current();
    
    static final String ALICE = "alice";
    static final String BOB = "bob";
    
    static final String ALICE_PASS = "alice-pass"+RANDOM.nextLong();
    static final String BOB_PASS = "bob-pass"+RANDOM.nextLong();

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
