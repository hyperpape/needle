package com.justinblank.strings.Search;

/**
 * A search method for exact matches of a literal pattern where all chars are unique.
 *
 * <p>Uniqueness allows us to improve on Boyer-Moore by simplifying the step of computing how far forward to jump
 * when we have failed to match our pattern.</p>
 */
class UniqueCharPattern implements SearchMethod {

    private final int patternLength;
    private final String pattern;

    UniqueCharPattern(String pattern) {
        this.pattern = pattern;
        this.patternLength = pattern.length();
    }

    @Override
    public int findIndex(String hayStack) {
        final int hayStackLength = hayStack.length();
        int i = patternLength - 1;
        int hayStackIndex = patternLength - 1;
        while (i >= 0 && hayStackIndex < hayStackLength) {
            final char next = hayStack.charAt(hayStackIndex);
            if (next != pattern.charAt(i)) {
                hayStackIndex += patternLength - i;
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
