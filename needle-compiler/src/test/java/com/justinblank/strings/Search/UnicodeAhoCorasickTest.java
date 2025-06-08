package com.justinblank.strings.Search;

import com.justinblank.strings.MatchResult;
import org.junit.jupiter.api.Test;
import org.quicktheories.QuickTheory;
import org.quicktheories.core.Gen;
import org.quicktheories.generators.IntegersDSL;
import org.quicktheories.generators.ListsDSL;
import org.quicktheories.generators.StringsDSL;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UnicodeAhoCorasickTest {

    private static final String LITERAL_1 = "ab";
    private static final String LITERAL_2 = "bc";;
    private static final String LITERAL_3 = "cd";

    @Test
    void singleStringPatternContainedIn() {
        List<String> patterns = List.of(LITERAL_1);
        SearchMethod method = UnicodeAhoCorasickBuilder.buildAhoCorasick(patterns);
        assertNotNull(method);
        assertTrue(method.containedIn(LITERAL_1));
        assertTrue(method.containedIn("a" + LITERAL_1));
        assertTrue(method.containedIn(LITERAL_1 + "a"));
        assertFalse(method.containedIn("de"));
    }

    @Test
    void singleStringPatternMatches() {
        List<String> patterns = List.of(LITERAL_1);
        SearchMethod method = UnicodeAhoCorasickBuilder.buildAhoCorasick(patterns);
        assertNotNull(method);
        assertTrue(method.matches(LITERAL_1));
        assertFalse(method.matches("a" + LITERAL_1));
        assertFalse(method.matches(LITERAL_1 + "a"));
        assertFalse(method.containedIn("de"));
    }

    @Test
    void singleStringPatternMatchResult() {
        List<String> patterns = List.of(LITERAL_1);
        SearchMethod method = UnicodeAhoCorasickBuilder.buildAhoCorasick(patterns);
        assertNotNull(method);
        assertEquals(MatchResult.success(0, 2), method.find(LITERAL_1));
        assertEquals(MatchResult.success(1, 3), method.find("a" + LITERAL_1));
        assertEquals(MatchResult.success(0, 2), method.find(LITERAL_1 + "a"));
        assertFalse(method.find("de").matched);
    }


    @Test
    void singleStringPatternRepetition() {
        List<String> patterns = List.of(LITERAL_1);
        SearchMethod method = UnicodeAhoCorasickBuilder.buildAhoCorasick(patterns);
        assertNotNull(method);
        assertTrue(method.containedIn("a" + LITERAL_1));
    }

    @Test
    void multiStringPatternSingleLiteral() {
        List<String> patterns = List.of(LITERAL_1, LITERAL_2, LITERAL_3);
        SearchMethod method = UnicodeAhoCorasickBuilder.buildAhoCorasick(patterns);
        assertNotNull(method);
        assertTrue(method.containedIn(LITERAL_1));
        assertTrue(method.containedIn(LITERAL_2));
        assertTrue(method.containedIn(LITERAL_3));
        assertFalse(method.containedIn("de"));
    }

    @Test
    void repeatedPatterns() {
        List<String> patterns = List.of("a", "aaa");
        SearchMethod method = UnicodeAhoCorasickBuilder.buildAhoCorasick(patterns);
        assertNotNull(method);
    }

    @Test
    void multiStringPatternIrrelevantChars() {
        List<String> patterns = List.of(LITERAL_1, LITERAL_2, LITERAL_3);
        SearchMethod method = UnicodeAhoCorasickBuilder.buildAhoCorasick(patterns);
        assertNotNull(method);
        assertTrue(method.containedIn("zxyz" + LITERAL_1 + "zxyz"));
        assertTrue(method.containedIn("zxyz" + LITERAL_2 + "zxyz"));
        assertTrue(method.containedIn("zxyz" + LITERAL_3 + "zxyz"));
    }

    @Test
    void multiStringPattern() {
        List<String> patterns = List.of(LITERAL_1, LITERAL_2, LITERAL_3);
        SearchMethod method = UnicodeAhoCorasickBuilder.buildAhoCorasick(patterns);
        assertNotNull(method);
        assertTrue(method.containedIn("za" + LITERAL_1 + "ad"));
        assertTrue(method.containedIn("zb" + LITERAL_2 + "bd"));
        assertTrue(method.containedIn("zc" + LITERAL_3 + "cd"));
    }

    @Test
    void all() {
        Gen<String> stringGen = new StringsDSL().betweenCodePoints(1, 256 * 256).ofLengthBetween(1, 12);
        int listSize = 100;
        Gen<List<String>> patternStrings = new ListsDSL().of(stringGen).ofSizeBetween(1, listSize);
        Gen<Integer> indexGen = new IntegersDSL().between(0, listSize - 1);
        Gen<String> all = new StringsDSL().allPossible().ofLengthBetween(0, 24);
        QuickTheory.qt().forAll(patternStrings, indexGen, all, all).check((patterns, index, left, right) -> {
            try {
                SearchMethod method = UnicodeAhoCorasickBuilder.buildAhoCorasick(patterns);
                String match = patterns.get(index % patterns.size());
                String hayStack = left + match + right;
                return method.containedIn(hayStack);
            }
            catch (Exception e) {
                e.printStackTrace();
                return false;
            }

        });
    }

    @Test
    void allBuild() {
        Gen<String> stringGen = new StringsDSL().betweenCodePoints(0, 256 * 256).ofLengthBetween(1, 100);
        Gen<List<String>> patternStrings = new ListsDSL().of(stringGen).ofSizeBetween(1, 1);
        QuickTheory.qt().forAll(patternStrings).check((patterns) -> {
            SearchMethod method = UnicodeAhoCorasickBuilder.buildAhoCorasick(patterns);
            return method != null;
        });
    }

}
