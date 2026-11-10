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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Integration tests of {@link IMAP} against an in-memory GreenMail server.
 *
 * @author franci
 */
public class IMAPTest
{
    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP_IMAP)
            .withConfiguration(GreenMailConfiguration.aConfig()
            .withUser(ALICE_LOCALHOST, ALICE, ALICE_PASS)
            .withUser(BOB_LOCALHOST, BOB, BOB_PASS));

    private static final String TEST_SUBJECT = "IMAP Test Subject";
    private static final String THIS_IS_THE_EMAIL_BODY = "This is the IMAP email body!";

    private void send(String subject) throws MessagingException
    {
        String host = greenMail.getSmtp().getBindTo();
        int port = greenMail.getSmtp().getPort();
        try( SMTP smtp = new SMTP(host, port, false, false, ALICE, new SecureChars(ALICE_PASS.toCharArray()), ALICE_LOCALHOST).connect() )
        {
            smtp.send(subject, THIS_IS_THE_EMAIL_BODY, BOB_LOCALHOST);
        }
    }

    private MailReader connectedIMAP() throws Exception
    {
        MailReader imap = new IMAP(greenMail.getImap().getBindTo(), greenMail.getImap().getPort(),
                false, false, false, BOB, new SecureChars(BOB_PASS.toCharArray()));
        imap.connect();
        return imap;
    }

    @Test
    void testIsConnectedLifecycle() throws MessagingException, Exception
    {
        send(TEST_SUBJECT);

        IMAP imap = new IMAP(greenMail.getImap().getBindTo(), greenMail.getImap().getPort(),
                false, false, false, BOB, new SecureChars(BOB_PASS.toCharArray()));

        assertFalse(imap.isConnected());

        imap.connect();
        assertTrue(imap.isConnected());

        imap.close();
        assertFalse(imap.isConnected());
    }

    @Test
    void testConnectRefusesAuthWithoutEncryption() throws MessagingException
    {
        IMAP imap = new IMAP(greenMail.getImap().getBindTo(), greenMail.getImap().getPort(),
                true, false, false, BOB, new SecureChars(BOB_PASS.toCharArray()));

        assertThrows(MessagingException.class, imap::connect);
        assertFalse(imap.isConnected());
    }

    @Test
    void testGetMessages() throws MessagingException, Exception
    {
        send(TEST_SUBJECT);
        greenMail.waitForIncomingEmail(5000, 1);

        try( MailReader imap = connectedIMAP() )
        {
            Message[] messages = imap.getMessages();
            assertEquals(1, messages.length);
            assertEquals(TEST_SUBJECT, messages[0].getSubject());
            assertEquals(BOB_LOCALHOST, messages[0].getAllRecipients()[0].toString());
        }
    }

    @Test
    void testGetMessagesSinceNullReturnsAll() throws MessagingException, Exception
    {
        send(TEST_SUBJECT);
        send(TEST_SUBJECT);
        greenMail.waitForIncomingEmail(5000, 2);

        try( MailReader imap = connectedIMAP() )
        {
            Message[] messages = imap.getMessages();
            assertEquals(2, messages.length);

            messages = imap.getMessages(null);
            assertEquals(2, messages.length);
        }
    }

    @Test
    void testGetMessagesSinceEpochReturnsAll() throws MessagingException, Exception
    {
        send(TEST_SUBJECT);
        greenMail.waitForIncomingEmail(5000, 1);

        try( MailReader imap = connectedIMAP() )
        {
            Message[] messages = imap.getMessages(new Date(0));
            assertEquals(1, messages.length);
        }
    }

    @Test
    void testGetMessagesSinceFutureReturnsNone() throws MessagingException, Exception
    {
        send(TEST_SUBJECT);
        greenMail.waitForIncomingEmail(5000, 1);

        try( MailReader imap = connectedIMAP() )
        {
            // IMAP date search is day-granular: another day keeps it exact
            Message[] messages = imap.getMessages(new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)));
            assertEquals(0, messages.length);
        }
    }

    @Test
    void testIdleDeliversMessagesToListener() throws Exception
    {
        AtomicReference<String> received = new AtomicReference<>();
        AtomicReference<Long> lastUID = new AtomicReference<>();
        AtomicReference<Long> uidValidity = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        IMAP imap = new IMAP(greenMail.getImap().getBindTo(), greenMail.getImap().getPort(),
                false, false, false, BOB, new SecureChars(BOB_PASS.toCharArray()));

        imap.setImapListener((message, uidValid, uid) -> {
            try
            {
                received.set(message.getSubject());
            }
            catch(MessagingException ex)
            {
                received.set(null);
            }
            lastUID.set(uid);
            uidValidity.set(uidValid);
            latch.countDown();
        }, 0, 0);

        Thread idleWorker = new Thread(() -> {
            try
            {
                imap.idle();
            }
            catch(Exception ex)
            {
                // the connection is closed at the end of the test: ignore
            }
        });

        try
        {
            imap.connect();
            idleWorker.start();

            send(TEST_SUBJECT);

            assertTrue(latch.await(10, TimeUnit.SECONDS));
            assertEquals(TEST_SUBJECT, received.get());
            assertTrue(lastUID.get() > 0);
            assertTrue(uidValidity.get() > 0);
        }
        finally
        {
            imap.close();
        }

        idleWorker.join(5_000);
        assertFalse(idleWorker.isAlive());
    }
}
