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

/**
 *
 * @author franci
 */
public interface MailReader
{
    void setHost(String host);
    void setPort(int port);
    void setSslEnable(boolean sslEnable);
    void setUsername(String username);
    void setPassword(String password);
    void setReadonly(boolean readonly);
    
    void connect() throws Exception;
    Message[] getMessages() throws Exception;
    void close() throws Exception;
}
