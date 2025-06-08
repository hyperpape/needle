package com.justinblank.strings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class ByteClassUtilTest {

    @Test
    void encodeDecode() {
        for (int i = 0; i < 4096; i++) {
            assertEquals(i, ByteClassUtil.decode(ByteClassUtil.encode(i)), "Encode/decode failure for " + i);
        }
    }

    @Test
    void fillBytesFromString() {
        var byteClasses = ByteClassUtil.fillBytesFromString(4,"1-2,2-3,3-c");
        assertEquals(2, byteClasses[1]);
        assertEquals(3, byteClasses[2]);
        assertEquals(12, byteClasses[3]);
    }

    @Test
    void fill_bytes_from_string_errors_on_bad_input() {
        try {
            ByteClassUtil.fillBytesFromString(1, "1-z");
        }
        catch (IllegalArgumentException e) {
            return;
        }
        fail("No exception thrown");
    }

    @Test
    void fillMultipleByteClassesFromStringUsingBytesSingleArray() {
        var byteClasses = new byte[13 * 4];
        ByteClassUtil.fillMultipleByteClassesFromString_singleArray(byteClasses, 4, "0:1-2,2-3,3-c;1:1-3,2-4,3-13;2:1-4,2-5,3-e;c:1-d,2-e,3-17");
        // "0:1-2,2-3,3-c;1:1-3,2-4,3-13;2:1-4,2-5,3-e;c:1-d,2-e,3-17"
        assertEquals((byte) 2, byteClasses[1]);
        assertEquals((byte) 3, byteClasses[2]);
        assertEquals((byte) 12, byteClasses[3]);
        assertEquals((byte) 3, byteClasses[5]);
        assertEquals((byte) 13, byteClasses[49]);
    }

    @Test
    void fillMultipleByteClassesFromStringUsingShortsSingleArray() {
        var byteClasses = new short[13 * 4];
        ByteClassUtil.fillMultipleByteClassesFromStringUsingShorts_singleArray(byteClasses, 4, "0:1-2,2-3,3-c;1:1-3,2-4,3-13;2:1-4,2-5,3-e;c:1-d,2-e,3-17");
        // "0:1-2,2-3,3-c;1:1-3,2-4,3-13;2:1-4,2-5,3-e;c:1-d,2-e,3-17"
        assertEquals((short) 2, byteClasses[1]);
        assertEquals((short) 3, byteClasses[2]);
        assertEquals((short) 12, byteClasses[3]);
        assertEquals((short) 3, byteClasses[5]);
        assertEquals((short) 13, byteClasses[49]);
    }

}