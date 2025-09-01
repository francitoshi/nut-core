/*
 *  SMTP.java
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
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.NoSuchProviderException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.Closeable;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author franci
 */
public class SMTP implements Closeable
{
    private static final String MAIL_SMTP_AUTH              = "mail.smtp.auth";
    private static final String MAIL_SMTP_STARTTLS_ENABLE   = "mail.smtp.starttls.enable";
    private static final String MAIL_SMTP_HOST              = "mail.smtp.host";
    private static final String MAIL_SMTP_PORT              = "mail.smtp.port";

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
    public SMTP(String host, int port, boolean auth, boolean starttlsEnable, String username, char[] password, String from, String replyTo)
    {
        this(host, port, auth, starttlsEnable, username, new SecureChars(password), from, replyTo);
    }    
    public SMTP(String host, int port, boolean auth, boolean starttlsEnable, String username, SecureChars password, String from)
    {
        this(host, port, auth, starttlsEnable, username, password, from, null);
    }
    public SMTP(String host, int port, boolean auth, boolean starttlsEnable, String username, char[] password, String from)
    {
        this(host, port, auth, starttlsEnable, username, new SecureChars(password), from, null);
    }
    
    private volatile Session session;
    private volatile Transport transport;

    public SMTP connect() throws NoSuchProviderException, MessagingException
    {
        synchronized (lock)
        {
            Properties props = new Properties();
            props.put(MAIL_SMTP_AUTH, auth?"true":"false");
            props.put(MAIL_SMTP_STARTTLS_ENABLE, starttlsEnable?"true":"false");
            props.put(MAIL_SMTP_HOST, host);
            props.put(MAIL_SMTP_PORT, Integer.toString(port));

            session = Session.getInstance(props);
            transport = session.getTransport("smtp");
            char[] pass = password.getChars();
            try
            {
                transport.connect(username, new String(pass));
            }
            finally
            {
                Arrays.fill(pass,'\0');
            }
            return this;
        }
    }

    public boolean isConnected()
    {
        synchronized (lock)
        {
            return transport!=null && transport.isConnected();
        }
    }
    
    @Override
    public void close()
    {
        synchronized (lock)
        {
            try
            {
                transport.close();
            }
            catch (MessagingException ex)
            {
                Logger.getLogger(SMTP.class.getName()).log(Level.SEVERE, (String) null, ex);
            }
        }
    }
    
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
            transport.sendMessage(message, message.getAllRecipients());
        }
    }
    public void send(String subject, String text, String to, String cc) throws AddressException, MessagingException
    {
        send(subject, text, to, cc, null);
    }
    public void send(String subject, String text, String to) throws AddressException, MessagingException
    {
        send(subject, text, to, null, null);
    }
    
}
