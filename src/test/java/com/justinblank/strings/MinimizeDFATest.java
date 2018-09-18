package com.justinblank.strings;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;

public class MinimizeDFATest {

    @Test
    public void testPartition() {
        DFA dfa = fourStateMinimizableDFA();
        Set<Set<DFA>> partition = MinimizeDFA.createPartition(dfa);
        assertEquals(partition.size(), 3);
    }

    private DFA fourStateMinimizableDFA() {
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

    @Test
    public void testMinimizeMinimizableFourStateDFA() {
        DFA dfa = fourStateMinimizableDFA();
        DFA minimized = MinimizeDFA.minimizeDFA(dfa);
        assertEquals(minimized.statesCount(), 3);
    }

    @Test
    public void testMinimizeMinimalDFA() {
        DFA dfa = new DFA(false);
        DFA second = new DFA(true);
        dfa.addTransition(new CharRange('a', 'a'), second);

        DFA minimized = MinimizeDFA.minimizeDFA(dfa);
        assertEquals(minimized.statesCount(), 2);
    }
}
