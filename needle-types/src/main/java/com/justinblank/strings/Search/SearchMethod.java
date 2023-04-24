package com.justinblank.strings.Search;

import com.justinblank.strings.MatchResult;
import com.justinblank.strings.Matcher;
import com.justinblank.strings.Pattern;

public interface SearchMethod extends Pattern {

    default int findIndex(String s) {
        MatchResult result = find(s);
        if (result.matched) {
            return result.start;
        }
        return -1;
    }

    default MatchResult find(String s) {
        return find(s, 0, s.length());
    }

    default MatchResult find(String s, int start, int end) {
        return find(s, start, end, false);
    }

    MatchResult find(String s, int start, int end, boolean anchored);

    default boolean matches(String s) {
        // TODO: this will have bad perf...could search entire string for matching
        MatchResult result = find(s, 0, s.length(), true);
        // TODO: Note that if anchored was working correctly, the check for result.start would not be required
        return result.matched && result.start == 0 && result.end == s.length();
    }

    default Matcher matcher(String s) {
        return new SearchMethodMatcher(this, s);
    }

    default boolean containedIn(String s) {
        return findIndex(s) != -1;
    }
}
