package com.justinblank.strings;

import com.justinblank.strings.RegexAST.CharRangeNode;
import com.justinblank.strings.RegexAST.Node;
import com.justinblank.strings.RegexAST.Repetition;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;

public class ASTToNFATest {

    @Test
    public void testRepetition() {
        Node charNode = new CharRangeNode('a', 'a');
        Node node = new Repetition(charNode);
        NFA nfa = ASTToNFA.createNFA(node);
        assertTrue(nfa.matches(""));
        assertTrue(nfa.matches("a"));
        assertTrue(nfa.matches("aaaa"));
    }
}
