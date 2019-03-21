package com.justinblank.strings;

import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class RegexToNFATest {

    @Test
    public void testConversionSingleChar() {
        NFA nfa = ThompsonNFABuilder.createNFA(RegexParser.parse("a"));
        assertTrue(nfa.matches("a"));
        assertFalse(nfa.matches("b"));
        assertFalse(nfa.matches("ab"));
    }

    @Test
    public void testConversionMultipleChars() {
        NFA nfa = ThompsonNFABuilder.createNFA(RegexParser.parse("ab"));
        assertTrue(nfa.matches("ab"));
        assertFalse(nfa.matches("ba"));
        assertFalse(nfa.matches("aba"));
    }

    @Test
    public void testSingleCharRepetition() {
        NFA nfa = ThompsonNFABuilder.createNFA(RegexParser.parse("a*"));
        assertTrue(nfa.matches("a"));
        assertTrue(nfa.matches(""));
        assertTrue(nfa.matches("aaaa"));
        assertFalse(nfa.matches("ba"));
        assertFalse(nfa.matches("aba"));
    }

    @Test
    public void testCharRanges() {
        NFA nfa = ThompsonNFABuilder.createNFA(RegexParser.parse("[0-9]"));
        assertTrue(nfa.matches("0"));
        assertTrue(nfa.matches("5"));
        assertTrue(nfa.matches("9"));
    }
}
