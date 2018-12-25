package com.justinblank.strings.Search;

/**
 * BNDMSearch for ASCII chars
 */
class BNDMSearch implements SearchMethod {

    public static final int MAX_PATTERN_SIZE = 64;
    public static final int MAX_CHAR = 128;
    private final String pattern;
    private final int patternLength;
    private final long[] masks;

    private BNDMSearch(String pattern) {
        this.pattern = pattern;
        this.patternLength = pattern.length();
        this.masks = makeMasks(pattern);
    }

    // TODO name
    private long[] makeMasks(String pattern) {
        long[] masks = new long[MAX_CHAR];
        for (int i = 0; i < MAX_CHAR; i++) {
            masks[i] = 0;
        }
        long nextMask = 1; // TODO name
        for (int i = patternLength - 1; i >= 0; i--) {
            char c = pattern.charAt(i);
            masks[(int) c] |= nextMask;
            nextMask <<= 1;
        }
        return masks;
    }

    static BNDMSearch prepare(String pattern) {
        if (pattern.length() > MAX_PATTERN_SIZE) {
            throw new IllegalArgumentException("BNDM does not support patterns with more than 64 bits");
        }
        return new BNDMSearch(pattern);
    }

    @Override
    public int findIndex(String s) {
        long strLen = s.length();
        int position = 0;
        while (position <= strLen - patternLength) {
            int i = patternLength - 1;
            long state = ~0;
            int last = patternLength;
            while (i >= 0 && state != 0) {
                state &= masks[(int) s.charAt(position + i)];
                i--;
                if (state != 0) {
                    if (i < 0) {
                        return position;
                    } else {
                        last = i + 1;
                    }
                }
                state <<= 1;
            }
            position += last;
        }
        return -1;
    }
}
