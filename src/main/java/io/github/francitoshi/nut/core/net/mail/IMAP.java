/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.core.net.mail;

import io.github.francitoshi.nut.base.commons.security.SecureChars;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.NoSuchProviderException;
import jakarta.mail.Provider;
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
 * {@link MailReader} implementation over IMAP, using implicit SSL or STARTTLS.
 * {@link #connect()} refuses to authenticate over an unencrypted channel.
 * <p>When an {@link ImapListener} is set, {@link #idle()} keeps the connection
 * open and reports new messages together with the folder's UIDVALIDITY and the
 * highest UID seen so far, so the caller can resume from where it left off.</p>
 *
 * @author franci
 */
public class IMAP implements MailReader
{
    /**
     * Callback notified, synchronously from {@link IMAP#idle()} or during
     * {@link IMAP#connect()}, about messages the caller has not processed yet.
     */
    public interface ImapListener
    {
        /**
         * @param message     the new message.
         * @param uidValidity current UIDVALIDITY of the mailbox.
         * @param lastUID     highest UID processed so far.
         */
        void send(Message message, long uidValidity, long lastUID);
    }
    
    private static final String IMAP = "imap";
    private static final String ANGUS_IMAP_STORE = "org.eclipse.angus.mail.imap.IMAPStore";
    private static final String MAIL_STORE_PROTOCOL = "mail.store.protocol";
    private static final String MAIL_IMAP_SSL_ENABLE = "mail.imap.ssl.enable";
    private static final String MAIL_IMAP_SSL_CHECKSERVERIDENTITY = "mail.imap.ssl.checkserveridentity";
    private static final String MAIL_IMAP_SSL_PROTOCOLS = "mail.imap.ssl.protocols";
    private static final String MAIL_IMAP_STARTTLS_ENABLE = "mail.imap.starttls.enable";
    private static final String MAIL_IMAP_STARTTLS_REQUIRED = "mail.imap.starttls.required";
    private static final String MAIL_IMAP_PORT = "mail.imap.port";
    private static final String MAIL_IMAP_HOST = "mail.imap.host";

    private static final String SSL_PROTOCOLS = "TLSv1.3 TLSv1.2";

    /** Implicit-SSL port commonly used by IMAP servers. */
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
    private final boolean starttlsEnable;
    private final boolean readonly;
    private final String username;
    private final SecureChars password;
    
    private volatile Store store;
    private volatile Folder inbox;
    private volatile IMAPFolder imapInbox;
    private volatile ImapListener imapListener;

    private volatile long uidValidity;
    private volatile long lastUID;

    /**
     * Client intended for explicit STARTTLS (or implicit SSL when both flags
     * are false and {@code sslEnable} is true) against {@code host:port}.
     *
     * @param host            server host name.
     * @param port            server port, e.g. {@link #SAFE_PORT_993}.
     * @param auth            whether to authenticate with {@code username}.
     * @param sslEnable       enable implicit SSL.
     * @param starttlsEnable  enable STARTTLS and make it mandatory.
     * @param readonly        open the mailbox read-only.
     * @param username        user name, may be {@code null} when {@code auth} is false.
     * @param password        password holder, may be {@code null} when {@code auth} is false.
     */
    public IMAP(String host, int port, boolean auth, boolean sslEnable, boolean starttlsEnable, boolean readonly, String username, SecureChars password)
    {
        this.host = host;
        this.port = port;
        this.auth = auth;
        this.sslEnable = sslEnable;
        this.starttlsEnable = starttlsEnable;
        this.readonly = readonly;
        this.username = username;
        this.password = password;
    }

    /** Convenience variant that keeps the password in a {@link SecureChars}. */
    public IMAP(String host, int port, boolean auth, boolean sslEnable, boolean readonly, String username, SecureChars password)
    {
        this(host, port, auth, sslEnable, false, readonly, username, password);
    }
    /** Convenience variant that builds a {@link SecureChars} from {@code password}. */
    public IMAP(String host, int port, boolean auth, boolean sslEnable, boolean readonly, String username, char[] password)
    {
        this(host, port, auth, sslEnable, false, readonly, username, new SecureChars(password));
    }
    
    
    /**
     * Registers the listener and the resume point. If the mailbox reports a
     * different UIDVALIDITY at {@link #connect()}, the UID cursor is reset to
     * 0 and {@code listener} will see every message.
     *
     * @param listener    receives new messages; may be {@code null}.
     * @param uidValidity UIDVALIDITY as last seen by the caller.
     * @param lastUID     highest UID already processed by the caller.
     */
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
    
    /**
     * When several jakarta.mail providers are on the classpath, {@link Session}
     * uses the first provider registered for {@code imap}, which may lack UID
     * and IDLE support. Selects the angus implementation explicitly when it is
     * present.
     */
    private static void forceAngusImapProvider(Session session) throws NoSuchProviderException
    {
        for(Provider provider : session.getProviders())
        {
            if(Provider.Type.STORE == provider.getType()
                    && IMAP.equals(provider.getProtocol())
                    && ANGUS_IMAP_STORE.equals(provider.getClassName()))
            {
                session.setProvider(provider);
                return;
            }
        }
    }

    /**
     * Connects to the server and opens INBOX, refusing to authenticate over an
     * unencrypted channel. If a listener is registered, client-side
     * notifications are wired up and the UID cursor is reset when the
     * mailbox's UIDVALIDITY changed.
     */
    @Override
    public void connect() throws Exception
    {
       synchronized (lock)
        {
            if (auth && !sslEnable && !starttlsEnable)
            {
                throw new MessagingException("Refusing to authenticate over an unencrypted IMAP channel: enable STARTTLS or implicit SSL");
            }
            Properties props = new Properties();
            props.put(MAIL_STORE_PROTOCOL, IMAP);
            props.put(MAIL_IMAP_HOST, host);
            props.put(MAIL_IMAP_PORT, Integer.toString(port));
            props.put(MAIL_IMAP_SSL_ENABLE, sslEnable?"true":"false"); // enables implicit SSL
            props.put(MAIL_IMAP_SSL_CHECKSERVERIDENTITY, "true");
            props.put(MAIL_IMAP_SSL_PROTOCOLS, SSL_PROTOCOLS);
            props.put(MAIL_IMAP_STARTTLS_ENABLE, starttlsEnable?"true":"false");
            props.put(MAIL_IMAP_STARTTLS_REQUIRED, starttlsEnable?"true":"false");
            
            Session session = Session.getInstance(props);
            forceAngusImapProvider(session); // the angus provider exposes UID and IDLE
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
    
    /**
     * Re-dispatches every message newer than the UID cursor through the
     * listener, then blocks on IMAP IDLE (inside a loop) dispatching newly
     * received messages, until the connection is closed. Requires a listener
     * and a server that supports IDLE.
     */
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
                Logger.getLogger(IMAP.class.getName()).log(Level.SEVERE, (String) null, ex);
            }
        }
    }
}
