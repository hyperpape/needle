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

    @Test
    public void testCountedRepetitionOneChar() {
        String regexString = "1{1,1}";
        Node node = RegexParser.parse(regexString);
        NFA nfa = ASTToNFA.createNFA(node);
        assertTrue(nfa.matches("1"));
        assertFalse(nfa.matches(""));
        assertFalse(nfa.matches("11"));

    }

    @Test
    public void testZeroBoundCountedRepetition() {
        String regexString = "1{0,2}";
        Node node = RegexParser.parse(regexString);
        NFA nfa = ASTToNFA.createNFA(node);
        assertTrue(nfa.matches(""));
        assertTrue(nfa.matches("1"));
        assertTrue(nfa.matches("11"));
        assertFalse(nfa.matches("111"));
    }

    @Test
    public void testCountedRepetition() {
        String regexString = "1{2,3}abc";
        Node node = RegexParser.parse(regexString);
        NFA nfa = ASTToNFA.createNFA(node);
        assertTrue(nfa.matches("11abc"));
        assertTrue(nfa.matches("111abc"));
        assertFalse(nfa.matches(""));
        assertFalse(nfa.matches("1abc"));
        assertFalse(nfa.matches("1111abc"));
        assertFalse(nfa.matches("11def"));
        DFA dfa = NFAToDFACompiler.compile(nfa);
        assertTrue(dfa.matches("11abc"));
        assertTrue(dfa.matches("111abc"));
        assertFalse(dfa.matches(""));
        assertFalse(dfa.matches("1abc"));
        assertFalse(dfa.matches("1111abc"));
        assertFalse(dfa.matches("11def"));
    }

    @Test
    public void test_ab_PLUS() {
        String regexString = "(ab)+";
        Node node = RegexParser.parse(regexString);
        NFA nfa = ASTToNFA.createNFA(node);
        assertTrue(nfa.matches("ab"));
        assertTrue(nfa.matches("ababab"));
        assertFalse(nfa.matches(""));
        DFA dfa = NFAToDFACompiler.compile(nfa);
        assertTrue(dfa.matches("ab"));
        assertTrue(dfa.matches("abab"));
        assertFalse(dfa.matches(""));
    }

    @Test
    public void test_a_PLUS() {
        String regexString = "a+";
        Node node = RegexParser.parse(regexString);
        NFA nfa = ASTToNFA.createNFA(node);
        assertTrue(nfa.matches("a"));
        assertTrue(nfa.matches("aaaaaaa"));
        assertFalse(nfa.matches(""));
        DFA dfa = NFAToDFACompiler.compile(nfa);
        assertTrue(dfa.matches("a"));
        assertTrue(dfa.matches("aaaaaaa"));
        assertFalse(dfa.matches(""));
    }

    @Test
    public void test_aORb_PLUS() {
        String regexString = "[a-c]+";
        Node node = RegexParser.parse(regexString);
        NFA nfa = ASTToNFA.createNFA(node);
        assertTrue(nfa.matches("a"));
        assertTrue(nfa.matches("c"));
        assertTrue(nfa.matches("abacbabababbbabababa"));
        assertFalse(nfa.matches(""));
    }

    @Test
    public void testConcatenatedMultiRegex() {
        String regexString = "[a-zA-Z@#]";
        Node node = RegexParser.parse(regexString);
        NFA nfa = ASTToNFA.createNFA(node);
        assertTrue(nfa.matches("a"));
        assertTrue(nfa.matches("@"));
        assertTrue(nfa.matches("#"));
        assertFalse(nfa.matches(""));
        assertFalse(nfa.matches("ab"));
        assertFalse(nfa.matches("a#"));
    }
}
