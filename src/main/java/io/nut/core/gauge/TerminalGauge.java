/*
 *  TerminalGauge.java
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

import io.nut.base.gauge.AbstractGauge;
import io.nut.base.io.ansi.Ansi;
import io.nut.base.util.Strings;
import java.io.IOException;
import java.io.PrintStream;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 *
 * @author franci
 */
public class TerminalGauge extends AbstractGauge
{
    public final char BLOCK = '\u2588';
    public final char LIGHT_SHADE = '\u2591';
    
    private boolean debug = false;
   
    private final boolean hasConsole;
    private final Terminal terminal;
    private final PrintStream out;
    private volatile int widthLimit=Integer.MAX_VALUE;
    private volatile char fillChar = BLOCK;
    private volatile char emptyChar = LIGHT_SHADE;
        
    public TerminalGauge(Terminal terminal)
    {
        super();
        this.out = System.out;
        this.hasConsole = System.console()!=null;
        this.terminal = terminal;
    }
    public TerminalGauge() throws IOException
    {
        super();
        this.out = System.out;
        this.hasConsole = System.console()!=null;
        this.terminal = hasConsole ? TerminalBuilder.terminal() : TerminalBuilder.builder().streams(System.in, System.out).build();
    }

    public void setWidthLimit(int value)
    {
        this.widthLimit = value;
    }

    public void setFillChar(char fillChar)
    {
        this.fillChar = fillChar;
    }

    public void setEmptyChar(char emptyChar)
    {
        this.emptyChar = emptyChar;
    }

    public void println(String s)
    {
        this.out.print(new Ansi().append('\r').eraseLine(Ansi.Erase.ALL).append(s).append('\n'));
        this.out.flush();
        invalidate();
    }
    
    private static final String[] TIME_FMT = 
    {
        "",                 //000   0
        "%3$s",             //001   1 
        "%2$s",             //020   2   
        "%2$s = %3$s",      //021   3
        "%1$s",             //400   4
        "%1$s / %3$s",      //401   5
        "%1$s + %2$s",      //420   6
        "%1$s + %2$s = %3$s"//421   7
    };
    
    private volatile boolean prevEnabled;
    private volatile boolean nextEnabled;
    private volatile boolean fullEnabled;
    
    public void paint(boolean started, int max, int val, double done, String prefix, String prev, String next, String full)
    {
        prevEnabled &= prev!=null;
        nextEnabled &= next!=null;
        fullEnabled &= full!=null;
        
        int index = (prev!=null?4:0) + (prev!=null?2:0) + (prev!=null?1:0);
        int width = (prev!=null?9:0) + (prev!=null?9:0) + (prev!=null?9:0);
        
        StringBuilder head = new StringBuilder("\r");
        StringBuilder tail = new StringBuilder();
        if(prefix!=null && !(prefix=prefix.trim()).isEmpty())
        {
            head.append(prefix);
            width += prefix.length()+3;
        }
        if(max>0)
        {
            tail.append(String.format("%d/%d ", val, max));
            width += Math.log10(max)*2+3;
        }
        tail.append(String.format("%.2f%%", done*100));
        width += 7;
        
        int maxWidth = Math.min(widthLimit, terminal.getWidth());
        int bar = maxWidth-width;
        
        if(bar<0)
        {
            if(full!=null)
            {
                full = null;
            }
            else if(prev!=null)
            {
                prev = null;
            }
            else if(prefix!=null)
            {
                prefix = null;
            }
            else if(max!=0)
            {
                max = 0;
            }
            else
            {
                bar = 0;
            }
            if(bar<0)
            {
                paint(started, max, val, done, prefix, prev, next, full);
                return;
            }
        }
        if(bar>8)
        {
            int b = bar-4;
            int d = (int)(b*done);
            int n = b-d;
            String dd = Strings.repeat(fillChar, d);
            String nn = Strings.repeat(emptyChar, n);
            head.append(" |").append(dd).append(nn).append("| ");
        }
        if(index!=0)
        {                   
            boolean show = (prev!=null && !prev.isEmpty()) || (next!=null && !next.isEmpty()) || (full!=null && !full.isEmpty());
            if(show)
            {
                tail.append(" | ").append(String.format(TIME_FMT[index], prev, next, full));
            }
        }
        String s = Ansi.ansi().append('\r').append(head).append(tail).append(hasConsole?'\r':'\n').toString();
        this.out.print(s);
        this.out.flush();
    }

    public boolean isDebug()
    {
        return debug;
    }

    public void setDebug(boolean debug)
    {
        this.debug = debug;
    }

    //hacer que consoleGaugeView herede de esta

}
