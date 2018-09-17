package com.justinblank.strings;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
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

    @Test
    public void testAllStates() {
        DFA dfa = sampleDFA();
        assertEquals(dfa.allStates().size(), 4);
    }

    @Test
    public void testAcceptingStates() {
        DFA dfa = sampleDFA();
        assertEquals(dfa.acceptingStates().size(), 1);
    }

    private DFA sampleDFA() {
        DFA dfa = new DFA(false);
        DFA second1 = new DFA(false);
        DFA second2 = new DFA(false);
        DFA accepting = new DFA(true);
        dfa.addTransition(new CharRange('a', 'a'), second1);
        dfa.addTransition(new CharRange('b', 'b'), second2);

        second1.addTransition(new CharRange('c', 'c'), accepting);
        second2.addTransition(new CharRange('c', 'c'), accepting);
        return dfa;
    }

}
