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
