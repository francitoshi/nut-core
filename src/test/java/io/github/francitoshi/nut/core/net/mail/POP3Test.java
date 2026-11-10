/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.core.net.mail;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import io.github.francitoshi.nut.base.commons.security.SecureChars;
import static io.github.francitoshi.nut.core.net.mail.SMTPTest.ALICE;
import static io.github.francitoshi.nut.core.net.mail.SMTPTest.ALICE_LOCALHOST;
import static io.github.francitoshi.nut.core.net.mail.SMTPTest.ALICE_PASS;
import static io.github.francitoshi.nut.core.net.mail.SMTPTest.BOB;
import static io.github.francitoshi.nut.core.net.mail.SMTPTest.BOB_LOCALHOST;
import static io.github.francitoshi.nut.core.net.mail.SMTPTest.BOB_PASS;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import java.util.Date;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Integration tests of {@link POP3} against an in-memory GreenMail server.
 *
 * @author franci
 */
public class POP3Test
{
    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP_POP3)
            .withConfiguration(GreenMailConfiguration.aConfig()
            .withUser(ALICE_LOCALHOST, ALICE, ALICE_PASS)
            .withUser(BOB_LOCALHOST, BOB, BOB_PASS));

    private static final String SUBJECT_ONE = "POP3 Subject One";
    private static final String SUBJECT_TWO = "POP3 Subject Two";
    private static final String THIS_IS_THE_EMAIL_BODY = "This is the POP3 email body!";

    private void send(String subject) throws MessagingException
    {
        String host = greenMail.getSmtp().getBindTo();
        int port = greenMail.getSmtp().getPort();
        try( SMTP smtp = new SMTP(host, port, false, false, ALICE, new SecureChars(ALICE_PASS.toCharArray()), ALICE_LOCALHOST).connect() )
        {
            smtp.send(subject, THIS_IS_THE_EMAIL_BODY, BOB_LOCALHOST);
        }
    }

    private MailReader connectedPOP3() throws Exception
    {
        MailReader pop3 = new POP3(greenMail.getPop3().getBindTo(), greenMail.getPop3().getPort(),
                false, false, false, BOB, new SecureChars(BOB_PASS.toCharArray()));
        pop3.connect();
        return pop3;
    }

    @Test
    void testIsConnectedLifecycle() throws MessagingException, Exception
    {
        send(SUBJECT_ONE);

        POP3 pop3 = new POP3(greenMail.getPop3().getBindTo(), greenMail.getPop3().getPort(),
                false, false, false, BOB, new SecureChars(BOB_PASS.toCharArray()));

        assertFalse(pop3.isConnected());

        pop3.connect();
        assertTrue(pop3.isConnected());

        pop3.close();
        assertFalse(pop3.isConnected());
    }

    @Test
    void testGetMessages() throws MessagingException, Exception
    {
        send(SUBJECT_ONE);
        greenMail.waitForIncomingEmail(5000, 1);

        try( MailReader pop3 = connectedPOP3() )
        {
            Message[] messages = pop3.getMessages();
            assertEquals(1, messages.length);
            assertEquals(SUBJECT_ONE, messages[0].getSubject());
            assertEquals(BOB_LOCALHOST, messages[0].getAllRecipients()[0].toString());
        }
    }

    @Test
    void testGetMessagesSinceNullReturnsAll() throws MessagingException, Exception
    {
        send(SUBJECT_ONE);
        send(SUBJECT_TWO);
        greenMail.waitForIncomingEmail(5000, 2);

        try( MailReader pop3 = connectedPOP3() )
        {
            Message[] messages = pop3.getMessages();
            assertEquals(2, messages.length);

            messages = pop3.getMessages(null);
            assertEquals(2, messages.length);
        }
    }

    @Test
    void testGetMessagesSinceFutureReturnsNone() throws MessagingException, Exception
    {
        send(SUBJECT_ONE);
        greenMail.waitForIncomingEmail(5000, 1);

        try( MailReader pop3 = connectedPOP3() )
        {
            Message[] messages = pop3.getMessages(new Date(System.currentTimeMillis() + 60_000));
            assertEquals(0, messages.length);
        }
    }

    @Test
    void testGetMessagesSinceKeepsOnlyNewerMessages() throws MessagingException, Exception
    {
        send(SUBJECT_ONE);
        greenMail.waitForIncomingEmail(5000, 1);

        // POP3 messages have no received date and the Date (sent) header has
        // second granularity, so the boundary is aligned to whole seconds:
        // start at the second of the first mail and wait for the next one.
        long second = (System.currentTimeMillis() / 1000) * 1000;
        Date since = new Date(second + 999);
        while(System.currentTimeMillis() < second + 1000)
        {
            Thread.sleep(10);
        }

        send(SUBJECT_TWO);
        greenMail.waitForIncomingEmail(5000, 2);

        try( MailReader pop3 = connectedPOP3() )
        {
            Message[] messages = pop3.getMessages(since);
            assertEquals(1, messages.length);
            assertEquals(SUBJECT_TWO, messages[0].getSubject());
        }
    }
}
