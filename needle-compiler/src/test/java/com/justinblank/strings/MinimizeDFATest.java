package com.justinblank.strings;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// TODO: fixup tests/impl after benchmark
@Disabled
class MinimizeDFATest {

    @Test
    void partition() {
        DFA dfa = fourStateMinimizableDFA();
        Map<DFA, Set<DFA>> partition = new MinimizeDFA().createPartition(dfa);
        assertEquals(3, new HashSet<>(partition.values()).size());
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
    void minimizeMinimizableFourStateDFA() {
        DFA dfa = fourStateMinimizableDFA();
        DFA minimized = MinimizeDFA.minimizeDFA(dfa, false);
        assertEquals(3, minimized.statesCount());
    }

    @Test
    void minimizeMinimalDFA() {
        DFA dfa = DFA.root(false);
        DFA second = new DFA(dfa, true, 1);
        dfa.addTransition(new CharRange('a', 'a'), second);

        DFA minimized = MinimizeDFA.minimizeDFA(dfa, false);
        assertEquals(2, minimized.statesCount());
    }

    @Test
    void minimizeDFAHandlesSingleCharLiteral() {
        DFA dfa = DFA.createDFA("a");
        assertTrue(MinimizeDFA.minimizeDFA(dfa, false).matches("a"));
    }

    @Test
    void minimizeDFAHandlesMultiCharLiteral() {
        DFA dfa = DFA.createDFA("ab");
        assertTrue(MinimizeDFA.minimizeDFA(dfa, false).matches("ab"));
    }

    @Test
    void minimizeDFAHandlesUnion() {
        DFA dfa = DFA.createDFA("[0-9]");
        assertTrue(MinimizeDFA.minimizeDFA(dfa, false).matches("2"));
    }

    @Test
    void minimizeDFAHandlesRepetition() {
        DFA dfa1 = DFA.createDFA("a*");
        DFA dfa = MinimizeDFA.minimizeDFA(dfa1, false);
        assertEquals(1, dfa.statesCount());
        assertTrue(dfa.matches("a"));
    }

    @Test
    void countedRepetition() {
        var nfa = new NFA(RegexInstrBuilder.createNFA(RegexParser.parse("1{0,2}")));
        DFA original = new NFAToDFACompiler(nfa)._compile(ConversionMode.BASIC);
        assertTrue(original.matches("1"));
        DFA dfa = MinimizeDFA.minimizeDFA(original, false);
        assertTrue(dfa.matches("1"));
    }

    @Test
    void minimizeRange() {
        var dfa = DFA.createDFA("(a|b)");
        assertEquals(2, dfa.statesCount());

    }

    @ParameterizedTest
    @MethodSource("provideTestSpecs")
    void minimizationTests(MinimizationTestSpec spec) throws Exception {
        com.justinblank.strings.RegexAST.Node node = RegexParser.parse(spec.pattern);
        NFA nfa = new NFA(RegexInstrBuilder.createNFA(node));

        NFAToDFACompiler compiler = new NFAToDFACompiler(nfa);
        DFA preMinimized = compiler._compile(ConversionMode.BASIC);
        preMinimized.pruneDeadStates();

        // Get minimized state count
        DFA minimized = MinimizeDFA.minimizeDFA(preMinimized, false);

        assertEquals(spec.stateCount, preMinimized.statesCount(),
                "State count mismatch for pattern: " + spec.pattern);
        assertEquals(spec.withMinimization, minimized.statesCount(),
                "Minimized state count mismatch for pattern: " + spec.pattern);
    }

    static List<MinimizationTestSpec> provideTestSpecs() throws Exception {
        return new MinimizationTestSpecParser().readTests();
    }
}
