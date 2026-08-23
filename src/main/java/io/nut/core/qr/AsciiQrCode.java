package io.nut.core.qr;

import io.nayuki.qrcodegen.QrCode;

import java.util.Objects;

/**
 * Generates an ASCII/Unicode representation of a {@link QrCode} from the
 * io.nayuki.fastqrcodegen library, in four distinct variants depending on how
 * many modules (bits) are packed into each output character.
 *
 * <p>
 * The available modes are:</p>
 * <ol>
 * <li>{@link #render2CharsPerBit()}: each bit occupies TWO characters (a full
 * block U+2588 twice, or two spaces). The "square" variant visually, since it
 * compensates for monospaced font characters typically being taller than wide.</li>
 * <li>{@link #render1CharPerBit()}: each bit occupies ONE character (a full
 * block or space). The simplest, but it looks squashed (taller than wide).</li>
 * <li>{@link #render2VerticalBitsPerChar()}: each character represents 2 bits
 * stacked vertically using half-blocks (▀ U+2580, ▄ U+2584, █ U+2588, or
 * space). Reduces lines by half and gives a very square terminal appearance.</li>
 * <li>{@link #render4BitsPerChar()}: each character represents a 2x2 block of
 * bits using Unicode quadrant characters (▘ ▝ ▖ ▗ ▚ ▞ ▛ ▜ ▙ ▟ ▀ ▄ ▌ ▐ █ and
 * space), covering all 16 possible combinations. The most compact representation
 * (1/4 the characters of mode 2) but with the least resolution retained.</li>
 * </ol>
 */
public final class AsciiQrCode
{

    /**
     * Full block character.
     */
    private static final char FULL_BLOCK = '\u2588';

    // Half-block characters (mode 3: 2 vertical bits per character).
    private static final char UPPER_HALF = '\u2580'; // ▀
    private static final char LOWER_HALF = '\u2584'; // ▄

    // Quadrant characters (mode 4: 4 bits, 2x2 per character).
    // Table index = (TL << 3) | (TR << 2) | (BL << 1) | BR
    // where TL/TR/BL/BR are the top-left, top-right, bottom-left,
    // bottom-right bits (1 = dark module).
    private static final char[] QUADRANTS = new char[16];

    static
    {
        QUADRANTS[0b0000] = ' ';
        QUADRANTS[0b0001] = '\u2597'; // ▗ bottom right
        QUADRANTS[0b0010] = '\u2596'; // ▖ bottom left
        QUADRANTS[0b0011] = '\u2584'; // ▄ bottom half
        QUADRANTS[0b0100] = '\u259D'; // ▝ top right
        QUADRANTS[0b0101] = '\u2590'; // ▐ right half
        QUADRANTS[0b0110] = '\u259E'; // ▞ top right + bottom left
        QUADRANTS[0b0111] = '\u259F'; // ▟ top right + bottom left + bottom right
        QUADRANTS[0b1000] = '\u2598'; // ▘ top left
        QUADRANTS[0b1001] = '\u259A'; // ▚ top left + bottom right
        QUADRANTS[0b1010] = '\u258C'; // ▌ left half
        QUADRANTS[0b1011] = '\u2599'; // ▙ top left + bottom left + bottom right
        QUADRANTS[0b1100] = '\u2580'; // ▀ top half
        QUADRANTS[0b1101] = '\u259C'; // ▜ top left + top right + bottom right
        QUADRANTS[0b1110] = '\u259B'; // ▛ top left + top right + bottom left
        QUADRANTS[0b1111] = FULL_BLOCK; // █ all 4 bits set
    }

    private final QrCode qr;
    private final int size;
    private final int border;

    /**
     * Creates the renderer with a default border (silent zone) of 4 modules,
     * the minimum recommended by the QR standard.
     *
     * @param qr QR code already generated with fastqrcodegen
     */
    public AsciiQrCode(QrCode qr)
    {
        this(qr, 4);
    }

    /**
     * @param qr QR code already generated with fastqrcodegen
     * @param border border size (silent zone) in modules to add around the QR;
     * can be 0 if no border is desired
     */
    public AsciiQrCode(QrCode qr, int border)
    {
        this.qr = Objects.requireNonNull(qr, "qr no puede ser null");
        if (border < 0)
        {
            throw new IllegalArgumentException("border no puede ser negativo");
        }
        this.size = qr.size;
        this.border = border;
    }

    /**
     * Indicates whether the module at coordinate (x, y) —including the border— is
     * "on" (dark). Outside the QR bounds, or within the quiet zone, it is always
     * considered clear (false).
     */
    private boolean isDark(int x, int y)
    {
        int qx = x - border;
        int qy = y - border;
        if (qx < 0 || qy < 0 || qx >= size || qy >= size)
        {
            return false;
        }
        return qr.getModule(qx, qy);
    }

    private int totalSize()
    {
        return size + border * 2;
    }

    /**
     * Mode 1: each module is represented with TWO full block characters,
     * or two spaces if the module is off.
     */
    public String render2CharsPerBit()
    {
        int n = totalSize();
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < n; y++)
        {
            for (int x = 0; x < n; x++)
            {
                if (isDark(x, y))
                {
                    sb.append(FULL_BLOCK).append(FULL_BLOCK);
                }
                else
                {
                    sb.append("  ");
                }
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * Mode 2: each module is represented with a single character: full block
     * if on, space if off.
     */
    public String render1CharPerBit()
    {
        int n = totalSize();
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < n; y++)
        {
            for (int x = 0; x < n; x++)
            {
                sb.append(isDark(x, y) ? FULL_BLOCK : ' ');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * Mode 3: each character represents TWO modules stacked vertically,
     * using half-block characters ▀, ▄, █, or space.
     */
    public String render2VerticalBitsPerChar()
    {
        int n = totalSize();
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < n; y += 2)
        {
            for (int x = 0; x < n; x++)
            {
                boolean top = isDark(x, y);
                boolean bottom = isDark(x, y + 1);
                char c;
                if (top && bottom)
                {
                    c = FULL_BLOCK;
                }
                else if (top)
                {
                    c = UPPER_HALF;
                }
                else if (bottom)
                {
                    c = LOWER_HALF;
                }
                else
                {
                    c = ' ';
                }
                sb.append(c);
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * Mode 4: each character represents a 2x2 block of modules, using Unicode
     * quadrant characters, covering all 16 possible combinations.
     */
    public String render4BitsPerChar()
    {
        int n = totalSize();
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < n; y += 2)
        {
            for (int x = 0; x < n; x += 2)
            {
                int tl = isDark(x, y) ? 1 : 0;
                int tr = isDark(x + 1, y) ? 1 : 0;
                int bl = isDark(x, y + 1) ? 1 : 0;
                int br = isDark(x + 1, y + 1) ? 1 : 0;
                int index = (tl << 3) | (tr << 2) | (bl << 1) | br;
                sb.append(QUADRANTS[index]);
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * Default representation: uses mode 3
     * ({@link #render2VerticalBitsPerChar()}), which typically offers the best
     * balance between resolution and size in a terminal.
     */
    @Override
    public String toString()
    {
        return render2VerticalBitsPerChar();
    }

}
