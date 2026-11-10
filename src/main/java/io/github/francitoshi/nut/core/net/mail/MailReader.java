/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.core.net.mail;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import java.io.Closeable;
import java.util.Date;

/**
 * Common contract for reading a mailbox, implemented by {@link IMAP} and
 * {@link POP3}. Instances must be {@link #connect() connected} before reading
 * and {@link #close() closed} when no longer needed.
 *
 * @author franci
 */
public interface MailReader extends Closeable
{
    /**
     * @return {@code true} while the underlying connection is alive.
     */
    boolean isConnected();

    /**
     * Opens the connection and the mailbox; must be called before reading.
     */
    void connect() throws Exception;

    /**
     * @return every message currently in the mailbox.
     */
    Message[] getMessages() throws MessagingException;

    /**
     * @param since only return messages received after this instant, or all
     *              of them when {@code null}.
     * @return the messages received after {@code since}.
     */
    Message[] getMessages(Date since) throws MessagingException;

    /**
     * Closes the mailbox and the connection without expunging anything.
     */
    @Override
    void close();
}
