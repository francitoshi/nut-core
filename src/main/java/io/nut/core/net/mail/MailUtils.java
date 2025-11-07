/*
 *  MailUtils.java
 *
 *  Copyright (C) 2025 francitoshi@gmail.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Report bugs or new features to: francitoshi@gmail.com
 *
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
                return m.getSentDate();
            } 
            catch (MessagingException e) 
            {
                return new Date(0);
            }
        }));
        return items;
    }
}
