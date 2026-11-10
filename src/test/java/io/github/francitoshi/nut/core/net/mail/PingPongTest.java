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
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Alice and bob trade a number by mail: alice starts with 0 and every player
 * answers with the number it received plus one, until reaching 7. Played once
 * over SMTP/IMAP and once over SMTP/POP3.
 *
 * @author franci
 */
public class PingPongTest
{
    /** The play stops when this number is reached. */
    private static final int LAST_NUMBER = 7;

    private static final String MAIL_BODY = "ping-pong round";

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP_POP3_IMAP)
            .withConfiguration(GreenMailConfiguration.aConfig()
            .withUser(ALICE_LOCALHOST, ALICE, ALICE_PASS)
            .withUser(BOB_LOCALHOST, BOB, BOB_PASS));

    /**
     * Reads the subjects currently visible in a player mailbox. IMAP sees new
     * mail in a persistent session; POP3 needs a fresh session per read.
     */
    private interface Mailbox
    {
        List<String> subjects() throws Exception;
    }

    /**
     * One player: an SMTP connection to send with and its mailbox to read.
     */
    private static class Player implements AutoCloseable
    {
        final String otherAddress;
        final SMTP smtp;
        final Mailbox probe;
        final MailReader owned; // persistent mailbox, or null when probed per read

        Player(String otherAddress, SMTP smtp, Mailbox probe, MailReader owned)
        {
            this.otherAddress = otherAddress;
            this.smtp = smtp;
            this.probe = probe;
            this.owned = owned;
        }

        void send(int number) throws MessagingException
        {
            smtp.send(String.valueOf(number), MAIL_BODY + " " + number, otherAddress);
        }

        @Override
        public void close()
        {
            if(owned != null)
            {
                owned.close();
            }
            smtp.close();
        }
    }

    /**
     * Builds a player with its persistent SMTP connection and mailbox probe.
     *
     * @param owned persistent mailbox of the player, or {@code null} when the
     *              probe opens its own sessions.
     */
    private Player newPlayer(String username, String password, String mailboxAddress, String otherAddress, Mailbox probe, MailReader owned) throws Exception
    {
        SMTP smtp = new SMTP(greenMail.getSmtp().getBindTo(), greenMail.getSmtp().getPort(),
                false, false, username, new SecureChars(password.toCharArray()), mailboxAddress).connect();
        return new Player(otherAddress, smtp, probe, owned);
    }

    /**
     * Plays the game: the first player sends 0 and the other answers with 1,
     * alternating players until {@link #LAST_NUMBER} is reached.
     */
    private void playPingPong(Player player, Player other) throws Exception
    {
        int number = 0;
        Player sender = player;
        Player receiver = other;
        while(true)
        {
            sender.send(number);
            int received = awaitNumber(receiver, number);
            if(received == LAST_NUMBER)
            {
                break;
            }
            number = received + 1;
            // alternate turns
            Player swap = sender;
            sender = receiver;
            receiver = swap;
        }
        // each player saw its half of the rounds (0..7)
        assertEquals((LAST_NUMBER + 1) / 2, receiver.probe.subjects().size());
        assertEquals((LAST_NUMBER + 1) / 2, sender.probe.subjects().size());
    }

    /**
     * Polls the player mailbox until the message with the given number
     * arrives.
     *
     * @param number the number the other party was supposed to send.
     * @return the received number, which is asserted to be {@code number}.
     */
    private int awaitNumber(Player player, int number) throws Exception
    {
        long deadline = System.currentTimeMillis() + 30_000;
        while(System.currentTimeMillis() < deadline)
        {
            if(player.probe.subjects().contains(String.valueOf(number)))
            {
                return number;
            }
            Thread.sleep(20);
        }
        fail("never received number " + number);
        return -1; // unreachable
    }

    /**
     * Collects the subjects of every message in the mailbox.
     */
    private static List<String> subjects(MailReader reader) throws MessagingException
    {
        List<String> list = new ArrayList<>();
        for(Message message : reader.getMessages())
        {
            list.add(message.getSubject());
        }
        return list;
    }

    /**
     * Runs the play over IMAP: both mailboxes stay open and see new mail
     * thanks to server side updates.
     */
    @Test
    void testPingPongViaIMAP() throws Exception
    {
        String imapHost = greenMail.getImap().getBindTo();
        int imapPort = greenMail.getImap().getPort();

        MailReader aliceInbox = new IMAP(imapHost, imapPort, false, false, false, ALICE, new SecureChars(ALICE_PASS.toCharArray()));
        MailReader bobInbox = new IMAP(imapHost, imapPort, false, false, false, BOB, new SecureChars(BOB_PASS.toCharArray()));
        aliceInbox.connect();
        bobInbox.connect();

        try( Player alice = newPlayer(ALICE, ALICE_PASS, ALICE_LOCALHOST, BOB_LOCALHOST, () -> subjects(aliceInbox), aliceInbox);
             Player bob = newPlayer(BOB, BOB_PASS, BOB_LOCALHOST, ALICE_LOCALHOST, () -> subjects(bobInbox), bobInbox) )
        {
            playPingPong(alice, bob);
        }
    }

    /**
     * Runs the same play over POP3. POP3 reports the mailbox content once per
     * session (STAT), so every probe opens a fresh session.
     */
    @Test
    void testPingPongViaPOP3() throws Exception
    {
        String pop3Host = greenMail.getPop3().getBindTo();
        int pop3Port = greenMail.getPop3().getPort();

        Mailbox aliceMailbox = () -> {
            try( MailReader inbox = new POP3(pop3Host, pop3Port, false, false, false, ALICE, new SecureChars(ALICE_PASS.toCharArray())) )
            {
                inbox.connect();
                return subjects(inbox);
            }
        };
        Mailbox bobMailbox = () -> {
            try( MailReader inbox = new POP3(pop3Host, pop3Port, false, false, false, BOB, new SecureChars(BOB_PASS.toCharArray())) )
            {
                inbox.connect();
                return subjects(inbox);
            }
        };

        try( Player alice = newPlayer(ALICE, ALICE_PASS, ALICE_LOCALHOST, BOB_LOCALHOST, aliceMailbox, null);
             Player bob = newPlayer(BOB, BOB_PASS, BOB_LOCALHOST, ALICE_LOCALHOST, bobMailbox, null) )
        {
            playPingPong(alice, bob);
        }
    }
}
