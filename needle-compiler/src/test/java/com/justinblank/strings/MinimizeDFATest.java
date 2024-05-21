package com.justinblank.strings;

import org.junit.Ignore;
import org.junit.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// TODO: fixup tests/impl after benchmark
@Ignore
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
        DFA minimized = MinimizeDFA.minimizeDFA(dfa, false);
        assertEquals(minimized.statesCount(), 3);
    }

    @Test
    public void testMinimizeMinimalDFA() {
        DFA dfa = DFA.root(false);
        DFA second = new DFA(dfa, true, 1);
        dfa.addTransition(new CharRange('a', 'a'), second);

        DFA minimized = MinimizeDFA.minimizeDFA(dfa, false);
        assertEquals(minimized.statesCount(), 2);
    }

    @Test
    public void testMinimizeDFAHandlesSingleCharLiteral() {
        DFA dfa = DFA.createDFA("a");
        assertTrue(MinimizeDFA.minimizeDFA(dfa, false).matches("a"));
    }

    @Test
    public void testMinimizeDFAHandlesMultiCharLiteral() {
        DFA dfa = DFA.createDFA("ab");
        assertTrue(MinimizeDFA.minimizeDFA(dfa, false).matches("ab"));
    }

    @Test
    public void testMinimizeDFAHandlesUnion() {
        DFA dfa = DFA.createDFA("[0-9]");
        assertTrue(MinimizeDFA.minimizeDFA(dfa, false).matches("2"));
    }

    @Test
    public void testMinimizeDFAHandlesRepetition() {
        DFA dfa1 = DFA.createDFA("a*");
        DFA dfa = MinimizeDFA.minimizeDFA(dfa1, false);
        assertEquals(1, dfa.statesCount());
        assertTrue(dfa.matches("a"));
    }

    @Test
    public void testCountedRepetition() {
        var nfa = new NFA(RegexInstrBuilder.createNFA(RegexParser.parse("1{0,2}")));
        DFA original = new NFAToDFACompiler(nfa)._compile(nfa, ConversionMode.BASIC);
        assertTrue(original.matches("1"));
        DFA dfa = MinimizeDFA.minimizeDFA(original, false);
        assertTrue(dfa.matches("1"));
    }

    @Test
    public void testMinimizeRange() {
        var dfa = DFA.createDFA("(a|b)");
        assertEquals(2, dfa.statesCount());

    }
}
