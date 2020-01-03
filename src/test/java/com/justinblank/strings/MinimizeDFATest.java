package com.justinblank.strings;

import org.junit.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MinimizeDFATest {

    @Test
    public void testPartition() {
        DFA dfa = fourStateMinimizableDFA();
        Map<DFA, Set<DFA>> partition = new MinimizeDFA().createPartition(dfa);
        assertEquals(new HashSet<>(partition.values()).size(), 3);
    }

    private DFA fourStateMinimizableDFA() {
        DFA dfa = DFA.root(false);
        DFA second1 = new DFA(dfa, false, 1);
        DFA second2 = new DFA(dfa, false, 2);
        DFA accepting = new DFA(dfa, true, 3);
        dfa.addTransition(new CharRange('a', 'a'), second1);
        dfa.addTransition(new CharRange('b', 'b'), second2);

        second1.addTransition(new CharRange('c', 'c'), accepting);
        second2.addTransition(new CharRange('c', 'c'), accepting);
        dfa.checkRep();
        return dfa;
    }

    @Test
    public void testMinimizeMinimizableFourStateDFA() {
        DFA dfa = fourStateMinimizableDFA();
        DFA minimized = MinimizeDFA.minimizeDFA(dfa);
        assertEquals(minimized.statesCount(), 3);
    }

    @Test
    public void testMinimizeMinimalDFA() {
        DFA dfa = DFA.root(false);
        DFA second = new DFA(dfa, true, 1);
        dfa.addTransition(new CharRange('a', 'a'), second);

        DFA minimized = MinimizeDFA.minimizeDFA(dfa);
        assertEquals(minimized.statesCount(), 2);
    }

    @Test
    public void testMinimizeDFAHandlesSingleCharLiteral() {
        assertTrue(MinimizeDFA.minimizeDFA(DFA.createDFA("a")).matches("a"));
    }

    @Test
    public void testMinimizeDFAHandlesMultiCharLiteral() {
        assertTrue(MinimizeDFA.minimizeDFA(DFA.createDFA("ab")).matches("ab"));
    }

    @Test
    public void testMinimizeDFAHandlesAlternation() {
        assertTrue(MinimizeDFA.minimizeDFA(DFA.createDFA("[0-9]")).matches("2"));
    }

    @Test
    public void testMinimizeDFAHandlesRepetition() {
        DFA dfa = MinimizeDFA.minimizeDFA(DFA.createDFA("a*"));
        assertTrue(dfa.matches("a"));
    }

    @Test
    public void testCountedRepetition() {
        DFA original = DFA.createDFA("1{0,2}");
        assertTrue(original.matches("1"));
        DFA dfa = MinimizeDFA.minimizeDFA(original);
        assertTrue(dfa.matches("1"));
    }
}
