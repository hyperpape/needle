package com.justinblank.strings;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NFAToDFACompilerTest {

    @Test
    public void testEmptyAcceptingNFACompile() {
        NFA nfa = new NFA(true);
        DFA dfa = NFAToDFACompiler.compile(nfa);
        assertTrue(dfa.statesCount() == 1);
        assertTrue(dfa.matches(""));
    }

    @Test
    public void testEmptyRejectingNFACompile() {
        NFA nfa = new NFA(false);
        DFA dfa = NFAToDFACompiler.compile(nfa);
        assertEquals(1, dfa.statesCount());
        assertFalse(dfa.matches(""));
    }

    @Test
    public void testSingleCharNFACompile() {
        NFA nfa = new NFA(false);
        List<NFA> postTransition = Collections.singletonList(new NFA(true));
        nfa.addTransitions(new CharRange('a', 'a'), postTransition);
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
