package com.justinblank.strings;

import com.justinblank.strings.Search.SearchMethod;
import org.quicktheories.QuickTheory;
import org.quicktheories.generators.IntegersDSL;
import org.quicktheories.generators.StringsDSL;

import static org.junit.Assert.*;

public class SearchMethodTestUtil {

    /**
     * Using a small alphabet seems to be more effective at finding bugs, but also provides more readable output and
     * better shrinking. So in many tests, I use the small alphabet first, then run with a large alphabet later. I don't
     * hide any bugs that might somehow happen with the large alphabet, but in general, I'll see the nicer output from
     * the small alphabet when debugging failures.
     */
    public static final StringsDSL.StringGeneratorBuilder SMALL_ALPHABET = new StringsDSL().betweenCodePoints(97, 101);
    public static final StringsDSL.StringGeneratorBuilder A_THROUGH_Z = new StringsDSL().betweenCodePoints(65, 90);
    public static final StringsDSL.StringGeneratorBuilder NON_METACHAR_BMP = new StringsDSL().betweenCodePoints(256, 256 * 256);
    public static final StringsDSL.StringGeneratorBuilder SMALL_BMP = new StringsDSL().betweenCodePoints(0x00c5, 0x00c9);// 0x05D0, 0x05D4);
    public static final int SMALL_DATA_SIZE = 10;
    public static final int LARGE_DATA_SIZE = 64;

    ////// Testing the find method //////

    /**
     * Test that a pattern correctly finds a needle within a String
     *
     * @param method the pattern
     * @param s      the string to search within
     * @param start  the starting point within which to search.
     * @param end    the endpoint within which to search
     *
     * Given an accurate matches method, we can specify the find method. If any substring of our string is findable,
     * then:
     * <ol>
     *     <li> find will give us a substring</li>
     *     <li> that substring will return true from the matches method</li>
     *     <li> a prefix of the string starting before the found substring will match only if it is the empty string,
     *               and the pattern matches the empty string</li>
     *     <li> no string that extends the found substring (starts at the same location but extends further) will
     *               match</li>
     *     <li> no string that starts before the found substring will match</li>
     *     <li> if the pattern matches the empty string, the substring will begin at the 0 index</li>
     * </ol>
     */
    public static void find(Pattern method, String s, int start, int end) {
        assertTrue("Wrong result for containedIn on string=\"" + s + "\"", method.matcher(s).containedIn());
        MatchResult result = method.matcher(s).find(start, end);
        // Property #1 -- we'll find substring using the find method
        assertTrue("Failed to find in string=\"" + s + "\", start=" + start + ",end=" + end, result.matched);
        // Property #2 -- that substring will match using the matches method
        assertTrue("Failed to match substring of string=\"" + s + "\", start=" + start + ",end=" + end,
                method.matcher(s.substring(result.start, result.end)).matches());

        var prefix = s.substring(start, result.start);
        // Property #3
        assertTrue(method.matcher("").matches() && prefix.isEmpty() || !method.matcher(prefix).matches());

        // Property #4 -- match cannot be extended forwards
        if (result.end < end) {
            int maxExtension = end - result.end;
            QuickTheory.qt().forAll(new IntegersDSL().between(1, maxExtension))
                    .check((extension) -> {
                        String extendedMatch = s.substring(start, result.end + extension);
                        return !method.matcher(extendedMatch).matches();
                    });
        }
        // Property #5 -- no string that starts earlier will match
        // TODO: we could also remove from end
        if (result.start > start) {
            QuickTheory.qt().forAll(new IntegersDSL().between(start, result.start - 1)).check(
                    prefixTrimLength -> !method.matcher(s.substring(prefixTrimLength, result.end)).matches()
            );
        }
        // Property 6 -- string that matches empty substring must be anchored at zero
        if (method.matcher("").matches()) {
            assertEquals(result.start, start);
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
