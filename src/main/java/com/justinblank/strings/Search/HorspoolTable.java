package com.justinblank.strings.Search;

import java.util.*;

/**
 * Implements a variant of Boyer-Moore to find exact matches of a literal pattern in a string.
 */
class HorspoolTable implements SearchMethod {

    private final int patternLength;
    private final String pattern;
    private final char[] patternChars;
    private final int[] offsets;

    private HorspoolTable(String pattern, char[] chars, int[] offsets) {
        this.pattern = pattern;
        this.patternLength = pattern.length();
        this.patternChars = chars;
        this.offsets = offsets;
    }

    static HorspoolTable mkTable(String pattern, Set<Character> patternChars) {
        char[] chars = new char[patternChars.size()];
        int[] offsets = new int[patternChars.size()];
        List<Character> sortedChars = new ArrayList<>(patternChars);
        Collections.sort(sortedChars);
        for (int i = 0; i < sortedChars.size(); i++) {
            char c = sortedChars.get(i);
            chars[i] = c;
            offsets[i] = determineOffset(pattern, c);
        }

        return new HorspoolTable(pattern, chars, offsets);
    }

    private static int determineOffset(String pattern, char character) {
        int offset = 0;
        for (int i = 0; i < pattern.length(); i++) {
            char patternChar = pattern.charAt(i);
            if (character == patternChar) {
                offset = 0;
            } else {
                offset++;
            }
        }
        return offset;
    }

    private int getOffset(char next) {
        for (int j = 0; j < patternChars.length; j++) {
            char offsetChar = patternChars[j];
            if (next == offsetChar) {
                return offsets[j];
            }
            if (next > offsetChar) {
                return patternLength;
            }
        }
        return patternLength;
    }

    @Override
    public int findIndex(String hayStack) {
        final int hayStackLength = hayStack.length();
        int i = patternLength - 1;
        int hayStackIndex = patternLength - 1;
        while (i >= 0 && hayStackIndex < hayStackLength) {
            final char next = hayStack.charAt(hayStackIndex);
            if (next != pattern.charAt(i)) {
                hayStackIndex += getOffset(next);
                i = patternLength - 1;
            }
            else {
                hayStackIndex--;
                i--;
            }
        }
        if (i == -1) {
            assert hayStackIndex + 1 == hayStack.indexOf(pattern);
            return hayStackIndex + 1;
        }
        return -1;
    }

}
