package com.justinblank.strings.Search;

import org.junit.Test;
import org.quicktheories.QuickTheory;
import org.quicktheories.core.Gen;
import org.quicktheories.generators.IntegersDSL;
import org.quicktheories.generators.ListsDSL;
import org.quicktheories.generators.StringsDSL;

import java.util.List;

import static org.junit.Assert.*;

public class ASCIIAhoCorasickTest {

    private static final String LITERAL_1 = "ab";
    private static final String LITERAL_2 = "bc";;
    private static final String LITERAL_3 = "cd";

    @Test
    public void testSingleStringPattern() {
        List<String> patterns = List.of(LITERAL_1);
        SearchMethod method = AsciiAhoCorasickBuilder.buildAhoCorasick(patterns);
        assertNotNull(method);
        assertTrue(method.containedIn(LITERAL_1));
        assertFalse(method.containedIn("de"));
    }

    @Test
    public void testSingleStringPatternRepetition() {
        List<String> patterns = List.of(LITERAL_1);
        SearchMethod method = AsciiAhoCorasickBuilder.buildAhoCorasick(patterns);
        assertNotNull(method);
        assertTrue(method.containedIn("a" + LITERAL_1));
    }

    @Test
    public void testMultiStringPatternSingleLiteral() {
        List<String> patterns = List.of(LITERAL_1, LITERAL_2, LITERAL_3);
        SearchMethod method = AsciiAhoCorasickBuilder.buildAhoCorasick(patterns);
        assertNotNull(method);
        assertTrue(method.containedIn(LITERAL_1));
        assertTrue(method.containedIn(LITERAL_2));
        assertTrue(method.containedIn(LITERAL_3));
        assertFalse(method.containedIn("de"));
    }

    @Test
    public void testRepeatedPatterns() {
        List<String> patterns = List.of("a", "aaa");
        SearchMethod method = AsciiAhoCorasickBuilder.buildAhoCorasick(patterns);
        assertNotNull(method);
    }

    @Test
    public void testMultiStringPatternIrrelevantChars() {
        List<String> patterns = List.of(LITERAL_1, LITERAL_2, LITERAL_3);
        SearchMethod method = AsciiAhoCorasickBuilder.buildAhoCorasick(patterns);
        assertNotNull(method);
        assertTrue(method.containedIn("zxyz" + LITERAL_1 + "zxyz"));
        assertTrue(method.containedIn("zxyz" + LITERAL_2 + "zxyz"));
        assertTrue(method.containedIn("zxyz" + LITERAL_3 + "zxyz"));
    }

    @Test
    public void testMultiStringPattern() {
        List<String> patterns = List.of(LITERAL_1, LITERAL_2, LITERAL_3);
        SearchMethod method = AsciiAhoCorasickBuilder.buildAhoCorasick(patterns);
        assertNotNull(method);
        assertTrue(method.containedIn("za" + LITERAL_1 + "ad"));
        assertTrue(method.containedIn("zb" + LITERAL_2 + "bd"));
        assertTrue(method.containedIn("zc" + LITERAL_3 + "cd"));
    }

    @Test
    public void testAll() {
        Gen<String> stringGen = new StringsDSL().ascii().ofLengthBetween(1, 12);
        int listSize = 100;
        Gen<List<String>> patternStrings = new ListsDSL().of(stringGen).ofSizeBetween(1, listSize);
        Gen<Integer> indexGen = new IntegersDSL().between(0, listSize - 1);
        Gen<String> all = new StringsDSL().allPossible().ofLengthBetween(0, 24);
        QuickTheory.qt().forAll(patternStrings, indexGen, all, all).check((patterns, index, left, right) -> {
            try {
                SearchMethod method = AsciiAhoCorasickBuilder.buildAhoCorasick(patterns);
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
    public void testAllBuild() {
        Gen<String> stringGen = new StringsDSL().ascii().ofLengthBetween(1, 100);
        Gen<List<String>> patternStrings = new ListsDSL().of(stringGen).ofSizeBetween(1, 1);
        QuickTheory.qt().forAll(patternStrings).check((patterns) -> {
            SearchMethod method = AsciiAhoCorasickBuilder.buildAhoCorasick(patterns);
            return method != null;
        });
    }

}
