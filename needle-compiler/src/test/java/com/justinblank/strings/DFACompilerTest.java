package com.justinblank.strings;

import org.junit.Ignore;
import org.junit.Test;
import org.quicktheories.QuickTheory;
import org.quicktheories.core.Gen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.justinblank.strings.SearchMethodTestUtil.*;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

public class DFACompilerTest {

    static final String CORE_LARGE_REGEX_STRING = "((123)|(234)|(345)|(456)|(567)|(678)|(789)|(0987)|(9876)|(8765)|(7654)|(6543)|(5432)|(4321)|(3210)){1,";

    // TODO: This is suboptimal, as it doesn't mix unicode and A-Z.
    static final Gen<String> ALPHABET = A_THROUGH_Z.ofLengthBetween(0, SMALL_DATA_SIZE)
            .mix(SMALL_BMP.ofLengthBetween(0, SMALL_DATA_SIZE));

    // There are lots of painful little fencepost type errors possible as we start to experiment with inlining and
    // handling prefixes, so we'll explicitly test sizes 1-4
    @Test
    public void testSingleCharLiteralRegex() {
        Pattern pattern = DFACompiler.compile("a", "SingleCharRegex");
        match(pattern, "a");
        fail(pattern, "b");

        assertFalse(pattern.matcher("ab").matches());
        find(pattern, "ab");
        find(pattern, "aba");

        assertFalse(pattern.matcher("ba").matches());
        find(pattern, "ba");
        find(pattern, "bad");

        fail(pattern, "AB{");

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "a", prefix, suffix);
            return true;
        });
    }

    @Test
    public void testMultipleFindSingleCharLiteralRegex() {
        Pattern pattern = DFACompiler.compile("a", "SingleCharRegexMultipleFind");
        var matcher = pattern.matcher("aba");
        find(pattern, "ab");
        assertEquals(MatchResult.success(0, 1), pattern.matcher("ab").find());
        find(pattern, "aba");
        assertEquals(MatchResult.success(0, 1), matcher.find());
        assertEquals(MatchResult.success(2, 3), matcher.find());
        assertFalse(matcher.find().matched);
    }


    @Test
    public void testTwoCharLiteralRegex() {
        Pattern pattern = DFACompiler.compile("xy", "TwoCharLiteralRegex");
        match(pattern, "xy");

        fail(pattern, "z");
        assertFalse(pattern.matcher("xyz").matches());
        assertTrue(pattern.matcher("xyz").containedIn());
        assertFalse(pattern.matcher("zxy").matches());
        assertTrue(pattern.matcher("zxy").containedIn());
        assertFalse(pattern.matcher("xzxy").matches());
        assertTrue(pattern.matcher("xzxy").containedIn());
        fail(pattern, "XY{");

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "xy", prefix, suffix);
            return true;
        });
    }

    @Test
    public void testThreeCharLiteralRegex() {
        Pattern pattern = DFACompiler.compile("abc", "ThreeCharLiteralRegex");
        match(pattern, "abc");

        new ArrayList<>().addAll(new ArrayList<>());
        fail(pattern, "d");
        assertFalse(pattern.matcher("abcd").matches());
        assertTrue(pattern.matcher("abcd").containedIn());
        assertFalse(pattern.matcher("dabc").matches());
        assertTrue(pattern.matcher("dabc").containedIn());
        assertTrue(pattern.matcher("abdabc").containedIn());
        fail(pattern, "AB{");
        fail(pattern, "abd");

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "abc", prefix, suffix);
            return true;
        });
    }

    @Test
    public void testFourCharLiteralRegex() {
        Pattern pattern = DFACompiler.compile("abcd", "FourCharLiteralRegex");
        match(pattern, "abcd");

        fail(pattern, "e");
        assertFalse(pattern.matcher("abcde").matches());
        assertTrue(pattern.matcher("abcde").containedIn());
        assertFalse(pattern.matcher("eabcd").matches());
        assertTrue(pattern.matcher("eabcd").containedIn());
        fail(pattern, "ABC{");
        fail(pattern, "abc");
        fail(pattern, "abce");

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "abcd", prefix, suffix);
            return true;
        });
    }

    @Test
    public void testNineCharLiteralRegex() {
        Pattern pattern = DFACompiler.compile("abcdefghi", "NineCharLiteralRegex");
        match(pattern, "abcdefghi");

        fail(pattern, "abcd");
        fail(pattern, "abcdefgh");

        // Match initial state, pass by offset, but then hit zero state
        find(pattern, "a0cdefghi" + "abcdefghi");

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "abcdefghi", prefix, suffix);
            return true;
        });
    }

    @Test
    public void testLiteralRepetitionRegex() {
        Pattern pattern = DFACompiler.compile("a*", "aStarRegex");
        match(pattern, "");
        match(pattern, "a");
        match(pattern, "aa");
        match(pattern, "aaa");
        match(pattern, "aaaa");

        assertTrue(pattern.matcher("ab").containedIn());
        assertTrue(pattern.matcher("e").containedIn());

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "a", prefix, suffix);
            find(pattern, "aa", prefix, suffix);
            find(pattern, "aaa", prefix, suffix);
            find(pattern, "aaaa", prefix, suffix);
            return true;
        });
    }

    @Test
    public void testLiteralRepetitionLiteralRegex() {
        Pattern pattern = DFACompiler.compile("ad*g", "literalReptitionLiteralRegex");
        match(pattern, "ag");
        match(pattern, "adg");
        match(pattern, "adddg");

        fail(pattern, "adeg");

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "ag", prefix, suffix);
            find(pattern, "adg", prefix, suffix);
            find(pattern, "addg", prefix, suffix);
            find(pattern, "adddg", prefix, suffix);
            return true;
        });
    }

    @Test
    public void testDFACompiledSimpleRegex() {
        Pattern pattern = DFACompiler.compile("[0-9A-Za-z]*", "TestName");
        match(pattern, "AB09");

        match(pattern, "ABC09az");
        assertFalse(pattern.matcher("AB{").matches());
        assertTrue(pattern.matcher("AB{").containedIn());

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "AB", prefix, suffix);
            find(pattern, "BA", prefix, suffix);
            return true;
        });
    }

    @Test
    public void testGroupedDFAUnion() {
        Pattern pattern = DFACompiler.compile("(AB)|(BA)", "testGroupedDFAUnion");
        match(pattern, "AB");
        match(pattern, "BA");

        assertFalse(pattern.matcher("ABBA").matches());
        assertTrue(pattern.matcher("ABBA").containedIn());

        fail(pattern, "A");
        fail(pattern, "AA");
        fail(pattern, "B");
        fail(pattern, "BB");

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "AB", prefix, suffix);
            find(pattern, "BA", prefix, suffix);
            return true;
        });
    }

    @Test
    public void testTwoAcceptingStateDFA() throws Exception {
        Pattern pattern = DFACompiler.compile("(A+)|(B+)", "testTwoAcceptingStateDFA");
        match(pattern, "A");
        match(pattern, "B");
        match(pattern, "AA");
        match(pattern, "BB");
        fail(pattern, "");
        assertFalse(pattern.matcher("AB").matches());
        assertTrue(pattern.matcher("AB").containedIn());

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "A", prefix, suffix);
            find(pattern, "B", prefix, suffix);
            find(pattern, "AA", prefix, suffix);
            find(pattern, "BB", prefix, suffix);
            return true;
        });
    }

    @Test
    public void testDFACompiledManyStateRegex() throws Exception {
        String regexString = IntegrationTest.MANY_STATE_REGEX_STRING;
        Pattern pattern = DFACompiler.compile(regexString, "testDFACompiledManyStateRegex");
        match(pattern, "456");
        match(pattern, "456456");

        fail(pattern, "");
        fail(pattern, "059{");

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "456", prefix, suffix);
            find(pattern, "456456", prefix, suffix);
            find(pattern, "123", prefix, suffix);
            return true;
        });
    }

    @Test
    public void testCountedRepetitionSingleChar() {
        String regexString = "A{1,2}";
        Pattern pattern = DFACompiler.compile(regexString, "CountedRepetitionSingleCharRegex");
        match(pattern, "A");
        match(pattern, "AA");
        fail(pattern, "");
        fail(pattern, "B");
        fail(pattern, "BB");

        find(pattern, "BAB");

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "A", prefix, suffix);
            find(pattern, "AA", prefix, suffix);
            return true;
        });

    }

    @Test
    public void testCountedRepetitionTwoChar() {
        String regexString = "(AB){1,2}";
        Pattern pattern = DFACompiler.compile(regexString, "CountedRepetitionRegexTwoCharRegex");
        match(pattern, "AB");
        match(pattern, "ABAB");
        fail(pattern, "");
        fail(pattern, "BB");
        fail(pattern, "AA");

        find(pattern, "AAB");
        assertFalse(pattern.matcher("ABABAB").matches());

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "AB", prefix, suffix);
            find(pattern, "ABAB", prefix, suffix);
            return true;
        });
    }

    @Test
    public void testCountedRepetitionOfUnion() {
        String regexString = "((AB)|(BA)){1,2}";
        Pattern pattern = DFACompiler.compile(regexString, "CountedRepetitionOfUnion");
        fail(pattern, "");
        match(pattern, "BA");
        match(pattern, "ABBA");
        match(pattern, "BAAB");
        match(pattern, "BABA");
        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "BA", prefix, suffix);
            find(pattern, "ABBA", prefix, suffix);
            find(pattern, "BAAB", prefix, suffix);
            find(pattern, "BABA", prefix, suffix);
            return true;
        });
    }

    @Test
    public void testCountedRepetitionWithLiteralSuffix() throws Exception {
        String regexString = "((AB)|(CD)){1,2}" + "AB";
        Pattern pattern = DFACompiler.compile(regexString, "GroupedRepetitionWithLiteralSuffix");
        var m = pattern.matcher("ABAB");
        match(pattern, "ABAB");
        match(pattern, "ABCDAB");
        match(pattern, "CDAB");
        match(pattern, "CDCDAB");
        fail(pattern, "");
        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "ABAB", prefix, suffix);
            find(pattern, "ABCDAB", prefix, suffix);
            find(pattern, "CDAB", prefix, suffix);
            find(pattern, "CDCDAB", prefix, suffix);
            return true;
        });
    }

    @Test
    public void testTheSpaceWord() {
        Pattern pattern = DFACompiler.compile("the\\s+\\w+", "theSpaceWord");
        find(pattern, "the a");
        find(pattern, "the art");
        find(pattern, "the   art");
        find(pattern, "the   art ");

        find(pattern, " the a");
        find(pattern, " the art");
        find(pattern, " the   art");
        find(pattern, " the   art ");
        find(pattern, "a the u");

        fail(pattern, "the");
        fail(pattern, "the ");
        fail(pattern, "the    ");
        fail(pattern, "theart");

    }

    @Test
    public void testManyStateRegexWithLiteralSuffix() {
        String regexString = IntegrationTest.MANY_STATE_REGEX_STRING + "ab";
        Pattern pattern = DFACompiler.compile(regexString, "ManyStateRegexWithLiteralSuffix");
        match(pattern, "123ab");
        match(pattern, "234234ab");
        fail(pattern, "");

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "123ab", prefix, suffix);
            find(pattern, "234234ab", prefix, suffix);
            return true;
        });
    }

    @Test
    public void testDFACompiledDigit() throws Exception {
        Pattern pattern = DFACompiler.compile("[0-9]", "testDFACompiledDigit");
        match(pattern, "0");

        fail(pattern, "");
        assertFalse(pattern.matcher("0{").matches());
        assertTrue(pattern.matcher("1{").containedIn());
        assertTrue(pattern.matcher("0{").containedIn());

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "1{", prefix, suffix);
            find(pattern, "0{", prefix, suffix);
            return true;
        });
    }

    @Test
    public void testDFACompiledDigitPlus() throws Exception {
        Pattern pattern = DFACompiler.compile("[0-9]+", "testDFACompiledDigitPlus");
        match(pattern, "0");

        fail(pattern, "");
        assertFalse(pattern.matcher("059{").matches());
        assertTrue(pattern.matcher("12{").containedIn());
        assertTrue(pattern.matcher("059{").containedIn());

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "12{", prefix, suffix);
            find(pattern, "059{", prefix, suffix);
            return true;
        });
    }

    @Test
    public void testDFACompiledBMP() throws Exception {
        Pattern pattern = DFACompiler.compile("[\u0600-\u06FF]", "testDFACompiledBMP");
        match(pattern, "\u0600");
        fail(pattern, "AB{");
    }

    @Test
    public void testDFACompiledUnionOfLiterals() throws Exception {
        Pattern pattern = DFACompiler.compile("A|BCD|E", "union1");
        match(pattern, "A");
        match(pattern, "BCD");
        match(pattern, "E");
        assertFalse(pattern.matcher("F").matches());
        assertFalse(pattern.matcher("F").containedIn());

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "A", prefix, suffix);
            find(pattern, "BCD", prefix, suffix);
            find(pattern, "E", prefix, suffix);
            return true;
        });
    }

    @Test
    public void testRepeatedRangeOverlappingWithSuffix() throws Exception {
        Pattern pattern = DFACompiler.compile("[A-Za-z]+ab", "repeatedRangeOverlappingWithSuffix");
        match(pattern, "Aab");
        match(pattern, "aab");

        match(pattern, "AZDab");
        match(pattern, "AZDab");
        match(pattern, "ZDaab");

        var m = pattern.matcher("AaDab");
        match(pattern, "AaDab");

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern,"Aab", prefix, suffix);
            find(pattern, "aab", prefix, suffix);
            find(pattern, "AZDab", prefix, suffix);
            find(pattern, "AZDab", prefix, suffix);
            find(pattern, "aZDaab", prefix, suffix);
            return true;
        });
    }

    @Test
    public void testRepeatedRangeWithIngSuffix() throws Exception {
        Pattern pattern = DFACompiler.compile("[A-Za-z]+ing", "repeatedRangeWithIngSuffix");
        match(pattern, "bing");
        match(pattern, "Bing");
        match(pattern, "zing");
        match(pattern, "Zing");
    }

    @Test
    public void testTwoLargeRangesPrefixSuffixLiteral() throws Exception {
        Pattern pattern = DFACompiler.compile("[A-Za-z]+abcdef", "TwoLargeRangesPrefixWithSuffixLiteral");
        match(pattern, "Aabcdef");
        match(pattern, "aabcdef");

        match(pattern, "AZDabcdef");
        match(pattern, "AZDabcdef");
        match(pattern, "ZDaabcdef");

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "Aabcdef", prefix, suffix);
            find(pattern, "aabcdef", prefix, suffix);
            find(pattern, "AZDabcdef", prefix, suffix);
            find(pattern, "AZDabcdef", prefix, suffix);
            find(pattern, "aZDaabcdef", prefix, suffix);
            return true;
        });
    }

    @Test
    public void testPrefixLargeRangeWithSuffixLiteral() {
        Pattern pattern = DFACompiler.compile("[A-Z]+abcdef", "PrefixLargeRangeWithSuffixLiteral");
        match(pattern, "Aabcdef");
        match(pattern, "AZDabcdef");

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "Aabcdef", prefix, suffix);
            find(pattern, "AZDabcdef", prefix, suffix);
            return true;
        });
    }

    @Test
    public void testLargeRangePrefixWithInfixLiteralAndLargeRangeSuffix() {
        Pattern pattern = DFACompiler.compile("[A-Z]+abcdef[A-Z]+", "LargeRangePrefixWithInfixLiteralAndLargeRangeSuffix");
        match(pattern, "AabcdefZ");
        match(pattern, "AZDabcdefDZA");

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "AabcdefZ", prefix, suffix);
            find(pattern, "AZDabcdefDZA", prefix, suffix);
            return true;
        });
    }

    @Test
    public void testOverSimplifiedURLMatcher() {
        final Pattern pattern = DFACompiler.compile("http://.+", "OverSimplifiedURLMatcher");
        Matcher matcher = pattern.matcher("http://www.google.com");
        matcher.matches();
        matcher.containedIn();
        MatchResult matchResult = matcher.find();
        assertTrue(matchResult.matched);
        assertEquals(0, matchResult.start);
        assertEquals(21, matchResult.end);
    }

    @Test
    public void testHolmesWithin25CharactersOfWatson() {
        final var pattern = DFACompiler.compile("Holmes.{0,25}Watson|Watson.{0,25}Holmes", "HolmesWithin25CharactersOfWatson");
        Matcher matcher = pattern.matcher("HolmesThenWatson");
        assertTrue(matcher.matches());
    }

    @Test
    public void testSomething() {
        DFACompiler.compile("[A-Za-z]+@[A-Za-z0-9]+.com", "another");
    }

    // Has prefix with offset
    @Test
    public void testTheCrown() {
        var pattern = DFACompiler.compile("the [Cc]rown", "theCrown");
        find(pattern, "the Crown");
    }

//    @Test(expected =  IllegalArgumentException.class)
//    public void testDFACompileFailsLargePattern() {
//        String manyStateRegexString = "((123)|(234)|(345)|(456)|(567)|(678)|(789)|(0987)|(9876)|(8765)|(7654)|(6543)|(5432)|(4321)|(3210)){1,1000}";
//        DFACompiler.compile(manyStateRegexString, "tooBig");
//    }

    @Test
    @Ignore
    // TODO: we've increased class size and DFAs this big can't be compiled any more...find solution
    public void testLargeRegex() {
        String largeRegex = CORE_LARGE_REGEX_STRING + "4}";
        var pattern = DFACompiler.compile(largeRegex, "testLargeRegex4");
        var hayStack = "1232343450987";
        assertTrue(pattern.matcher(hayStack).matches());

        pattern = DFACompiler.compile(CORE_LARGE_REGEX_STRING + "8}", "testLargeRegex8");
        hayStack = hayStack + hayStack; // + hayStack + hayStack;
        assertTrue(pattern.matcher(hayStack).matches());
        assertTrue(pattern.matcher(hayStack).matches());
    }

    @Test
    public void testFindingIngWords() {
        String regexString = "[a-zA-Z]+ing";
        String hayStack = "the most perfect reasoning and observing machine that the world has seen";
        var pattern = DFACompiler.compile(regexString, "ingregex");
        var count = 0;
        var m = pattern.matcher(hayStack);
        while (m.find().matched) {
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    public void testFourCharOffsetWithoutPrefix() {
        var pattern = DFACompiler.compile("[a-q][^u-z]{3}x", "testFourCharOffsetWithoutPrefix");
        find(pattern, "aaaax");
    }


    @Test
    public void fileBasedTests() throws Exception {
        var baseName = "dfaFileBasedTests";
        var counter = new AtomicInteger();
        var patterns = new HashMap<String, Pattern>();
        var testSpecs = new RegexTestSpecParser().readTests();
        var correctMatches = 0;
        var nonMatches = 0;
        var errors = new ArrayList<String>();
        for (var spec : testSpecs) {
            var pattern = patterns.computeIfAbsent(spec.pattern, (p) -> DFACompiler.compile(spec.pattern, baseName + counter.incrementAndGet()));
            if (spec.successful) {
                var result = pattern.matcher(spec.target).find();
                if (!result.matched) {
                    errors.add("Matching spec=" + spec.pattern + " against needle=" + spec.target + " failed: expected start=" + spec.start + ", expected end=" + spec.end);
                }
                else {
                    if (result.start != spec.start || result.end != spec.end) {
                        errors.add("Matching spec=" + spec.pattern + " against needle=" + spec.target + " had incorrect indexes, expected start=" + spec.start + ", expected end=" + spec.end + ", actualStart=" + result.start + ", actualEnd=" + result.end);
                        nonMatches++;
                    } else {
                        correctMatches++;
                    }
                }
                try {
                    find(pattern, spec.target);
                }
                catch (Exception e) {
                    errors.add("Matching spec=" + spec.pattern + " against needle=" + spec.target + " had error in find" + e.toString());
                }
                catch (AssertionError e) {
                    errors.add("Matching spec=" + spec.pattern + " against needle=" + spec.target + " had error in find" + e.toString());
                }
            }
            else {
                nonMatches++;
            }
        }
        if (errors.size() > 0) {
            for (var error : errors) {
                System.out.println(error);
            }
            fail("Errors in file based tests");
        }
        // If these fail, then the test parsing is broken
        assertNotEquals(0, correctMatches);
        assertNotEquals(0, nonMatches);
    }
}
