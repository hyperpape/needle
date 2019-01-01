package com.justinblank.strings;

import com.justinblank.strings.RegexAST.Node;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class IntegrationTest {

    @Test
    public void testConcatenatedRangesWithRepetition() {
        Node node = RegexParser.parse("[A-Za-z][A-Za-z0-9]*");
        DFA dfa = NFAToDFACompiler.compile(ASTToNFA.createNFA(node));
        assertTrue(dfa.matches("ABC0123"));
    }

    @Test
    public void testRanges() {
        Node node = RegexParser.parse("[A-Za-z]");
        DFA dfa = NFAToDFACompiler.compile(ASTToNFA.createNFA(node));
        assertTrue(dfa.matches("C"));
        assertTrue(dfa.matches("c"));
    }

    @Test
    public void testNFARepetition() {
        Node node = RegexParser.parse("C*");
        NFA nfa = ASTToNFA.createNFA(node);
        assertTrue(nfa.matches(""));
        assertTrue(nfa.matches("C"));
        assertTrue(nfa.matches("CCCCCC"));
    }

    @Test
    public void testDFARepetition() {
        Node node = RegexParser.parse("C*");
        DFA dfa = NFAToDFACompiler.compile(ASTToNFA.createNFA(node));
        assertTrue(dfa.matches(""));
        assertTrue(dfa.matches("C"));
        assertTrue(dfa.matches("CCCCCC"));
    }

    @Test
    public void testDFAAlternation() {
        Node node = RegexParser.parse("A|B");
        DFA dfa = NFAToDFACompiler.compile(ASTToNFA.createNFA(node));
        assertTrue(dfa.matches("A"));
        assertTrue(dfa.matches("B"));
        assertFalse(dfa.matches("AB"));
    }

    @Test
    public void testGroupedDFAAlternation() {
        Node node = RegexParser.parse("(AB)|(BA)");
        DFA dfa = NFAToDFACompiler.compile(ASTToNFA.createNFA(node));
        assertTrue(dfa.matches("AB"));
        assertTrue(dfa.matches("BA"));
        assertFalse(dfa.matches("ABBA"));
    }

    @Test
    public void testManyStateRegex() {
        String regexString = "(123)|(234)|(345)|(456)|(567)";
        Node node = RegexParser.parse(regexString);
        NFA nfa = ASTToNFA.createNFA(node);
        assertTrue(nfa.matches("567"));
        assertFalse(nfa.matches(""));
        DFA dfa = NFAToDFACompiler.compile(nfa);
        assertTrue(dfa.matches("567"));
        assertFalse(dfa.matches(""));
    }
}
