/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.core.net.mail;

import io.github.francitoshi.nut.base.commons.security.SecureChars;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.NoSuchProviderException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.Closeable;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Minimal SMTP client for sending plain text mail over STARTTLS (or without
 * encryption). {@link #connect()} refuses to authenticate over an unencrypted
 * channel.
 * <p>An instance may be closed and connected again at will; use
 * {@link #reconnect(long)} to drop connections that have been idle for longer
 * than a given timeout.</p>
 *
 * @author franci
 */
public class SMTP implements Closeable
{
    private static final String MAIL_SMTP_AUTH              = "mail.smtp.auth";
    private static final String MAIL_SMTP_STARTTLS_ENABLE   = "mail.smtp.starttls.enable";
    private static final String MAIL_SMTP_STARTTLS_REQUIRED = "mail.smtp.starttls.required";
    private static final String MAIL_SMTP_SSL_CHECKSERVERIDENTITY = "mail.smtp.ssl.checkserveridentity";
    private static final String MAIL_SMTP_SSL_PROTOCOLS          = "mail.smtp.ssl.protocols";
    private static final String MAIL_SMTP_HOST              = "mail.smtp.host";
    private static final String MAIL_SMTP_PORT              = "mail.smtp.port";

    private static final String SSL_PROTOCOLS = "TLSv1.3 TLSv1.2";

    /** Port commonly used by STARTTLS-capable SMTP relays. */
    public static final int SAFE_PORT_587 = 587;
    
    private final Object lock = new Object();
    
    private final String host;
    private final int port;
    private final boolean auth;
    private final boolean starttlsEnable;
    private final String username;
    private final SecureChars password;
    private final String from;
    private final String replyTo;

    /**
     * Client for {@code host:port} that sends mail as {@code from}.
     *
     * @param host           server host name.
     * @param port           server port, e.g. {@link #SAFE_PORT_587}.
     * @param auth           whether to authenticate with {@code username}.
     * @param starttlsEnable enable STARTTLS and make it mandatory.
     * @param username       user name, may be {@code null} when {@code auth} is false.
     * @param password       password holder, may be {@code null} when {@code auth} is false.
     * @param from           address used as the sender.
     * @param replyTo        Reply-To address, or {@code null} to omit it.
     */
    public SMTP(String host, int port, boolean auth, boolean starttlsEnable, String username, SecureChars password, String from, String replyTo)
    {
        this.host = host;
        this.port = port;
        this.auth = auth;
        this.starttlsEnable = starttlsEnable;
        this.username = username;
        this.password = password;
        this.from = from;
        this.replyTo = replyTo;
    }
    /** Convenience variant that builds a {@link SecureChars} from {@code password}. */
    public SMTP(String host, int port, boolean auth, boolean starttlsEnable, String username, char[] password, String from, String replyTo)
    {
        this(host, port, auth, starttlsEnable, username, new SecureChars(password), from, replyTo);
    }
    /** Convenience variant with no Reply-To. */
    public SMTP(String host, int port, boolean auth, boolean starttlsEnable, String username, SecureChars password, String from)
    {
        this(host, port, auth, starttlsEnable, username, password, from, null);
    }
    /** Convenience variant that builds a {@link SecureChars} and sets no Reply-To. */
    public SMTP(String host, int port, boolean auth, boolean starttlsEnable, String username, char[] password, String from)
    {
        this(host, port, auth, starttlsEnable, username, new SecureChars(password), from, null);
    }
    
    private volatile Session session;
    private volatile Transport transport;
    private volatile long lastUsed = 0;

    /**
     * Creates the transport and connects to the server. May be called again
     * after {@link #close()} to reuse this instance.
     *
     * @return this instance, for chaining.
     */
    public SMTP connect() throws NoSuchProviderException, MessagingException
    {
        synchronized (lock)
        {
            if (auth && !starttlsEnable)
            {
                throw new MessagingException("Refusing to authenticate over an unencrypted SMTP channel: enable STARTTLS or use implicit SSL");
            }
            Properties props = new Properties();
            props.put(MAIL_SMTP_AUTH, auth?"true":"false");
            props.put(MAIL_SMTP_STARTTLS_ENABLE, starttlsEnable?"true":"false");
            props.put(MAIL_SMTP_STARTTLS_REQUIRED, starttlsEnable?"true":"false");
            props.put(MAIL_SMTP_SSL_CHECKSERVERIDENTITY, "true");
            props.put(MAIL_SMTP_SSL_PROTOCOLS, SSL_PROTOCOLS);
            props.put(MAIL_SMTP_HOST, host);
            props.put(MAIL_SMTP_PORT, Integer.toString(port));

            session = Session.getInstance(props);
            transport = session.getTransport("smtp");
            
            transport.connect(username, password.apply((pass)-> new String(pass)));
            lastUsed = System.nanoTime();
            return this;
        }
    }

    /**
     * Keeps {@code this} usable, closing and reconnecting only when the
     * connection has been idle for longer than the given timeout.
     *
     * @param idleTimeoutMillis reconnect when idle longer than this.
     * @return this instance, for chaining.
     */
    public SMTP reconnect(long idleTimeoutMillis) throws MessagingException
    {
        synchronized (lock)
        {
            long idleTimeoutNanos = TimeUnit.MILLISECONDS.toNanos(idleTimeoutMillis);
            long now = System.nanoTime();
            if (transport != null && transport.isConnected() && (now - lastUsed) > idleTimeoutNanos)
            {
                close(); // reconectamos fresca
            }
            if (transport == null || !transport.isConnected())
            {
                connect();
            }
            return this;
        }
    }    

    /**
     * @return {@code true} while the transport is connected.
     */
    public boolean isConnected()
    {
        synchronized (lock)
        {
            return transport!=null && transport.isConnected();
        }
    }
    
    /**
     * Closes the transport. Any {@link MessagingException} is only logged.
     */
    @Override
    public void close()
    {
        synchronized (lock)
        {
            try
            {
                if(transport!=null)
                {
                    transport.close();
                }
            }
            catch (MessagingException ex)
            {
                Logger.getLogger(SMTP.class.getName()).log(Level.SEVERE, (String) null, ex);
            }
        }
    }
    
    /**
     * Builds and sends a plain text mail. The transport must be connected.
     * {@link MimeMessage#saveChanges()} is applied, so a {@code Date} header
     * is generated when the caller did not set one.
     *
     * @param subject subject line.
     * @param text    plain text body.
     * @param to      comma separated TO recipients; must not be empty.
     * @param cc      comma separated CC recipients, or {@code null}/empty to omit.
     * @param bcc     comma separated BCC recipients, or {@code null}/empty to omit.
     */
    public void send(String subject, String text, String to, String cc, String bcc) throws AddressException, MessagingException
    {
        synchronized (lock)
        {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            if(replyTo!=null && !replyTo.isEmpty())
            {
                message.setReplyTo(InternetAddress.parse(replyTo));
            }
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            if (cc != null && !cc.isEmpty())
            {
                message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(cc));
            }
            if (bcc != null && !bcc.isEmpty())
            {
                message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(bcc));
            }
            message.setSubject(subject);
            message.setText(text);
            message.saveChanges(); // generates the Date header by Transport.send() behavior
            transport.sendMessage(message, message.getAllRecipients());
            lastUsed = System.nanoTime();
        }
    }
    /** {@link #send(String, String, String, String, String)} without BCC. */
    public void send(String subject, String text, String to, String cc) throws AddressException, MessagingException
    {
        send(subject, text, to, cc, null);
    }
    /** {@link #send(String, String, String)} convenience without CC nor BCC. */
    public void send(String subject, String text, String to) throws AddressException, MessagingException
    {
        send(subject, text, to, null, null);
    }
    
}
