package com.justinblank.strings;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class NFAToDFACompilerTest {

    @Test
    void compileBasic() {
        var nfa = NFA.createNFANoAhoCorasick("(AB){1,2}");
        var dfa = new NFAToDFACompiler(nfa)._compile(nfa, ConversionMode.BASIC);
        assertEquals(7, dfa.statesCount());
        assertTrue(dfa.matches("AB"));
        assertTrue(dfa.matches("ABAB"));
        assertFalse(dfa.matches("ABABAB"));
    }

    @Test
    void compileContainedIn() {
        var nfa = NFA.createNFANoAhoCorasick("(AB){1,2}");
        var dfa = new NFAToDFACompiler(nfa)._compile(nfa, ConversionMode.CONTAINED_IN);
        assertEquals(3, dfa.statesCount());
        assertTrue(dfa.matches("AB"));
        assertEquals(dfa.after("AAA").map(DFA::getStateNumber), Optional.of(1));
    }

    @Test
    void compileDFASearch() {
        var nfa = NFA.createNFANoAhoCorasick("(AB){1,2}");
        var dfa = new NFAToDFACompiler(nfa)._compile(nfa, ConversionMode.DFA_SEARCH);
        assertEquals(6, dfa.statesCount());
    }
}
