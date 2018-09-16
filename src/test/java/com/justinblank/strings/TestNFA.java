package com.justinblank.strings;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class TestNFA {


    @Test
    public void testEpsilonClosureShallow() {
        NFA nfa = new NFA(false);
        NFA second = new NFA(false);
        nfa.addEpsilonTransition(second);
        assertTrue(nfa.epsilonClosure().contains(nfa));
        assertTrue(nfa.epsilonClosure().contains(second));
    }

    @Test
    public void testEpsilonClosureDeep() {
        NFA nfa = new NFA(false);
        NFA second = new NFA(false);
        NFA third = new NFA(false);
        nfa.addEpsilonTransition(second);
        second.addEpsilonTransition(second);
        assertTrue(nfa.epsilonClosure().contains(nfa));
        assertTrue(nfa.epsilonClosure().contains(second));
        assertTrue(nfa.epsilonClosure().contains(third));
    }

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
        NFA nfa = NFATestUtil._0to9AtoZatoz_STAR();

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
