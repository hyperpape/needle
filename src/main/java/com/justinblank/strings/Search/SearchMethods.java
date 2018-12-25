package com.justinblank.strings.Search;

import java.util.HashSet;
import java.util.Set;

// TODO: Not the best class name
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

    protected static boolean allAscii(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c > '\u007F') {
                return false;
            }
        }
        return true;
    }
}
