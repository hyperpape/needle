package com.justinblank.strings;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestNFA {

    @Test
    public void testEmptyNFAMatchingEmptyString() {
        NFA nfa = new NFA(true);
        assertTrue(nfa.matches(""));
        assertFalse(nfa.matches("b"));
    }

    @Test
    public void testEmptyNFANotMatchingEmptyString() {
        NFA nfa = new NFA(false);
        assertFalse(nfa.matches(""));
    }

    @Test
    public void testNFAMatchingInterpreted() {
        NFA nfa = new NFA(true);
        nfa.addTransitions(new CharRange('0', '9'), Collections.singletonList(nfa));
        nfa.addTransitions(new CharRange('A', 'Z'), Collections.singletonList(nfa));
        nfa.addTransitions(new CharRange('a', 'z'), Collections.singletonList(nfa));

        assertTrue(nfa.matches("ABC09az"));
        assertFalse(nfa.matches("09{"));
    }

    @Test
    public void testNFAMatchingInterpreted2() {
        NFA nfa = new NFA(false);
        NFA step2 = new NFA(false);
        NFA terminal = new NFA(true);

        nfa.addTransitions(new CharRange('a', 'a'), Arrays.asList(nfa, step2));
        step2.addTransitions(new CharRange('b', 'b'), Collections.singletonList(terminal));

        assertTrue(nfa.matches("aaaaaaaab"));
        assertFalse(nfa.matches("aaaaaaaabb"));
        assertFalse(nfa.matches("b"));
        assertFalse(nfa.matches("acb"));
    }

    @Test
    public void testaSTAR_aORb_NFA() {
        NFA nfa = NFATestUtil.aSTAR_aORb_();
        assertTrue(nfa.matches("a"));
        assertTrue(nfa.matches("aa"));
        assertTrue(nfa.matches("b"));
        assertTrue(nfa.matches("ab"));
        assertTrue(nfa.matches("aab"));
        assertFalse(nfa.matches("aba"));
    }
}
