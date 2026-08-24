/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.core.net.mail;

import io.nut.base.security.SecureChars;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import java.util.ArrayList;
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
            
            store.connect(host, username, password.apply((pass)-> new String(pass)));
            
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
                if(inbox!=null)
                {
                    inbox.close(false);
                }
                if(store!=null)
                {
                    store.close();
                }
            }
            catch (MessagingException ex)
            {
                Logger.getLogger(POP3.class.getName()).log(Level.SEVERE, (String) null, ex);
            }
        }
    }    
    
}
