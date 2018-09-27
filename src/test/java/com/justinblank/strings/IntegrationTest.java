package com.justinblank.strings;

import com.justinblank.strings.RegexAST.Node;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;

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
}
