package com.justinblank.strings;

import com.justinblank.strings.Search.SearchMethod;
import org.quicktheories.generators.StringsDSL;

import static org.junit.Assert.*;

public class SearchMethodTestUtil {

    /**
     * Using a small alphabet seems to be more effective at finding bugs, but also provides more readable output and
     * better shrinking. So in many tests, I use the small alphabet first, then run with a large alphabet later. I don't
     * hide any bugs that might somehow happen with the large alphabet, but in general, I'll see the nicer output from
     * the small alphabet if need be.
     */
    public static final StringsDSL.StringGeneratorBuilder SMALL_ALPHABET = new StringsDSL().betweenCodePoints(97, 101);
    public static final StringsDSL.StringGeneratorBuilder A_THROUGH_Z = new StringsDSL().betweenCodePoints(65, 90);
    public static final StringsDSL.StringGeneratorBuilder NON_METACHAR_BMP = new StringsDSL().betweenCodePoints(256, 256 * 256);
    public static final StringsDSL.StringGeneratorBuilder SMALL_BMP = new StringsDSL().betweenCodePoints(0x00c5, 0x00c9);// 0x05D0, 0x05D4);
    public static final int SMALL_DATA_SIZE = 10;
    public static final int LARGE_DATA_SIZE = 64;

    public static void find(Pattern method, String s, int start, int end) {
        assertTrue("Wrong result for containedIn on string=\"" + s + "\"", method.matcher(s).containedIn());
        MatchResult result = method.matcher(s).find(start, end);
        assertTrue(result.matched);
        assertTrue("Failed to find in string=\"" + s + "\", start=" + start + ",end=" + end, method.matcher(s).find(start, end).matched);
        assertTrue("Failed to match substring of string=\"" + s + "\", start=" + start + ",end=" + end, method.matcher(s.substring(result.start, result.end)).matches());

        var prefix = s.substring(start, result.start);
        // Can't check an empty prefix: consider a* matching 'aaa'
        if (!prefix.isEmpty()) {
            assertFalse(method.matcher(prefix).find().matched);
        }
    }

    public static void find(Pattern method, String s, String prefix, String suffix) {
        find(method, s);
        find(method, prefix + s);
        find(method, prefix + s + suffix);
        find(method, s + suffix);
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
        find(method, s);
    }
}
