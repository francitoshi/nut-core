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

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

/**
 *
 * @author franci
 */
public class SMTP
{
    private static final String MAIL_SMTP_AUTH              = "mail.smtp.auth";
    private static final String MAIL_SMTP_STARTTLS_ENABLE   = "mail.smtp.starttls.enable";
    private static final String MAIL_SMTP_HOST              = "mail.smtp.host";
    private static final String MAIL_SMTP_PORT              = "mail.smtp.port";

    public static final int SAFE_PORT_587 = 587;
    
    private volatile boolean auth;
    private volatile boolean starttlsEnable;
    private volatile String host;
    private volatile int port = 587;
    
    private volatile String username;
    private volatile String password;
    private volatile String from;
    private volatile String to;
    private volatile String cc;
    private volatile String bcc;
    private volatile String replyTo;

    public void setAuth(boolean value)
    {
        this.auth = value;
    }

    public void setStarttlsEnable(boolean value)
    {
        this.starttlsEnable = value;
    }

    public void setHost(String value)
    {
        this.host = value;
    }

    public void setPort(int value)
    {
        this.port = value;
    }

    public void setUsername(String value)
    {
        this.username = value;
    }

    public void setPassword(String value)
    {
        this.password = value;
    }

    public void setFrom(String value)
    {
        this.from = value;
    }

    public void setTo(String value)
    {
        this.to = value;
    }

    public void setCc(String value)
    {
        this.cc = value;
    }

    public void setBcc(String value)
    {
        this.bcc = value;
    }
    
    public void send(String subject, String text) throws AddressException, MessagingException
    {
        // Configuración del remitente

        Properties props = new Properties();
        props.put(MAIL_SMTP_AUTH, auth?"true":"false");
        props.put(MAIL_SMTP_STARTTLS_ENABLE, starttlsEnable?"true":"false");
        props.put(MAIL_SMTP_HOST, host);
        props.put(MAIL_SMTP_PORT, Integer.toString(port));

        Session session = Session.getInstance(props, new Authenticator()
        {
            @Override
            protected PasswordAuthentication getPasswordAuthentication()
            {
                return new PasswordAuthentication(username, password);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setText(text);
        if(replyTo!=null && !replyTo.isEmpty())
        {
            message.setReplyTo(InternetAddress.parse(replyTo));
        }

        Transport.send(message);
    }

    
}
