package com.justinblank.strings;

import java.util.Arrays;

public class ByteClassUtil {

    public static final java.util.regex.Pattern COMMA_REGEX = java.util.regex.Pattern.compile(",");
    public static final java.util.regex.Pattern DASH_REGEX = java.util.regex.Pattern.compile("-");

    public static final java.util.regex.Pattern COLON_REGEX = java.util.regex.Pattern.compile("\\:");
    public static final java.util.regex.Pattern SEMICOLON_REGEX = java.util.regex.Pattern.compile(";");

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

    /**
     * Fill arrays mapping byteClasses to target states by parsing the transitions out of a string.
     * <p>
     * The string encoding consists of semicolon delimited chunks, each of which represents the transitions for a
     * particular DFA state. The chunks have the format state:byteClass1-targetState1,byteClass2-targetState2...
     *
     * @param stateTransitionArrays the state transition arrays
     * @param length                the number of byteClasses + 1
     * @param s                     the encoded string
     */
    public static void fillMultipleByteClassesFromString(byte[][] stateTransitionArrays, int length, String s) {
        for (int i = 0; i < stateTransitionArrays.length; i++) {
            stateTransitionArrays[i] = new byte[length];
            Arrays.fill(stateTransitionArrays[i], (byte) -1);
        }
        String[] stateStrings = SEMICOLON_REGEX.split(s);
        for (String stateString : stateStrings) {
            if (stateString.isEmpty()) {
                continue;
            }
            String[] stateAndTransitions = COLON_REGEX.split(stateString);
            if (stateAndTransitions.length != 2) {
                throw new IllegalArgumentException("Malformed string, wrong number of parts. Component was: " + stateString);
            }
            try {
                int state = decode(stateAndTransitions[0]);
                stateTransitionArrays[state] = fillBytesFromString(length, stateAndTransitions[1]);
            }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException("Malformed string, could not parse integer. Component was: " + stateString);
            }
        }
    }

    public static void fillMultipleByteClassesFromStringUsingShorts(short[][] stateTransitionArrays, int length, String s) {
        for (int i = 0; i < stateTransitionArrays.length; i++) {
            if (stateTransitionArrays[i] == null) {
                stateTransitionArrays[i] = new short[length];
                Arrays.fill(stateTransitionArrays[i], (short) -1);
            }
        }
        String[] stateStrings = SEMICOLON_REGEX.split(s);
        for (String stateString : stateStrings) {
            String[] stateAndTransitions = COLON_REGEX.split(stateString);
            if (stateAndTransitions.length != 2) {
                throw new IllegalArgumentException("Malformed string, wrong number of parts. Component was: " + stateString);
            }
            try {
                int state = decode(stateAndTransitions[0]);
                stateTransitionArrays[state] = fillBytesFromStringUsingShorts(length, stateAndTransitions[1]);
            }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException("Malformed string, could not parse integer. Component was: " + stateString);
            }
        }
    }

    static int decode(String subpart) {
        return Integer.valueOf(subpart, 16);
    }

    static String encode(int i) {
        return Integer.toHexString(i);
    }

    public static byte[] fillBytesFromString(int length, String s) {
        byte[] byteClasses = new byte[length];
        Arrays.fill(byteClasses, (byte) -1);
        if (s.isEmpty()) {
            return byteClasses;
        }
        String[] components = COMMA_REGEX.split(s);
        for (String component : components) {
            String[] pieces = DASH_REGEX.split(component);
            if (pieces.length != 2) {
                throw new IllegalArgumentException("Malformed string, wrong number of parts. Component was: " + component);
            }
            try {
                int byteClass = decode(pieces[0]);
                int state = decode(pieces[1]);
                if (state > Byte.MAX_VALUE) {
                    throw new IllegalStateException("Tried to store a state that's too large to fit in a byte. State=" + state);
                }
                byteClasses[byteClass] = (byte) state;
            }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException("Malformed string, could not parse integer. Component was: " + component);
            }
        }
        return byteClasses;
    }

    public static short[] fillBytesFromStringUsingShorts(int length, String s) {
        short[] byteClasses = new short[length];
        Arrays.fill(byteClasses, (byte) -1);
        if (s.isEmpty()) {
            return byteClasses;
        }
        String[] components = COMMA_REGEX.split(s);
        for (String component : components) {
            String[] pieces = DASH_REGEX.split(component);
            if (pieces.length != 2) {
                throw new IllegalArgumentException("Malformed string, wrong number of parts. Component was: " + component);
            }
            try {
                int byteClass = decode(pieces[0]);
                int state = decode(pieces[1]);
                if (state > Short.MAX_VALUE) {
                    throw new IllegalStateException("Tried to store a state that's too large to fit in a short. State=" + state);
                }
                byteClasses[byteClass] = (short) state;
            }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException("Malformed string, could not parse integer. Component was: " + component);
            }
        }
        return byteClasses;
    }
}
