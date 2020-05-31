package com.justinblank.strings;

import org.junit.Test;

import static com.justinblank.strings.SearchMethodTestUtil.fail;
import static com.justinblank.strings.SearchMethodTestUtil.match;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DFACompilerTest {

    // There are lots of painful little fencepost type errors possible as we start to experiment with inlining and
    // handling prefixes, so we'll explicitly test sizes 1-4
    @Test
    public void testSingleCharLiteralRegex() {
        Pattern pattern = DFACompiler.compile("a", "SingleCharRegex");
        match(pattern, "a");

        fail(pattern, "b");

        assertFalse(pattern.matcher("ab").matches());
        assertTrue(pattern.matcher("ab").containedIn());

        assertFalse(pattern.matcher("ba").matches());
        assertTrue(pattern.matcher("ba").containedIn());
        assertTrue(pattern.matcher("bad").containedIn());

        fail(pattern, "AB{");
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
        fail(pattern, "XY{");
    }

    @Test
    public void testThreeCharLiteralRegex() {
        Pattern pattern = DFACompiler.compile("abc", "ThreeCharLiteralRegex");
        match(pattern, "abc");

        fail(pattern, "d");
        assertFalse(pattern.matcher("abcd").matches());
        assertTrue(pattern.matcher("abcd").containedIn());
        assertFalse(pattern.matcher("dabc").matches());
        assertTrue(pattern.matcher("dabc").containedIn());
        fail(pattern, "AB{");
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
    }

    @Test
    public void testLiteralRepetitionLiteralRegex() {
        Pattern pattern = DFACompiler.compile("ad*g", "literalReptitionLiteralRegex");
        match(pattern, "ag");
        match(pattern, "adg");
        match(pattern, "adddg");

        fail(pattern, "adeg");
    }

    @Test
    public void testDFACompiledSimpleRegex() {
        Pattern pattern = DFACompiler.compile("[0-9A-Za-z]*", "TestName");
        Matcher instance = pattern.matcher("AB09");
        match(pattern, "AB09");

        match(pattern, "ABC09az");
        assertFalse(pattern.matcher("AB{").matches());
        assertTrue(pattern.matcher("AB{").containedIn());
    }

    @Test
    public void testGroupedDFAAlternation() throws Exception {
        Pattern pattern = DFACompiler.compile("(AB)|(BA)", "testGroupedDFAAlternation");
        match(pattern, "AB");

        match(pattern, "BA");
        assertFalse(pattern.matcher("ABBA").matches());
        assertTrue(pattern.matcher("ABBA").containedIn());
    }

    @Test
    public void testTwoAcceptingStateDFA() throws Exception {
        Pattern pattern = DFACompiler.compile("(A+)|(B+)", "testTwoAcceptingStateDFA");
        match(pattern, "A");
        match(pattern, "AA");
        match(pattern, "BB");
        assertFalse(pattern.matcher("AB").matches());
        assertTrue(pattern.matcher("AB").containedIn());
    }

    @Test
    public void testDFACompiledManyStateRegex() throws Exception {
        String regexString = IntegrationTest.MANY_STATE_REGEX_STRING;
        Pattern pattern = DFACompiler.compile(regexString, "testDFACompiledManyStateRegex");
        match(pattern, "456");
        match(pattern, "456456");

        fail(pattern, "");
        fail(pattern, "059{");
    }

    @Test
    public void testCountedRepetition() {
        String regexString = "(AB){1,2}";
        Pattern pattern = DFACompiler.compile(regexString, "CountedRepetitionRegex");
        match(pattern, "AB");
        match(pattern, "ABAB");
        fail(pattern, "");
        fail(pattern, "BB");
        fail(pattern, "AA");
        assertFalse(pattern.matcher("ABABAB").matches());
    }

    @Test
    public void testCountedRepetitionOfAlternation() {
        String regexString = "((AB)|(BA)){1,2}";
        Pattern pattern = DFACompiler.compile(regexString, "CountedRepetitionOfAlternation");
        fail(pattern, "");
        match(pattern, "BA");
        match(pattern, "ABBA");
        match(pattern, "BAAB");
        match(pattern, "BABA");
    }

    @Test
    public void testCountedRepetitionWithLiteralSuffix() {
        String regexString = "((12)|(23)){1,2}" + "ab";
        Pattern pattern = DFACompiler.compile(regexString, "GroupedRepetitionWithLiteralSuffix");
        match(pattern, "12ab");
        match(pattern, "2323ab");
        fail(pattern, "");
    }

    @Test
    public void testManyStateRegexWithLiteralSuffix() {
        String regexString = IntegrationTest.MANY_STATE_REGEX_STRING + "ab";
        Pattern pattern = DFACompiler.compile(regexString, "ManyStateRegexWithLiteralSuffix");
        match(pattern, "123ab");
        match(pattern, "234234ab");
        fail(pattern, "");
    }

    @Test
    public void testDFACompiledDigitPlus() throws Exception {
        Pattern pattern = DFACompiler.compile("[0-9]+", "testDFACompiledDigitPlus");
        match(pattern, "0");

        fail(pattern, "");
        assertFalse(pattern.matcher("059{").matches());
        assertTrue(pattern.matcher("12{").containedIn());
        assertTrue(pattern.matcher("059{").containedIn());
    }

    @Test
    public void testDFACompiledBMP() throws Exception {
        Pattern pattern = DFACompiler.compile("[\u0600-\u06FF]", "testDFACompiledBMP");
        match(pattern, "\u0600");
        fail(pattern, "AB{");
    }

    @Test
    public void testDFACompiledAlternationOfLiterals() throws Exception {
        Pattern pattern = DFACompiler.compile("A|BCD|E", "alternation1");
        match(pattern, "A");
        match(pattern, "BCD");
        match(pattern, "E");
        assertFalse(pattern.matcher("F").matches());
        assertFalse(pattern.matcher("F").containedIn());
    }

    @Test
    public void testTwoLargeRangesPrefixSuffixLiteral() {
        Pattern pattern = DFACompiler.compile("[A-Za-z]+abcdef", "TwoLargeRangesPrefixWithSuffixLiteral");
        match(pattern, "Aabcdef");
        match(pattern, "aabcdef");

        match(pattern, "AZDabcdef");
        match(pattern, "AZDabcdef");

        match(pattern, "aZDaabcdef");
    }

    @Test
    public void testPrefixLargeRangeWithSuffixLiteral() {
        Pattern pattern = DFACompiler.compile("[A-Z]+abcdef", "PrefixLargeRangeWithSuffixLiteral");
        match(pattern, "Aabcdef");
        match(pattern, "AZDabcdef");
    }


    @Test
    public void testLargeRangePrefixWithInfixLiteralAndLargeRangeSuffix() {
        Pattern pattern = DFACompiler.compile("[A-Z]+abcdef[A-Z]+", "LargeRangePrefixWithInfixLiteralAndLargeRangeSuffix");
        match(pattern, "AabcdefZ");
        match(pattern, "AZDabcdefDZA");
    }


//    @Test(expected =  IllegalArgumentException.class)
//    public void testDFACompileFailsLargePattern() {
//        String manyStateRegexString = "((123)|(234)|(345)|(456)|(567)|(678)|(789)|(0987)|(9876)|(8765)|(7654)|(6543)|(5432)|(4321)|(3210)){1,1000}";
//        DFACompiler.compile(manyStateRegexString, "tooBig");
//    }

}
