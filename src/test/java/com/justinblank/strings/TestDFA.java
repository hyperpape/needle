package com.justinblank.strings;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestDFA {

    @Test
    public void testDFAMatchingInterpreted() {
        DFA dfa = new DFA(true);
        dfa.addTransition(new CharRange('0', '9'), dfa);
        dfa.addTransition(new CharRange('A', 'Z'), dfa);
        dfa.addTransition(new CharRange('a', 'z'), dfa);

        assertTrue(dfa.matches("ABC09az"));
        assertFalse(dfa.matches("09{"));
    }

}
