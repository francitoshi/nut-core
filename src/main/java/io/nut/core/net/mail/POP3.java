/*
 *  POP3.java
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

import io.nut.base.security.SecureChars;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author franci
 */
public class POP3 implements MailReader
{
    private static final String POP3 = "pop3";
    private static final String MAIL_POP3_HOST = "mail.pop3.host";
    private static final String MAIL_POP3_PORT = "mail.pop3.port";
    private static final String MAIL_POP3_AUTH = "mail.pop3.auth";
    private static final String MAIL_POP3_SSL_ENABLE = "mail.pop3.ssl.enable";

    public static final int SAFE_PORT_995 = 995;
    
    private final Object lock = new Object();
    
    private final String host;
    private final int port;
    private final boolean auth;
    private final boolean sslEnable;
    private final boolean readonly;
    private final String username;
    private final SecureChars password;
    
    private volatile Store store;
    private volatile Folder inbox;

    public POP3(String host, int port, boolean auth, boolean sslEnable, boolean readonly, String username, SecureChars password)
    {
        this.host = host;
        this.port = port;
        this.auth = auth;
        this.sslEnable = sslEnable;
        this.readonly = readonly;
        this.username = username;
        this.password = password;
    }
    public POP3(String host, int port, boolean auth, boolean sslEnable, boolean readonly, String username, char[] password)
    {
        this(host, port, auth, sslEnable, readonly, username, new SecureChars(password));
    }
    
    @Override
    public void connect() throws Exception
    {
       synchronized (lock)
        {
            Properties props = new Properties();
            props.put(MAIL_POP3_HOST, host);
            props.put(MAIL_POP3_PORT, Integer.toString(port));
            props.put(MAIL_POP3_AUTH, auth);
            props.put(MAIL_POP3_SSL_ENABLE, sslEnable);

            Session session = Session.getInstance(props);
            store = session.getStore(POP3);
            char[] pass = password.getChars();
            try
            {
                store.connect(host, username, new String(pass));
            }
            finally
            {
                Arrays.fill(pass,'\0');
            }
            inbox = store.getFolder("INBOX");
            inbox.open(readonly ? Folder.READ_ONLY:Folder.READ_WRITE);
        }
    }

    @Override
    public boolean isConnected()
    {
        synchronized (lock)
        {
            return store!=null && store.isConnected();
        }
    }
    
    @Override
    public Message[] getMessages() throws MessagingException
    {
        synchronized (lock)
        {
           return inbox.getMessages();
        }
    }
    
    @Override
    public Message[] getMessages(Date after) throws MessagingException
    {
        synchronized (lock)
        {
            ArrayList<Message> list = new ArrayList<>();
            Message[] m = inbox.getMessages();
            if(after==null)
            {
                return m;
            }
            for(Message item : m)
            {
                if(item.getReceivedDate().compareTo(after)>0)
                {
                    list.add(item);
                }
            }
            return list.toArray(new Message[0]);
        }
    }
 
    @Override
    public void close() 
    {
        synchronized (lock)
        {
            try
            {
                inbox.close(false);
                store.close();
            }
            catch (MessagingException ex)
            {
                Logger.getLogger(POP3.class.getName()).log(Level.SEVERE, (String) null, ex);
            }
        }
    }    
    
}
