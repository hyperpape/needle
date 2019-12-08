package com.justinblank.strings.Search;

import java.util.List;

public final class SearchMethods {

    private SearchMethods() {}

    public static SearchMethod makeSearchMethod(List<String> strings) {
        if (strings.isEmpty()) {
            throw new IllegalArgumentException("Cannot create SearchMethod using empty list of strings");
        }
        if (containsEmpty(strings)) {
            return AsciiAhoCorasickBuilder.buildEmptyAhoCorasick();
        }
        if (allAscii(strings)) {
            return AsciiAhoCorasickBuilder.buildAhoCorasick(strings);
        }
        else {
            return UnicodeAhoCorasickBuilder.buildAhoCorasick(strings);
        }
    }

    private static boolean containsEmpty(List<String> strings) {
        for (String s : strings) {
            if ("".equals(s)) {
                return true;
            }
        }
        return false;
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
