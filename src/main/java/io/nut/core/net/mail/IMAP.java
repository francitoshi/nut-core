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
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.ReceivedDateTerm;
import jakarta.mail.search.SearchTerm;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    private final String host;
    private final int port;
    private final boolean auth;
    private final boolean sslEnable;
    private final boolean readonly;
    private final String username;
    private final String password;
    
    private volatile Store store;
    private volatile Folder inbox;

    public IMAP(String host, int port, boolean auth, boolean sslEnable, boolean readonly, String username, String password)
    {
        this.host = host;
        this.port = port;
        this.auth = auth;
        this.sslEnable = sslEnable;
        this.readonly = readonly;
        this.username = username;
        this.password = password;
    }

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
    public Message[] getMessages() throws MessagingException
    {
       return inbox.getMessages();
    }

    @Override
    public Message[] getMessages(Date after) throws MessagingException
    {
        SearchTerm dateTerm = new ReceivedDateTerm(ComparisonTerm.GT, after);
        return inbox.search(dateTerm);
    }

    @Override
    public void close() 
    {
        try
        {
            inbox.close(false);
            store.close();
        }
        catch (MessagingException ex)
        {
            Logger.getLogger(IMAP.class.getName()).log(Level.SEVERE, (String) null, ex);
        }
    }
}
