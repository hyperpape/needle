package com.justinblank.strings.RegexAST;

import com.justinblank.strings.CharRange;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class NodeTest {

    @Test
    public void testMinMaxLengthLiteral() {
        var node = new LiteralNode("abcdef");
        assertEquals(6, node.minLength());
        assertEquals(Optional.of(6), node.maxLength());
    }

    @Test
    public void testMinMaxLengthEmptyString() {
        var node = new LiteralNode("");
        assertEquals(0, node.minLength());
        assertEquals(Optional.of(0), node.maxLength());
    }

    @Test
    public void testMinMaxLengthCharRange() {
        // [0-9];
        var node = new CharRangeNode('0', '9');
        assertEquals(1, node.minLength());
        assertEquals(Optional.of(1), node.maxLength());
    }

    @Test
    public void testMinMaxLengthaSTAR_aORb_() {
        // a*(a|b);
        var rep = new Repetition(new LiteralNode("a"));
        var union = new Union(new LiteralNode("a"), new LiteralNode("b"));
        var node = new Concatenation(rep, union);

        assertEquals(1, node.minLength());
        assertEquals(Optional.empty(), node.maxLength());
    }

    @Test
    public void testMinMaxLengthCountedRepetition_() {
        //(ab){2,3};
        var node = new CountedRepetition(new LiteralNode("ab"), 2, 3);
        assertEquals(4, node.minLength());
        assertEquals(Optional.of(6), node.maxLength());
    }

    @Test
    public void testMinMaxLengthUnionContainingRepetition() {
        // (ab)|(a*);
        var node = new Union(new LiteralNode("ab"), new Repetition(new LiteralNode("a")));
        assertEquals(0, node.minLength());
        assertEquals(Optional.empty(), node.maxLength());
    }

    @Test
    public void testMinMaxLengthUnionFollowedByChar() {
        // (a|b)a
        var node = new Concatenation(new Union(new LiteralNode("a"), new LiteralNode("b")), new LiteralNode("a"));
        assertEquals(2, node.minLength());
        assertEquals(Optional.of(2), node.maxLength());
    }

    @Test
    public void testMinMaxLengthUnionOfDifferentLengths() {
        // (ab)|b
        Node node = new Union(new LiteralNode("ab"), new LiteralNode("b"));
        assertEquals(1, node.minLength());
        assertEquals(Optional.of(2), node.maxLength());
    }

    @Test
    public void testConcatentionHeight_isSumOfNodesPlus1() {
        // (a|b)a
        var node = new Concatenation(new Union(new CharRangeNode('a', 'c'), new LiteralNode("f")), new LiteralNode("g"));
        assertEquals(2, node.height());
    }

    @Test
    public void testUnionHeight_isMaxOfNodesPlus1() {
        // (ab)|b
        Node node = new Union(new LiteralNode("ab"), new LiteralNode("b"));
        assertEquals(1, node.height());
    }


    @Test
    public void test_countedRepetitionHeightIs1PlusNodeHeight() {
        //(ab){2,3};
        var node = new CountedRepetition(new LiteralNode("ab"), 2, 3);
        assertEquals(1, node.height());
    }


    @Test
    public void test_repetitionHeightIs1PlusNodeHeight() {
        // a*(a|b);
        var rep = new Repetition(new LiteralNode("a"));
        var union = new Union(new LiteralNode("a"), new LiteralNode("b"));
        var node = new Concatenation(rep, union);

        assertEquals(1, node.minLength());
        assertEquals(Optional.empty(), node.maxLength());
    }

    @Test
    public void isFixedLength_handlesLiterals() {
        var node = new LiteralNode("Abc");
        assertTrue(node.isFixedLength());
    }

    @Test
    public void isFixedLength_handlesUnions() {
        var node = new Union(new LiteralNode("Abc"), new LiteralNode("def"));
        assertTrue(node.isFixedLength());
    }

    @Test
    public void isFixedLength_handlesVariableLengthNodes() {
        var node = new CountedRepetition(new CharRangeNode(new CharRange('a', 'z')), 1, 5);
        assertFalse(node.isFixedLength());
    }

    @Test
    public void isFixedLength_handlesNodesWithNoMaxLength() {
        var node = new Repetition(new CharRangeNode(new CharRange('a', 'z')));
        assertFalse(node.isFixedLength());
    }
}
