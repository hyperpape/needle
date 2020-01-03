package com.justinblank.strings;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestDFACompiler {

    @Test
    public void testSingleCharLiteralRegex() throws Exception {
        Pattern pattern = DFACompiler.compile("a", "SingleCharRegex");
        Matcher instance = pattern.matcher("a");
        assertTrue(instance.matches());

        assertFalse(pattern.matcher("b").matches());
        assertFalse(pattern.matcher("ab").matches());
        assertFalse(pattern.matcher("ba").matches());
        assertFalse(pattern.matcher("AB{").matches());
    }

    @Test
    public void testMultiCharLiteralRegex() {
        Pattern pattern = DFACompiler.compile("abc", "MultiCharLiteralRegex");
        Matcher instance = pattern.matcher("abc");
        assertTrue(instance.matches());

        assertFalse(pattern.matcher("d").matches());
        assertFalse(pattern.matcher("abcd").matches());
        assertFalse(pattern.matcher("dabc").matches());
        assertFalse(pattern.matcher("AB{").matches());
    }

    @Test
    public void testDFACompiledSimpleRegex() throws Exception {
        Pattern pattern = DFACompiler.compile("[0-9A-Za-z]*", "TestName");
        Matcher instance = pattern.matcher("AB09");
        assertTrue(instance.matches());
        
        assertTrue(pattern.matcher("ABC09az").matches());
        assertFalse(pattern.matcher("AB{").matches());
    }

    @Test
    public void testGroupedDFAAlternation() throws Exception {
        Pattern pattern = DFACompiler.compile("(AB)|(BA)", "testGroupedDFAAlternation");
        Matcher matcher = pattern.matcher("AB");
        assertTrue(matcher.matches());
        assertTrue(pattern.matcher("BA").matches());
        assertFalse(pattern.matcher("ABBA").matches());
    }

    @Test
    public void testTwoAcceptingStateDFA() throws Exception {
        Pattern pattern = DFACompiler.compile("(A+)|(B+)", "testTwoAcceptingStateDFA");
        Matcher matcher = pattern.matcher("A");
        assertTrue(matcher.matches());
        assertTrue(pattern.matcher("AA").matches());
        assertTrue(pattern.matcher("BB").matches());
        assertFalse(pattern.matcher("AB").matches());
    }

    @Test
    public void testDFACompiledManyStateRegex() throws Exception {
        String regexString = IntegrationTest.MANY_STATE_REGEX_STRING;
        Pattern pattern = DFACompiler.compile(regexString, "testDFACompiledManyStateRegex");
        Matcher instance = pattern.matcher("456");
        assertTrue(instance.matches());
        assertTrue(pattern.matcher("456456").matches());

        assertFalse(pattern.matcher("").matches());
        assertFalse(pattern.matcher("059{").matches());
    }

    @Test
    public void testRepetitionWithLiteralSuffix() {
        String regexString = "((12)|(23)){1,2}" + "ab";
        Pattern pattern = DFACompiler.compile(regexString, "GroupedRepetitionWithLiteralSuffix");
        assertTrue(pattern.matcher("12ab").matches());
        assertTrue(pattern.matcher("2323ab").matches());
        assertFalse(pattern.matcher("").matches());
    }

    @Test
    public void testManyStateRegexWithLiteralSuffix() {
        String regexString = IntegrationTest.MANY_STATE_REGEX_STRING + "ab";
        Pattern pattern = DFACompiler.compile(regexString, "ManyStateRegexWithLiteralSuffix");
        assertTrue(pattern.matcher("123ab").matches());
        assertTrue(pattern.matcher("234234ab").matches());
        assertFalse(pattern.matcher("").matches());
    }

    @Test
    public void testDFACompiledDigitPlus() throws Exception {
        Pattern pattern = DFACompiler.compile("[0-9]+", "testDFACompiledDigitPlus");
        Matcher instance = pattern.matcher("0");
        assertTrue(instance.matches());

        assertFalse(pattern.matcher("").matches());
        assertFalse(pattern.matcher("059{").matches());
    }

    @Test
    public void testDFACompiledBMP() throws Exception {
        Pattern pattern = DFACompiler.compile("[\u0600-\u06FF]", "testDFACompiledBMP");
        assertTrue(pattern.matcher("\u0600").matches());
        assertFalse(pattern.matcher("AB{").matches());
    }

    @Test
    public void testDFACompiledAlternationOfLiterals() throws Exception {
        Pattern pattern = DFACompiler.compile("A|BCD|E", "alternation1");
        assertTrue(pattern.matcher("A").matches());
        assertTrue(pattern.matcher("BCD").matches());
        assertTrue(pattern.matcher("E").matches());
        assertFalse(pattern.matcher("F").matches());
    }

    @Test(expected =  IllegalArgumentException.class)
    public void testDFACompileFailsLargePattern() {
        String manyStateRegexString = "((123)|(234)|(345)|(456)|(567)|(678)|(789)|(0987)|(9876)|(8765)|(7654)|(6543)|(5432)|(4321)|(3210)){1,160}";
        DFACompiler.compile(manyStateRegexString, "tooBig");
    }

}
