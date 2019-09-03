package com.justinblank.strings.Search;

import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SearchMethods {

    private SearchMethods() {}

    public static int findIndex(String pattern, String target) {
        return makeSearchMethod(pattern).findIndex(target);
    }

    public static SearchMethod makeSearchMethod(String pattern) {
        Set<Character> patternChars = new HashSet<>();
        for (char c : pattern.toCharArray()) {
            patternChars.add(c);
        }
        if (patternChars.size() == pattern.length()) {
            return new UniqueCharPattern(pattern);
        }
        if (patternChars.size() < BNDMSearch.MAX_PATTERN_SIZE && allAscii(pattern)) {
            return BNDMSearch.prepare(pattern);
        }
        else {
            return HorspoolTable.mkTable(pattern, patternChars);
        }
    }

    public static SearchMethod makeSearchMethod(List<String> strings) {
        if (strings.size() == 1) {
            return makeSearchMethod(strings.get(0));
        }
        else if (allAscii(strings)) {
            return AsciiAhoCorasickBuilder.buildAhoCorasick(strings);
        }
        throw new UnsupportedOperationException("Multistring matching is not yet implemented for non-ascii patterns");
    }

    // I keep double-checking StringUtils for where I missed this
    protected static boolean allAscii(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c > '\u007F') {
                return false;
            }
        }
        return true;
    }

    protected static boolean allAscii(List<String> strings) {
        for (String s : strings) {
            if (!allAscii(s)) {
                return false;
            }
        }
        return true;
    }
}
