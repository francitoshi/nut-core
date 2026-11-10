/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.core.net.mail;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

/**
 * Helper utilities for working with {@link Message}s.
 *
 * @author franci
 */
public class MailUtils
{
    /**
     * Sorts the array in place, oldest first, and returns it. The sort key is
     * the received date, falling back to the sent date; messages without any
     * date sort first.
     *
     * @param items messages to sort; must not contain {@code null} elements.
     * @return the same array, sorted.
     */
    public static Message[] sortByReceivedDate(Message[] items)
    {
        Arrays.sort(items, Comparator.comparing(m -> 
        {
            try 
            {
                Date received = m.getReceivedDate();
                return received != null ? received : m.getSentDate();
            } 
            catch (MessagingException e) 
            {
                return new Date(0);
            }
        }, Comparator.nullsFirst(Comparator.naturalOrder())));
        return items;
    }
}
