/*
 *  IMAP.java
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

import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Store;
import java.util.Properties;

/**
 *
 * @author franci
 */
public class IMAP implements MailReader
{
    private static final String IMAP = "imap";
    private static final String MAIL_STORE_PROTOCOL = "mail.store.protocol";
    private static final String MAIL_IMAP_SSL_ENABLE = "mail.imap.ssl.enable";
    private static final String MAIL_IMAP_PORT = "mail.imap.port";
    private static final String MAIL_IMAP_HOST = "mail.imap.host";

    public static final int SAFE_PORT_993 = 993;

    private volatile String host;
    private volatile int port = 993;
//    private volatile boolean auth;
    private volatile boolean sslEnable;
    private volatile boolean readonly;
    
    private volatile String username;
    private volatile String password;

    private volatile Store store;
    private volatile Folder inbox;

    @Override
    public void setHost(String value)
    {
        this.host = value;
    }

    @Override
    public void setPort(int value)
    {
        this.port = value;
    }

    @Override
    public void setSslEnable(boolean value)
    {
        this.sslEnable = value;
    }

    @Override
    public void setReadonly(boolean value)
    {
        this.readonly = value;
    }

    @Override
    public void setUsername(String value)
    {
        this.username = value;
    }

    @Override
    public void setPassword(String value)
    {
        this.password = value;
    }
    
    //hay que hacer que imap y pop3 hereden de la misma interfaz y con esto se puede hacer lo mismo con ambas una vez inicializado
    @Override
    public void connect() throws Exception
    {
        Properties props = new Properties();
        props.put(MAIL_STORE_PROTOCOL, IMAP);
        props.put(MAIL_IMAP_HOST, host);
        props.put(MAIL_IMAP_PORT, Integer.toString(port));
        props.put(MAIL_IMAP_SSL_ENABLE, sslEnable?"true":"false"); // enables SSL

        Session session = Session.getInstance(props);
        store = session.getStore(IMAP);
        store.connect(host, username, password);

        inbox = store.getFolder("INBOX");
        inbox.open(readonly ? Folder.READ_ONLY:Folder.READ_WRITE);
    }

    @Override
    public Message[] getMessages() throws Exception
    {
       return inbox.getMessages();
    }

    @Override
    public void close() throws Exception
    {
        inbox.close(false);
        store.close();
    }
}
