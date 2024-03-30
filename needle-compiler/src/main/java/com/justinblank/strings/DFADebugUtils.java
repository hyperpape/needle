package com.justinblank.strings;

import java.util.Arrays;

public class DFADebugUtils {

    public static void debugState(int state, char c) {
        System.out.println("State=" + state + ", char=" + encodeChr(c));
    }

    public static void debugStateTransition(int newState) {
        System.out.println("NewState=" + newState);
    }

    public static void returnWasAccepted(int state) {
        System.out.println("Returning was accepted state=" + state);
    }

    public static void debugIndexForwards(int index) {
        System.out.println("Index forwards was " + index);
    }

    public static void debugCallWasAccepted(int state) {
        System.out.println("Calling was accepted on " + state);
    }

    public static void failedLookAheadCheck(int index, int computedIndex) {
        System.out.println("Failed lookahead check index=" + index + ", offsetIndex=" + computedIndex);
    }

    public static void debugStateArrays(String stateType, byte[] stateTransitions) {
        System.out.println("StateTransitions, type=" + stateType + Arrays.toString(stateTransitions));
    }

    public static void debugStateArrays(String stateType, short[] stateTransitions) {
        System.out.println("StateTransitions, type=" + stateType + Arrays.toString(stateTransitions));
    }

    private static String encodeChr(char c) {
        switch (c) {
            case '\n':
                return "\\n";
            case '\r':
                return "\\r";
            case '\f':
                return "\\f";
            case '\"':
                return "\\\"";
            case '\b':
                return "\\b";
            case '\\':
                return "\\\\";
            case '\t':
                return "\\t";
            default:
                if (c < ' ') {
                    return "\\u00" + (Integer.toHexString((int) c));
                }
                return String.valueOf(c);
        }
    }
}
