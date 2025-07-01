/*
 * ProxySettings.java
 *
 * Copyright (c) 2016-2023 francitoshi@gmail.com
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
 */
package io.nut.core.net;

import java.io.IOException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.Objects;

import okhttp3.Credentials;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

/**
 * Created by franci on 27/11/16.
 */

public class ProxySettings
{
    public static final String PROXY_AUTHORIZATION = "Proxy-Authorization";

    private static class SimpleAuthenticator extends Authenticator
    {
        final PasswordAuthentication passwordAuthentication;
        public SimpleAuthenticator(PasswordAuthentication passwordAuthentication)
        {
            this.passwordAuthentication = passwordAuthentication;
        }
        @Override
        protected PasswordAuthentication getPasswordAuthentication()
        {
            return this.passwordAuthentication;
        }
    }
    private static class SimpleOkhttpAuthenticator implements okhttp3.Authenticator
    {
        final String credential;

        public SimpleOkhttpAuthenticator(String user, String pass)
        {
            this.credential = Credentials.basic(user, pass);
        }
        @Override
        public Request authenticate(Route route, Response response) throws IOException
        {
            return response.request().newBuilder().header(PROXY_AUTHORIZATION, credential).build();
        }
    }

    public static final ProxySettings NO_PROXY = new ProxySettings(null, 0, null, null, Proxy.Type.DIRECT);
    public static final Authenticator NO_AUTHENTICATOR = new SimpleAuthenticator(null);
    public static final okhttp3.Authenticator NO_OKHTTP_AUTHENTICATOR = okhttp3.Authenticator.NONE;

    public final String host;
    public final int port;
    public final String user;
    public final String pass;
    public final Proxy.Type type;
    private final boolean noproxy;
    private final boolean noauth;

    public ProxySettings(String host, int port, String user, String pass, Proxy.Type type)
    {
        this.host = host;
        this.port = port;
        this.user = user;
        this.pass = pass;
        this.type = type;
        this.noproxy = (type==Proxy.Type.DIRECT || this.host==null || this.host.isEmpty() || port<0);
        this.noauth = this.noproxy || (user==null && pass==null);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProxySettings that = (ProxySettings) o;
        return port == that.port &&
                Objects.equals(host, that.host) &&
                Objects.equals(user, that.user) &&
                Objects.equals(pass, that.pass) &&
                type == that.type;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(host, port, user, pass, type);
    }

    public boolean isNoProxy()
    {
        return this.noproxy;
    }
    public boolean isProxy()
    {
        return this.noproxy==false;
    }

    public boolean isNoAuth()
    {
        return this.noauth;
    }
    public boolean isAuth()
    {
        return this.noauth==false;
    }

    private volatile Proxy proxy;
    private volatile Authenticator authenticator;
    private volatile okhttp3.Authenticator okhttpAuthenticator;

    public Proxy getProxy()
    {
        if(this.proxy==null)
        {
            this.proxy = this.noproxy ? Proxy.NO_PROXY : new Proxy(type, new InetSocketAddress(host, port));
        }
        return this.proxy;
    }
    public Authenticator getAuthenticator()
    {
        if(this.authenticator==null)
        {
            this.authenticator = this.noproxy ? NO_AUTHENTICATOR : new SimpleAuthenticator(new PasswordAuthentication(user, pass.toCharArray()));
        }
        return authenticator;
    }
    public okhttp3.Authenticator getOkHttpAuthenticator()
    {
        if(this.okhttpAuthenticator==null)
        {
            this.okhttpAuthenticator = this.noproxy ? NO_OKHTTP_AUTHENTICATOR : new SimpleOkhttpAuthenticator(user, pass);
        }
        return this.okhttpAuthenticator;
    }

    public interface Factory
    {
        ProxySettings getProxySettings();
    }

    private final Factory factory = new Factory()
    {
        @Override
        public ProxySettings getProxySettings()
        {
            return ProxySettings.this;
        }
    };
    public Factory getFactory()
    {
        return factory;
    }

    public static boolean equivalents(ProxySettings a, ProxySettings b)
    {
        if(a==b)
        {
            return true;
        }
        boolean aNoProxy = a==null || a.isNoProxy();
        boolean bNoProxy = b==null || b.isNoProxy();
        if(aNoProxy && bNoProxy)
        {
            return true;
        }
        if(aNoProxy!=aNoProxy)
        {
            return false;
        }
        return a.equals(b);
    }
}
