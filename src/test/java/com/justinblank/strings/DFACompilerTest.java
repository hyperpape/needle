package com.justinblank.strings;

import org.junit.Test;

import static com.justinblank.strings.SearchMethodTestUtil.fail;
import static com.justinblank.strings.SearchMethodTestUtil.match;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DFACompilerTest {

    @Test
    public void testSingleCharLiteralRegex() {
        Pattern pattern = DFACompiler.compile("a", "SingleCharRegex");
        match(pattern, "a");

        fail(pattern, "b");

        assertFalse(pattern.matcher("ab").matches());
        assertTrue(pattern.matcher("ab").containedIn());

        assertFalse(pattern.matcher("ba").matches());
        assertTrue(pattern.matcher("ba").containedIn());

        fail(pattern, "AB{");
    }

    @Test
    public void testMultiCharLiteralRegex() {
        Pattern pattern = DFACompiler.compile("abc", "MultiCharLiteralRegex");
        match(pattern, "abc");

        fail(pattern, "d");
        assertFalse(pattern.matcher("abcd").matches());
        assertTrue(pattern.matcher("abcd").containedIn());
        assertFalse(pattern.matcher("dabc").matches());
        assertTrue(pattern.matcher("dabc").containedIn());
        fail(pattern, "AB{");
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
    public void testRepetitionWithLiteralSuffix() {
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

    @Test(expected =  IllegalArgumentException.class)
    public void testDFACompileFailsLargePattern() {
        String manyStateRegexString = "((123)|(234)|(345)|(456)|(567)|(678)|(789)|(0987)|(9876)|(8765)|(7654)|(6543)|(5432)|(4321)|(3210)){1,160}";
        DFACompiler.compile(manyStateRegexString, "tooBig");
    }

}
