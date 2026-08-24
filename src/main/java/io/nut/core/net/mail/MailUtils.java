/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.core.net.mail;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

/**
 *
 * @author franci
 */
public class MailUtils
{
    public static Message[] sortByReceivedDate(Message[] items)
    {
        Arrays.sort(items, Comparator.comparing(m -> 
        {
            try 
            {
                return m.getReceivedDate();
            } 
            catch (MessagingException e) 
            {
                return new Date(0);
            }
        }));
        return items;
    }
}
