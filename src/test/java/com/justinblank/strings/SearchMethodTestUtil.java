package com.justinblank.strings;

import com.justinblank.strings.Search.SearchMethod;

import static org.junit.Assert.*;

public class SearchMethodTestUtil {

    public static void find(SearchMethod method, String s, int start, int end) {
        assertTrue(method.containedIn(s));
        MatchResult result = method.find(s, start, end);
        assertTrue(result.matched);
        assertTrue(method.find(s, start, end, true).matched);
        assertTrue(method.matches(s.substring(result.start, result.end)));
    }

    public static void find(SearchMethod method, String s) {
        find(method, s, 0, s.length());
    }

    public static void fail(SearchMethod method, String s, int start, int end) {
        assertFalse(method.find(s, start, end).matched);
        assertFalse(method.find(s, start, end, true).matched);
        assertFalse(method.matches(s.substring(start, end)));
    }

    public static void fail(SearchMethod method, String s) {
        fail(method, s, 0, s.length());
    }

    public static void match(SearchMethod method, String s) {
        assertTrue(method.matches(s));
        assertEquals(MatchResult.success(0, s.length()), method.find(s));
        find(method, s, 0, s.length());
    }
}
