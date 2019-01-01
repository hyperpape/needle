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

        DFACompiler.compile(dfa, "TestName");
        Class<?> matcherClass = MyClassLoader.getInstance().loadClass("TestName");
        Constructor<Matcher> constructor = (Constructor<Matcher>) matcherClass.getDeclaredConstructors()[0];
        Matcher instance = constructor.newInstance("AB09");
        assertTrue(instance.matches());
        
        assertTrue(constructor.newInstance("ABC09az").matches());
        assertFalse(constructor.newInstance("AB{").matches());
    }

    @Test
    public void testGroupedDFAAlternation() throws Exception {
        DFACompiler.compileString("(AB)|(BA)", "testGroupedDFAAlternation");
        Class<?> matcherClass = MyClassLoader.getInstance().loadClass("testGroupedDFAAlternation");
        Constructor<Matcher> constructor = (Constructor<Matcher>) matcherClass.getDeclaredConstructors()[0];
        Matcher instance = constructor.newInstance("AB");
        assertTrue(instance.matches());
        assertTrue(constructor.newInstance("BA").matches());
        assertFalse(constructor.newInstance("ABBA").matches());
    }

    @Test
    public void testDFACompiledManyStateRegex() throws Exception {
        String regexString = "(123)|(234)|(345)|(456)|(567)|(678)|(789)|(0987)|(9876)|(8765)|(7654)|(6543)|(5432)|(4321)|(3210)";
        DFACompiler.compileString(regexString, "testDFACompiledManyStateRegex");
        Class<?> matcherClass = MyClassLoader.getInstance().loadClass("testDFACompiledManyStateRegex");
        Constructor<Matcher> constructor = (Constructor<Matcher>) matcherClass.getDeclaredConstructors()[0];
        Matcher instance = constructor.newInstance("7654");
        assertTrue(instance.matches());

        assertFalse(constructor.newInstance("").matches());
        assertFalse(constructor.newInstance("059{").matches());
    }

    @Test
    public void testDFACompiledDigitPlus() throws Exception {
        DFA dfa = new DFA(false);
        DFA accepting = new DFA(true);
        dfa.addTransition(new CharRange('0', '9'), accepting);
        accepting.addTransition(new CharRange('0', '9'), accepting);

        DFACompiler.compile(dfa, "testDFACompiledDigitPlus");
        Class<?> matcherClass = MyClassLoader.getInstance().loadClass("testDFACompiledDigitPlus");
        Constructor<Matcher> constructor = (Constructor<Matcher>) matcherClass.getDeclaredConstructors()[0];
        Matcher instance = constructor.newInstance("0");
        assertTrue(instance.matches());

        assertFalse(constructor.newInstance("").matches());
        assertFalse(constructor.newInstance("059{").matches());
    }

    @Test
    public void testDFACompiledBMP() throws Exception {
        DFA dfa = new DFA(true);
        dfa.addTransition(new CharRange('\u0600', '\u06FF'), dfa);
        DFACompiler.compile(dfa, "testDFACompiledBMP");
        Class<?> matcherClass = MyClassLoader.getInstance().loadClass("testDFACompiledBMP");
        Constructor<Matcher> constructor = (Constructor<Matcher>) matcherClass.getDeclaredConstructors()[0];
        assertTrue(constructor.newInstance("\u0600").matches());
        assertFalse(constructor.newInstance("AB{").matches());
    }
}
