package io.nut.core.qr;

import io.nayuki.qrcodegen.QrCode;
import org.junit.jupiter.api.Test;

public class AsciiQrCodeTest
{

    @Test
    public void test1()
    {
        QrCode qr = QrCode.encodeText("HELLO WORLD!", QrCode.Ecc.MEDIUM);
        AsciiQrCode ascii = new AsciiQrCode(qr);

        System.out.println("=== Mode 1: 2 characters per bit ===");
        System.out.println(ascii.render2CharsPerBit());

        System.out.println("=== Mode 2: 1 character per bit ===");
        System.out.println(ascii.render1CharPerBit());

        System.out.println("=== Mode 3: 2 vertical bits per character ===");
        System.out.println(ascii.render2VerticalBitsPerChar());

        System.out.println("=== Mode 4: 4 bits (2x2) per character ===");
        System.out.println(ascii.render4BitsPerChar());
    }

}
