package com.justinblank.strings;

import junit.framework.TestCase;
import org.junit.Test;

public class ByteClassUtilTest extends TestCase {

    public void testEncodeDecode() {
        for (int i = 0; i < 4096; i++) {
            assertEquals("Encode/decode failure for " + i, i, ByteClassUtil.decode(ByteClassUtil.encode(i)));
        }
    }

    public void testFillBytesFromString() {
        var byteClasses = ByteClassUtil.fillBytesFromString(4,"1-2,2-3,3-c");
        assertEquals(byteClasses[1], 2);
        assertEquals(byteClasses[2], 3);
        assertEquals(byteClasses[3], 12);
    }

    public void test_fillBytesFromString_errorsOnBadInput() {
        try {
            ByteClassUtil.fillBytesFromString(1, "1-z");
        }
        catch (IllegalArgumentException e) {
            return;
        }
        fail("No exception thrown");
    }

    public void testFillMultipleByteClassesFromString() {
        var byteClasses = new byte[13][];
        ByteClassUtil.fillMultipleByteClassesFromString(byteClasses, 4, "0:1-2,2-3,3-c;1:1-3,2-4,3-13;2:1-4,2-5,3-e;c:1-d,2-e,3-17");
        assertEquals((byte) 2, byteClasses[0][1]);
        assertEquals((byte) 3, byteClasses[1][1]);
        assertEquals((byte) 4, byteClasses[2][1]);
        assertEquals((byte) 13, byteClasses[12][1]);
    }

    @Test
    public void testFillMultipleByteClassesFromStringUsingShorts() {
        var byteClasses = new short[13][];
        ByteClassUtil.fillMultipleByteClassesFromStringUsingShorts(byteClasses, 4, "0:1-2,2-3,3-c;1:1-3,2-4,3-13;2:1-4,2-5,3-e;c:1-d,2-e,3-17");
        assertEquals((short) 2, byteClasses[0][1]);
        assertEquals((short) 3, byteClasses[1][1]);
        assertEquals((short) 4, byteClasses[2][1]);
        assertEquals((short) 13, byteClasses[12][1]);
    }

}