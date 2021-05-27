package com.justinblank.strings;

import com.justinblank.strings.Search.SearchMethod;

import static org.junit.Assert.*;

public class SearchMethodTestUtil {

    public static void find(Pattern method, String s, int start, int end) {
        assertTrue("Wrong result for containedIn on string=\"" + s + "\"", method.matcher(s).containedIn());
        MatchResult result = method.matcher(s).find(start, end);
        assertTrue(result.matched);
        assertTrue("Failed to find in string=\"" + s + "\", start=" + start + ",end=" + end, method.matcher(s).find(start, end).matched);
        assertTrue("Failed to match substring of string=\"" + s + "\", start=" + start + ",end=" + end, method.matcher(s.substring(result.start, result.end)).matches());
    }

    public static void find(Pattern method, String s) {
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

    public static void fail(Pattern pattern, String s) {
        assertFalse(pattern.matcher(s).containedIn());
        assertFalse(pattern.matcher(s).matches());
    }

    public static void match(Pattern method, String s) {
        assertTrue("Failed match for string=\"" + s + "\"", method.matcher(s).matches());
        assertTrue("Failed match for string=\"" + s + "\"", method.matcher(s).matches());
        assertEquals("Failed find for string='" + s + "'", MatchResult.success(0, s.length()), method.matcher(s).find());
        find(method, s, 0, s.length());
    }
}
