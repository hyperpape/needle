package com.justinblank.strings;

import java.util.Arrays;

public class ByteClassUtil {

    /**
     * We encode a byteclass transitions with [byteclass]-[targetState], e.g. 2-3.
     */
    public static char STATE_TRANSITION_DELINEATOR = '-';

    /**
     * Delineator that separates two byte class transitions for the same state. e.g. 2-3,4-5
     */
    public static char BYTE_CLASS_DELINEATOR = ',';


    /**
     * Delineator between a state and its transitions, [stateNumber]:[byteClass]-[targetState],[byteClass]-[targetState]...
     * e.g. 2:1-2,2-3
     */
    public static char STATE_TO_TRANSITIONS_DELINEATOR = ':';

    // TODO: wait, what do we use this for? 
    public static char STATE_DELINEATOR = ';';

    public static final java.util.regex.Pattern COMMA_REGEX = java.util.regex.Pattern.compile(String.valueOf(BYTE_CLASS_DELINEATOR));
    public static final java.util.regex.Pattern DASH_REGEX = java.util.regex.Pattern.compile(String.valueOf(STATE_TRANSITION_DELINEATOR));

    public static final java.util.regex.Pattern COLON_REGEX = java.util.regex.Pattern.compile("\\" + STATE_TO_TRANSITIONS_DELINEATOR);
    public static final java.util.regex.Pattern SEMICOLON_REGEX = java.util.regex.Pattern.compile(";");

    public static void fillBytes(byte[] bytes, byte state, int startingIndex, int endingIndex) {
        for (int i = startingIndex; i <= endingIndex; i++) {
            bytes[i] = state;
        }
    }

    public static void fillMultipleByteClassesFromString_singleArray(byte[] stateTransitionArray, int length, String s) {
        String[] stateStrings = SEMICOLON_REGEX.split(s);
        for (String stateString : stateStrings) {
            String[] stateAndTransitions = COLON_REGEX.split(stateString);
            if (stateAndTransitions.length != 2) {
                throw new IllegalArgumentException("Malformed string, wrong number of parts. Component was: " + stateString);
            }
            try {
                int state = decode(stateAndTransitions[0]);
                if (state > Short.MAX_VALUE) {
                    throw new IllegalStateException("Tried to store a source state that's too large to fit in a short. State=" + state);
                }
                String[] components = COMMA_REGEX.split(stateAndTransitions[1]);
                for (String component : components) {
                    String[] pieces = DASH_REGEX.split(component);
                    if (pieces.length != 2) {
                        throw new IllegalArgumentException("Malformed string, wrong number of parts. Component was: " + component);
                    }
                    try {
                        int byteClass = decode(pieces[0]);
                        int targetState = decode(pieces[1]);
                        if (targetState > Short.MAX_VALUE) {
                            throw new IllegalStateException("Tried to store a target state that's too large to fit in a short. State=" + state);
                        }
                        int offset = state * length + byteClass;
                        stateTransitionArray[offset] = (byte) targetState;
                    }
                    catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Malformed string, could not parse integer. Component was: " + component);
                    }
                }
            }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException("Malformed string, could not parse integer. Component was: " + stateString);
            }
        }
    }

    public static void fillMultipleByteClassesFromStringUsingShorts_singleArray(short[] stateTransitionArray, int length, String s) {
        String[] stateStrings = SEMICOLON_REGEX.split(s);
        for (String stateString : stateStrings) {
            String[] stateAndTransitions = COLON_REGEX.split(stateString);
            if (stateAndTransitions.length != 2) {
                throw new IllegalArgumentException("Malformed string, wrong number of parts. Component was: " + stateString);
            }
            try {
                int state = decode(stateAndTransitions[0]);
                if (state > Short.MAX_VALUE) {
                    throw new IllegalStateException("Tried to store a source state that's too large to fit in a short. State=" + state);
                }
                String[] components = COMMA_REGEX.split(stateAndTransitions[1]);
                for (String component : components) {
                    String[] pieces = DASH_REGEX.split(component);
                    if (pieces.length != 2) {
                        throw new IllegalArgumentException("Malformed string, wrong number of parts. Component was: " + component);
                    }
                    try {
                        int byteClass = decode(pieces[0]);
                        int targetState = decode(pieces[1]);
                        if (targetState > Short.MAX_VALUE) {
                            throw new IllegalStateException("Tried to store a target state that's too large to fit in a short. State=" + state);
                        }
                        int offset = state * length + byteClass;
                        stateTransitionArray[offset] = (short) targetState;
                    }
                    catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Malformed string, could not parse integer. Component was: " + component);
                    }
                }
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
}
