package com.justinblank.strings;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static com.justinblank.strings.MatchTestUtil.checkMatch;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.*;

public class TestNFA {

    @Test
    public void testEpsilonClosureShallow() {
        NFA nfa = new NFA(false, 0);
        NFA second = new NFA(false, 1);
        nfa.addEpsilonTransition(second);
        nfa.computeEpsilonClosure();
        assertTrue(nfa.epsilonClosure().contains(nfa));
        assertTrue(nfa.epsilonClosure().contains(second));
    }

    @Test
    public void testEpsilonClosureDeep() {
        NFA nfa = new NFA(false, 0);
        NFA second = new NFA(false, 1);
        NFA third = new NFA(false, 2);
        nfa.addEpsilonTransition(second);
        second.addEpsilonTransition(third);
        nfa.computeEpsilonClosure();
        second.computeEpsilonClosure();
        assertTrue(nfa.epsilonClosure().contains(nfa));
        assertTrue(nfa.epsilonClosure().contains(second));
        assertTrue(nfa.epsilonClosure().contains(third));
    }

    @Test
    public void testEmptyNFAMatchingEmptyString() {
        NFA nfa = new NFA(true, 0);
        nfa.computeEpsilonClosure();
        assertTrue(nfa.matches(""));
        assertFalse(nfa.matches("b"));
        checkMatch(nfa, "b", 0, 0);
    }

    @Test
    public void testEmptyNFANotMatchingEmptyString() {
        NFA nfa = new NFA(false, 0);
        nfa.computeEpsilonClosure();
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
        NFA nfa = new NFA(false, 0);
        NFA step2 = new NFA(false, 1);
        NFA terminal = new NFA(true, 2);

        nfa.addTransitions(new CharRange('a', 'a'), Arrays.asList(nfa, step2));
        step2.addTransitions(new CharRange('b', 'b'), Collections.singletonList(terminal));

        nfa.computeEpsilonClosure();
        step2.computeEpsilonClosure();
        terminal.computeEpsilonClosure();

        assertTrue(nfa.matches("aaaaaaaab"));
        assertFalse(nfa.matches("aaaaaaaabb"));
        assertFalse(nfa.matches("b"));
        assertFalse(nfa.matches("acb"));
    }

    @Test
    public void testaSTAR_aORb_NFA() {
        NFA nfa = NFATestUtil.aSTAR_aORb_();
        assertTrue(nfa.matches("a"));
        MatchResult result = nfa.search("a");
        assertTrue(result.matched);
        assertEquals(0, result.start);
        assertEquals(1, result.end);
        assertTrue(nfa.matches("aa"));
        result = nfa.search("aa");
        assertTrue(result.matched);
        assertEquals(0, result.start);
        assertEquals(2, result.end);
        assertTrue(nfa.matches("b"));
        checkMatch(nfa, "b", 0, 1);
        assertTrue(nfa.matches("ab"));
        checkMatch(nfa, "ab", 0, 2);
        assertTrue(nfa.matches("aab"));
        checkMatch(nfa, "aab", 0, 3);
        assertFalse(nfa.matches("aba"));
        checkMatch(nfa, "aba", 0, 2);
    }

    @Test
    public void testTerminalStates() {
        NFA nfa = NFATestUtil.aSTAR_aORb_();
        Set<NFA> terminals = nfa.terminalStates();
        assertNotEquals(terminals.size(), 0);
    }

    @Test
    public void testCircularTerminalStates() {
        NFA nfa = NFATestUtil._0to9AtoZatoz_STAR();
        Set<NFA> terminals = nfa.terminalStates();
        assertNotEquals(terminals.size(), 0);
    }
}
