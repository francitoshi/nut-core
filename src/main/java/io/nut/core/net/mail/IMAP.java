/*
 *  IMAP.java
 *
 *  Copyright (C) 2025-2026 francitoshi@gmail.com
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
import jakarta.mail.UIDFolder;
import jakarta.mail.event.MessageCountAdapter;
import jakarta.mail.event.MessageCountEvent;
import jakarta.mail.event.MessageCountListener;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.ReceivedDateTerm;
import jakarta.mail.search.SearchTerm;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.angus.mail.imap.IMAPFolder;

/**
 *
 * @author franci
 */
public class IMAP implements MailReader
{
    public interface ImapListener
    {
        void send(Message message, long uidValidity, long lastUID);
    }
    
    private static final String IMAP = "imap";
    private static final String MAIL_STORE_PROTOCOL = "mail.store.protocol";
    private static final String MAIL_IMAP_SSL_ENABLE = "mail.imap.ssl.enable";
    private static final String MAIL_IMAP_PORT = "mail.imap.port";
    private static final String MAIL_IMAP_HOST = "mail.imap.host";

    public static final int SAFE_PORT_993 = 993;
    
    static final int TYPE_TEXT = 1;
    static final int TYPE_IMAGE = 2;
    static final int TYPE_AUDIO = 3;
    static final int TYPE_VIDEO = 4;
    static final int TYPE_APPLICATION = 5;

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
    private volatile IMAPFolder imapInbox;
    private volatile ImapListener imapListener;

    private volatile long uidValidity;
    private volatile long lastUID;

    public IMAP(String host, int port, boolean auth, boolean sslEnable, boolean readonly, String username, SecureChars password)
    {
        this.host = host;
        this.port = port;
        this.auth = auth;
        this.sslEnable = sslEnable;
        this.readonly = readonly;
        this.username = username;
        this.password = password;
    }
    public IMAP(String host, int port, boolean auth, boolean sslEnable, boolean readonly, String username, char[] password)
    {
        this(host, port, auth, sslEnable, readonly, username, new SecureChars(password));
    }
    
    public void setImapListener(ImapListener listener, long uidValidity, long lastUID)
    {
        this.imapListener = listener;
        this.uidValidity = uidValidity;
        this.lastUID = lastUID;
    }

    private final MessageCountListener listener = new MessageCountAdapter()
    {
        @Override
        public void messagesAdded(MessageCountEvent event)
        {
            for (Message msg : event.getMessages())
            {
                if(imapInbox!=null)
                {
                    try
                    {
                        long uid = imapInbox.getUID(msg);
                        lastUID = Math.max(lastUID, uid);
                    }
                    catch (MessagingException ex)
                    {
                        Logger.getLogger(IMAP.class.getName()).log(Level.SEVERE, (String) null, ex);
                    }
                }
                imapListener.send(msg, uidValidity, lastUID);
            }
        }
    };
    
    @Override
    public void connect() throws Exception
    {
       synchronized (lock)
        {
            Properties props = new Properties();
            props.put(MAIL_STORE_PROTOCOL, IMAP);
            props.put(MAIL_IMAP_HOST, host);
            props.put(MAIL_IMAP_PORT, Integer.toString(port));
            props.put(MAIL_IMAP_SSL_ENABLE, sslEnable?"true":"false"); // enables SSL
            
            Session session = Session.getInstance(props);
            store = session.getStore(IMAP);

            store.connect(host, username, password.apply((pass)-> new String(pass)));
            inbox = store.getFolder("INBOX");
            inbox.open(readonly ? Folder.READ_ONLY:Folder.READ_WRITE);
            imapInbox = (inbox instanceof IMAPFolder) ? (IMAPFolder)inbox : null;

            if(imapListener!=null)
            {
                inbox.addMessageCountListener(listener);
                if(imapInbox!=null)
                {
                    long uid = imapInbox.getUIDValidity();
                    if(uid!=uidValidity)
                    {
                        lastUID = 0;
                        uidValidity = uid;
                    }
                }
            }
        }
    }
    
    public void idle() throws MessagingException
    {
        if(imapInbox!=null)
        {
            Message[] list = imapInbox.getMessagesByUID(lastUID + 1, UIDFolder.LASTUID);
            for (Message msg : list) 
            {
                long uid = imapInbox.getUID(msg);
                lastUID = Math.max(lastUID, uid);
                imapListener.send(msg, uidValidity, lastUID);
            }
            while(isConnected()) 
            {
                imapInbox.idle();
            }
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
            if(after==null)
            {
                return inbox.getMessages();
            }
            SearchTerm dateTerm = new ReceivedDateTerm(ComparisonTerm.GT, after);
            return inbox.search(dateTerm);
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
                Logger.getLogger(IMAP.class.getName()).log(Level.SEVERE, (String) null, ex);
            }
        }
    }
}
