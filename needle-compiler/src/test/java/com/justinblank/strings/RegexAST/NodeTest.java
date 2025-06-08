package com.justinblank.strings.RegexAST;

import com.justinblank.strings.CharRange;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class NodeTest {

    @Test
    void minMaxLengthLiteral() {
        var node = new LiteralNode("abcdef");
        assertEquals(6, node.minLength());
        assertEquals(Optional.of(6), node.maxLength());
    }

    @Test
    void minMaxLengthEmptyString() {
        var node = new LiteralNode("");
        assertEquals(0, node.minLength());
        assertEquals(Optional.of(0), node.maxLength());
    }

    @Test
    void minMaxLengthCharRange() {
        // [0-9];
        var node = new CharRangeNode('0', '9');
        assertEquals(1, node.minLength());
        assertEquals(Optional.of(1), node.maxLength());
    }

    @Test
    void minMaxLengthaSTARAORb() {
        // a*(a|b);
        var rep = new Repetition(new LiteralNode("a"));
        var union = new Union(new LiteralNode("a"), new LiteralNode("b"));
        var node = new Concatenation(rep, union);

        assertEquals(1, node.minLength());
        assertEquals(Optional.empty(), node.maxLength());
    }

    @Test
    void minMaxLengthCountedRepetition() {
        //(ab){2,3};
        var node = new CountedRepetition(new LiteralNode("ab"), 2, 3);
        assertEquals(4, node.minLength());
        assertEquals(Optional.of(6), node.maxLength());
    }

    @Test
    void minMaxLengthUnionContainingRepetition() {
        // (ab)|(a*);
        var node = new Union(new LiteralNode("ab"), new Repetition(new LiteralNode("a")));
        assertEquals(0, node.minLength());
        assertEquals(Optional.empty(), node.maxLength());
    }

    @Test
    void minMaxLengthUnionFollowedByChar() {
        // (a|b)a
        var node = new Concatenation(new Union(new LiteralNode("a"), new LiteralNode("b")), new LiteralNode("a"));
        assertEquals(2, node.minLength());
        assertEquals(Optional.of(2), node.maxLength());
    }

    @Test
    void minMaxLengthUnionOfDifferentLengths() {
        // (ab)|b
        Node node = new Union(new LiteralNode("ab"), new LiteralNode("b"));
        assertEquals(1, node.minLength());
        assertEquals(Optional.of(2), node.maxLength());
    }

    @Test
    void concatentionHeightIsSumOfNodesPlus1() {
        // (a|b)a
        var node = new Concatenation(new Union(new CharRangeNode('a', 'c'), new LiteralNode("f")), new LiteralNode("g"));
        assertEquals(2, node.height());
    }

    @Test
    void unionHeightIsMaxOfNodesPlus1() {
        // (ab)|b
        Node node = new Union(new LiteralNode("ab"), new LiteralNode("b"));
        assertEquals(1, node.height());
    }


    @Test
    void counted_repetition_height_is1_plus_node_height() {
        //(ab){2,3};
        var node = new CountedRepetition(new LiteralNode("ab"), 2, 3);
        assertEquals(1, node.height());
    }


    @Test
    void repetition_height_is1_plus_node_height() {
        // a*(a|b);
        var rep = new Repetition(new LiteralNode("a"));
        var union = new Union(new LiteralNode("a"), new LiteralNode("b"));
        var node = new Concatenation(rep, union);

        assertEquals(1, node.minLength());
        assertEquals(Optional.empty(), node.maxLength());
    }

    @Test
    void isFixedLength_handlesLiterals() {
        var node = new LiteralNode("Abc");
        assertTrue(node.isFixedLength());
    }

    @Test
    void isFixedLength_handlesUnions() {
        var node = new Union(new LiteralNode("Abc"), new LiteralNode("def"));
        assertTrue(node.isFixedLength());
    }

    @Test
    void isFixedLength_handlesVariableLengthNodes() {
        var node = new CountedRepetition(new CharRangeNode(new CharRange('a', 'z')), 1, 5);
        assertFalse(node.isFixedLength());
    }

    @Test
    void isFixedLength_handlesNodesWithNoMaxLength() {
        var node = new Repetition(new CharRangeNode(new CharRange('a', 'z')));
        assertFalse(node.isFixedLength());
    }
}
