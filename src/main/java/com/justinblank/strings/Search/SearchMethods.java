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
        else {
            return HorspoolTable.mkTable(pattern, patternChars);
        }
    }
}
