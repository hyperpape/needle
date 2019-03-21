package com.justinblank.strings;

import com.justinblank.strings.RegexAST.*;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class ThompsonNFABuilderTest {

    @Test
    public void testRepetition() {
        Node charNode = new CharRangeNode('a', 'a');
        Node node = new Repetition(charNode);
        NFA nfa = ThompsonNFABuilder.createNFA(node);
        assertTrue(nfa.matches(""));
        assertTrue(nfa.matches("a"));
        assertTrue(nfa.matches("aaaa"));
    }
    
    @Test
    public void testabPlus() {
        Node charNode = new CharRangeNode('a', 'b');
        Node node = new Concatenation(charNode, new Repetition(charNode));
        NFA nfa = ThompsonNFABuilder.createNFA(node);
        assertTrue(nfa.matches("a"));
        assertTrue(nfa.matches("b"));
        assertTrue(nfa.matches("abba"));
        assertFalse(nfa.matches(""));
    }

    @Test
    public void testAlternation() {
        Node charNodea = new CharRangeNode('a', 'a');
        Node charNodeb = new CharRangeNode('b', 'b');
        Node alternation = new Alternation(charNodea, charNodeb);
        NFA nfa = ThompsonNFABuilder.createNFA(alternation);
        assertTrue(nfa.matches("a"));
        assertTrue(nfa.matches("b"));
        assertFalse(nfa.matches(""));
    }

//    @Test
//    public void testSomething() {
//        Node charNodea = new CharRangeNode('a', 'a');
//        Node charNodeb = new CharRangeNode('b', 'b');
//        Node charNodec = new CharRangeNode('c', 'c');
//        Node concatenation = new Concatenation(charNodea, charNodeb);
//        Node alternation = new Alternation(concatenation, charNodec);
//        NFA nfa = ThompsonNFABuilder.createNFA(alternation);
//        assertTrue(nfa.matches("ab"));
//        assertTrue(nfa.matches("c"));
//        assertFalse(nfa.matches(""));
//    }
}
