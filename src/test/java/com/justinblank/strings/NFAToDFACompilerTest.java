package com.justinblank.strings;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NFAToDFACompilerTest {

    @Test
    public void testEmptyAcceptingNFACompile() {
        NFA nfa = new NFA(true, 0);
        nfa.setRoot(nfa);
        nfa.setStates(Arrays.asList(nfa));
        nfa.computeEpsilonClosure();
        DFA dfa = NFAToDFACompiler.compile(nfa);
        assertTrue(dfa.statesCount() == 1);
        assertTrue(dfa.matches(""));
    }

    @Test
    public void testEmptyRejectingNFACompile() {
        NFA nfa = new NFA(false, 0);
        nfa.setRoot(nfa);
        nfa.setStates(List.of(nfa));
        nfa.computeEpsilonClosure();
        DFA dfa = NFAToDFACompiler.compile(nfa);
        assertEquals(1, dfa.statesCount());
        assertFalse(dfa.matches(""));
    }

    @Test
    public void testSingleCharNFACompile() {
        NFA nfa = new NFA(false, 0);
        nfa.setRoot(nfa);
        List<NFA> postTransition = Collections.singletonList(new NFA(true, 1));
        nfa.setStates(Arrays.asList(nfa, postTransition.get(0)));
        nfa.addTransitions(new CharRange('a', 'a'), postTransition);

        postTransition.get(0).computeEpsilonClosure();
        nfa.computeEpsilonClosure();
        DFA dfa = NFAToDFACompiler.compile(nfa);
        assertEquals(2, dfa.statesCount());
        assertTrue(dfa.matches("a"));
        assertFalse(dfa.matches("ab"));
    }

    @Test
    public void testRepeatingCharNFACompile() {
        // nfa corresponding to a*(ab)
        NFA nfa = NFATestUtil.aSTAR_aORb_();
        DFA dfa = NFAToDFACompiler.compile(nfa);
        assertTrue(dfa.matches("a"));
        assertTrue(dfa.matches("ab"));
        assertTrue(dfa.matches("aaaab"));
    }
}
