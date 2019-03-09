package com.justinblank.strings;

import com.justinblank.classloader.MyClassLoader;
import org.junit.Test;

import java.lang.reflect.Constructor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestDFACompiler {

    @Test
    public void testDFACompiledSimpleRegex() throws Exception {
        DFA dfa = new DFA(true);
        dfa.addTransition(new CharRange('0', '9'), dfa);
        dfa.addTransition(new CharRange('A', 'Z'), dfa);
        dfa.addTransition(new CharRange('a', 'z'), dfa);

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
        String regexString = "(123)|(234)|(345)|(456)|(567)|(678)|(789)|(0987)|(9876)|(8765)|(7654)|(6543)|(5432)|(4321)|(3210)";
        Pattern pattern = DFACompiler.compileString(regexString, "testDFACompiledManyStateRegex");
        Matcher instance = pattern.matcher("7654");
        assertTrue(instance.matches());

        assertFalse(pattern.matcher("").matches());
        assertFalse(pattern.matcher("059{").matches());
    }

    @Test
    public void testDFACompiledDigitPlus() throws Exception {
        DFA dfa = new DFA(false);
        DFA accepting = new DFA(true);
        dfa.addTransition(new CharRange('0', '9'), accepting);
        accepting.addTransition(new CharRange('0', '9'), accepting);

        Pattern pattern = DFACompiler.compile(dfa, "testDFACompiledDigitPlus");
        Matcher instance = pattern.matcher("0");
        assertTrue(instance.matches());

        assertFalse(pattern.matcher("").matches());
        assertFalse(pattern.matcher("059{").matches());
    }

    @Test
    public void testDFACompiledBMP() throws Exception {
        DFA dfa = new DFA(true);
        dfa.addTransition(new CharRange('\u0600', '\u06FF'), dfa);
        Pattern pattern = DFACompiler.compile(dfa, "testDFACompiledBMP");
        assertTrue(pattern.matcher("\u0600").matches());
        assertFalse(pattern.matcher("AB{").matches());
    }
}
