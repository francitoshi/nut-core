/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.core.gauge;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class TerminalGaugeTest
{
    
    public TerminalGaugeTest()
    {
    }

    @Test
    public void testSetFillColors() throws IOException
    {
        TerminalGauge instance = new TerminalGauge(true);
        instance.setFillChar('#');
        instance.setEmptyChar(' ');
        instance.setBarStyle(true, 51,51);
        instance.setWidthLimit(80);
        instance.start(10);
        for(int i=0;i<10;i++)
        {
            instance.setVal(i);
        }
        instance.close();
    }

    
}
