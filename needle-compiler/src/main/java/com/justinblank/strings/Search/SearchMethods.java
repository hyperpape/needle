package com.justinblank.strings.Search;

import java.util.Collection;
import java.util.List;

public final class SearchMethods {

    private SearchMethods() {}

    public static SearchMethod makeSearchMethod(Collection<String> strings) {
        if (strings.isEmpty()) {
            throw new IllegalArgumentException("Cannot create SearchMethod using empty list of strings");
        }
        if (allAscii(strings)) {
            return AsciiAhoCorasickBuilder.buildAhoCorasick(strings);
        }
        else {
            return UnicodeAhoCorasickBuilder.buildAhoCorasick(strings);
        }
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

    protected static boolean allAscii(Collection<String> strings) {
        for (String s : strings) {
            if (!allAscii(s)) {
                return false;
            }
        }
        return true;
    }
}
