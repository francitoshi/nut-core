/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.core.net;

import java.net.Proxy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProxySettingsTest
{
    private static final String HOST = "proxy.example.com";
    private static final int PORT = 8080;
    private static final String USER = "user";
    private static final String PASS = "pass";

    @Test
    public void testEquivalentsNullVersusProxy()
    {
        ProxySettings proxy = new ProxySettings(HOST, PORT, USER, PASS, Proxy.Type.HTTP);
        assertFalse(ProxySettings.equivalents(null, proxy));
    }

    @Test
    public void testEquivalentsProxyVersusNull()
    {
        ProxySettings proxy = new ProxySettings(HOST, PORT, USER, PASS, Proxy.Type.HTTP);
        assertFalse(ProxySettings.equivalents(proxy, null));
    }

    @Test
    public void testEquivalentsNoProxyVersusProxy()
    {
        ProxySettings noproxy = new ProxySettings(null, 0, null, null, Proxy.Type.DIRECT);
        ProxySettings proxy = new ProxySettings(HOST, PORT, USER, PASS, Proxy.Type.HTTP);
        assertFalse(ProxySettings.equivalents(noproxy, proxy));
        assertFalse(ProxySettings.equivalents(proxy, noproxy));
    }

    @Test
    public void testEquivalentsBothNoProxy()
    {
        ProxySettings a = new ProxySettings(null, 0, null, null, Proxy.Type.DIRECT);
        ProxySettings b = new ProxySettings("", -1, USER, PASS, Proxy.Type.SOCKS);
        assertTrue(ProxySettings.equivalents(a, b));
    }

    @Test
    public void testEquivalentsSameInstanceAndNulls()
    {
        ProxySettings proxy = new ProxySettings(HOST, PORT, USER, PASS, Proxy.Type.HTTP);
        assertTrue(ProxySettings.equivalents(proxy, proxy));
        assertTrue(ProxySettings.equivalents(null, null));
    }

    @Test
    public void testEquivalentsEqualProxies()
    {
        ProxySettings a = new ProxySettings(HOST, PORT, USER, PASS, Proxy.Type.HTTP);
        ProxySettings b = new ProxySettings(HOST, PORT, USER, PASS, Proxy.Type.HTTP);
        assertTrue(ProxySettings.equivalents(a, b));
    }
}
