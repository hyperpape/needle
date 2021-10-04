package com.justinblank.strings;

public class ByteClassUtil {

    public static void fillBytes(byte[] bytes, byte state, int startingIndex, int endingIndex) {
        for (int i = startingIndex; i <= endingIndex; i++) {
            bytes[i] = state;
        }
    }

    public static int maxByteClass(byte[] bytes) {
        int max = Integer.MIN_VALUE;
        for (int i : bytes) {
            if (i > max) {
                max = i;
            }
        }
        return max;
    }
}
