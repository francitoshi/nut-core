/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.core.net.mail;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests of {@link MailUtils#sortByReceivedDate(Message[])}.
 *
 * @author franci
 */
public class MailUtilsTest
{
    private static final Session SESSION = Session.getInstance(new Properties());

    /**
     * {@link MimeMessage} with an overridable received date.
     */
    private static class TestMessage extends MimeMessage
    {
        private final Date received;

        TestMessage(Date received, Date sent) throws MessagingException
        {
            super(SESSION);
            this.received = received;
            if(sent != null)
            {
                setSentDate(sent);
            }
        }

        @Override
        public Date getReceivedDate()
        {
            return this.received;
        }
    }

    /**
     * {@link MimeMessage} whose received date fails to read, like a message
     * from a broken or closed folder.
     */
    private static class BrokenMessage extends MimeMessage
    {
        BrokenMessage() throws MessagingException
        {
            super(SESSION);
        }

        @Override
        public Date getReceivedDate() throws MessagingException
        {
            throw new MessagingException("boom");
        }
    }

    @Test
    void testSortByReceivedDateReturnsSameArraySorted() throws MessagingException
    {
        TestMessage older = new TestMessage(new Date(1_000), null);
        TestMessage middle = new TestMessage(new Date(2_000), null);
        TestMessage newer = new TestMessage(new Date(3_000), null);

        TestMessage[] items = {newer, older, middle};

        assertSame(items, MailUtils.sortByReceivedDate(items));
        assertArrayEquals(new Message[]{older, middle, newer}, items);
    }

    @Test
    void testSortFallsBackToSentDate() throws MessagingException
    {
        TestMessage bySent = new TestMessage(null, new Date(2_000));
        TestMessage byReceived = new TestMessage(new Date(3_000), null);
        TestMessage earlier = new TestMessage(null, new Date(1_000));

        TestMessage[] items = {byReceived, bySent, earlier};

        MailUtils.sortByReceivedDate(items);
        assertArrayEquals(new Message[]{earlier, bySent, byReceived}, items);
    }

    @Test
    void testSortPutsUndatedFirst() throws MessagingException
    {
        TestMessage undated = new TestMessage(null, null);
        TestMessage dated = new TestMessage(new Date(1_000), null);

        TestMessage[] items = {dated, undated};

        MailUtils.sortByReceivedDate(items);
        assertArrayEquals(new Message[]{undated, dated}, items);
    }

    @Test
    void testSortPutsUnreadableDatesFirst() throws MessagingException
    {
        BrokenMessage broken = new BrokenMessage();
        TestMessage dated = new TestMessage(new Date(1_000), null);

        Message[] items = {dated, broken};

        MailUtils.sortByReceivedDate(items);
        assertArrayEquals(new Message[]{broken, dated}, items);
    }

    @Test
    void testSortEmptyAndSingleElementArrays() throws MessagingException
    {
        assertEquals(0, MailUtils.sortByReceivedDate(new Message[0]).length);

        TestMessage single = new TestMessage(new Date(1_000), null);
        TestMessage[] items = new TestMessage[]{single};
        assertSame(items, MailUtils.sortByReceivedDate(items));
        assertEquals(1, items.length);
    }
}
