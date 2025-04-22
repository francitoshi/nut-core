/*
 *  TerminalGaugeTest.java
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
