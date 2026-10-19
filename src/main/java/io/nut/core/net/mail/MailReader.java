/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.core.net.mail;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import java.io.Closeable;
import java.util.Date;

/**
 *
 * @author franci
 */
public interface MailReader extends Closeable
{
    
    boolean isConnected();
    void connect() throws Exception;
    Message[] getMessages() throws MessagingException;
    Message[] getMessages(Date since) throws MessagingException;
    
    @Override
    void close();
}
