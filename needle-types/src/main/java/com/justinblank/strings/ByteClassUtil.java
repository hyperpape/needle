package com.justinblank.strings;

public class ByteClassUtil {

    public static void fillBytes(byte[] bytes, byte state, int startingIndex, int endingIndex) {
        for (var i = startingIndex; i <= endingIndex; i++) {
            bytes[i] = state;
        }
    }

    public static int maxByteClass(byte[] bytes) {
        int max = Integer.MIN_VALUE;
        for (var i = 0; i < bytes.length; i++) {
            if (bytes[i] > max) {
                max = bytes[i];
            }
        }
        return max;
    }
}
