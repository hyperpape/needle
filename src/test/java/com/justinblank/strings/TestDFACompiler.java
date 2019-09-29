package com.justinblank.strings;

import com.justinblank.classloader.MyClassLoader;
import org.junit.Test;

import java.lang.reflect.Constructor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestDFACompiler {

    @Test
    public void testSingleCharLiteralRegex() throws Exception {
        DFA dfa = DFA.createDFA("a");

        Pattern pattern = DFACompiler.compile(dfa, "SingleCharRegex");
        Matcher instance = pattern.matcher("a");
        assertTrue(instance.matches());

        assertFalse(pattern.matcher("b").matches());
        assertFalse(pattern.matcher("ab").matches());
        assertFalse(pattern.matcher("ba").matches());
        assertFalse(pattern.matcher("AB{").matches());
    }

    @Test
    public void testMultiCharLiteralRegex() {
        DFA dfa = DFA.createDFA("abc");
        Pattern pattern = DFACompiler.compile(dfa, "MultiCharLiteralRegex");
        Matcher instance = pattern.matcher("abc");
        assertTrue(instance.matches());

        assertFalse(pattern.matcher("d").matches());
        assertFalse(pattern.matcher("abcd").matches());
        assertFalse(pattern.matcher("dabc").matches());
        assertFalse(pattern.matcher("AB{").matches());
    }


    @Test
    public void testDFACompiledSimpleRegex() throws Exception {
        DFA dfa = DFA.createDFA("[0-9A-Za-z]*");

        Pattern pattern = DFACompiler.compile(dfa, "TestName");
        Matcher instance = pattern.matcher("AB09");
        assertTrue(instance.matches());
        
        assertTrue(pattern.matcher("ABC09az").matches());
        assertFalse(pattern.matcher("AB{").matches());
    }

    @Test
    public void testGroupedDFAAlternation() throws Exception {
        Pattern pattern = DFACompiler.compileString("(AB)|(BA)", "testGroupedDFAAlternation");
        Matcher matcher = pattern.matcher("AB");
        assertTrue(matcher.matches());
        assertTrue(pattern.matcher("BA").matches());
        assertFalse(pattern.matcher("ABBA").matches());
    }

    @Test
    public void testDFACompiledManyStateRegex() throws Exception {
        String regexString = IntegrationTest.MANY_STATE_REGEX_STRING;
        Pattern pattern = DFACompiler.compileString(regexString, "testDFACompiledManyStateRegex");
        Matcher instance = pattern.matcher("456");
        assertTrue(instance.matches());
        assertTrue(pattern.matcher("456456").matches());

        assertFalse(pattern.matcher("").matches());
        assertFalse(pattern.matcher("059{").matches());
    }

    @Test
    public void testDFACompiledDigitPlus() throws Exception {
        DFA dfa = DFA.createDFA("[0-9]+");

        Pattern pattern = DFACompiler.compile(dfa, "testDFACompiledDigitPlus");
        Matcher instance = pattern.matcher("0");
        assertTrue(instance.matches());

        assertFalse(pattern.matcher("").matches());
        assertFalse(pattern.matcher("059{").matches());
    }

    @Test
    public void testDFACompiledBMP() throws Exception {
        DFA dfa = DFA.createDFA("[\u0600-\u06FF]");
        Pattern pattern = DFACompiler.compile(dfa, "testDFACompiledBMP");
        assertTrue(pattern.matcher("\u0600").matches());
        assertFalse(pattern.matcher("AB{").matches());
    }
}
