package com.justinblank.strings;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class TestNFA {

    @Test
    public void testEpsilonClosureShallow() {
        NFA nfa = new NFA(false, 0);
        nfa.setRoot(nfa);
        NFA second = new NFA(false, 1);
        nfa.addEpsilonTransition(second);
        nfa.setStates(List.of(nfa, second));
        nfa.computeEpsilonClosure();
        assertTrue(nfa.epsilonClosure().contains(nfa));
        assertTrue(nfa.epsilonClosure().contains(second));
    }

    @Test
    public void testEpsilonClosureDeep() {
        NFA nfa = new NFA(false, 0);
        nfa.setRoot(nfa);

        NFA second = new NFA(false, 1);
        NFA third = new NFA(false, 2);
        nfa.addEpsilonTransition(second);
        second.addEpsilonTransition(third);
        nfa.setStates(List.of(nfa, second, third));
        nfa.computeEpsilonClosure();
        second.computeEpsilonClosure();
        assertTrue(nfa.epsilonClosure().contains(nfa));
        assertTrue(nfa.epsilonClosure().contains(second));
        assertTrue(nfa.epsilonClosure().contains(third));
    }

}
