/*
 *  MailReader.java
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
