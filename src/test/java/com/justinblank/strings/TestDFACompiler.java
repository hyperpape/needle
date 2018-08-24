package com.justinblank.strings;

import org.junit.Test;

import java.lang.reflect.Constructor;

import com.justinblank.classloader.MyClassLoader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestDFACompiler {

    @Test
    public void testDFACompiled() throws Exception {
        DFA dfa = new DFA(true);
        dfa.addTransition(new CharRange('0', '9'), dfa);
        dfa.addTransition(new CharRange('A', 'Z'), dfa);
        dfa.addTransition(new CharRange('a', 'z'), dfa);

        DFACompiler.compile(dfa, "TestName");
        Class<?> matcherClass = MyClassLoader.getInstance().loadClass("TestName");
        Constructor<Matcher> constructor = (Constructor<Matcher>) matcherClass.getDeclaredConstructors()[0];
        Matcher instance = constructor.newInstance("AB09"); // ABC09
        assertTrue(instance.matches());
        
        assertTrue(constructor.newInstance("ABC09az").matches());
        assertFalse(constructor.newInstance("AB{").matches());
    }
}
