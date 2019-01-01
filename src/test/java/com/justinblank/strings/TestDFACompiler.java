package com.justinblank.strings;

import org.junit.Test;

import java.lang.reflect.Constructor;

import com.justinblank.classloader.MyClassLoader;

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
    public void testNew() throws Exception {
        DFA dfa = new DFA(false);
        DFA accepting = new DFA(true);
        dfa.addTransition(new CharRange('0', '9'), accepting);
        accepting.addTransition(new CharRange('0', '9'), accepting);

        DFACompiler.compile(dfa, "testNew");
        Class<?> matcherClass = MyClassLoader.getInstance().loadClass("testNew");
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
