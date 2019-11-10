package com.justinblank.strings.Search;

import com.justinblank.strings.MatchResult;
import com.justinblank.strings.Pattern;

public interface SearchMethod {

    default int findIndex(String s) {
        MatchResult result = find(s);
        if (result.matched) {
            return result.start;
        }
        return -1;
    }

    MatchResult find(String s);

    MatchResult find(String s, int start, int end);

    MatchResult find(String s, int start, int end, boolean anchored);

    boolean matches(String s);

    default boolean containedIn(String s) {
        return findIndex(s) != -1;
    }
}
