package com.justinblank.strings;

import static org.junit.Assert.*;

public class MatchTestUtil {

    public static void checkMatch(NFA nfa, String s, int start, int end) {
        MatchResult matchResult = nfa.search(s);
        assertTrue(matchResult.matched);
        assertEquals(start, matchResult.start);
        assertEquals(end, matchResult.end);
    }

    public static void checkSearchFail(NFA nfa, String s) {
        assertFalse(nfa.search(s).matched);
    }
}
