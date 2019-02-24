package com.justinblank.strings;

import com.justinblank.strings.RegexAST.*;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RegexParserTest {

    @Test
    public void testSingleChar() {
        Node node = RegexParser.parse("a");
        assertNotNull(node);
        assertTrue(node instanceof CharRangeNode);
    }

    @Test
    public void testTwoCharConcatenation() {
        Node node = RegexParser.parse("ab");
        assertNotNull(node);
        assertTrue(node instanceof Concatenation);
    }

    @Test
    public void testMultipleCharConcatenation() {
        Node node = RegexParser.parse("abcdef");
        assertNotNull(node);
        assertTrue(node instanceof Concatenation);
    }

    @Test
    public void testSingleCharRepetition() {
        Node node = RegexParser.parse("b*");
        assertNotNull(node);
        assertTrue(node instanceof Repetition);
    }

    @Test
    public void testCurlyRepetition() {
        Node node = RegexParser.parse("b{0,1}");
        assertNotNull(node);
        assertTrue(node instanceof CountedRepetition);
        CountedRepetition cr = (CountedRepetition) node;
        assertEquals(0, cr.min);
        assertEquals(1, cr.max);
    }

    @Test
    public void testQuestionMarkSimple() {
        Node node = RegexParser.parse("1?");
        assertNotNull(node);
        assertTrue(node instanceof CountedRepetition);
        CountedRepetition cr = (CountedRepetition) node;
        assertEquals(0, cr.min);
        assertEquals(1, cr.max);
    }

    @Test
    public void testQuestionMarkGrouped() {
        Node node = RegexParser.parse("(1|2)?");
        assertNotNull(node);
        assertTrue(node instanceof CountedRepetition);
        CountedRepetition cr = (CountedRepetition) node;
        assertEquals(0, cr.min);
        assertEquals(1, cr.max);
    }

    @Test(expected = IllegalStateException.class)
    public void testMalformedQuestionMarkEmpty() {
        Node node = RegexParser.parse("?");
    }

    @Test
    public void testTwoCharAlternation() {
        Node node = RegexParser.parse("a|b");
        assertNotNull(node);
        assertTrue(node instanceof Alternation);
    }

    @Test
    public void testGroupedRepetition() {
        Node node = RegexParser.parse("(ab)*");
        assertNotNull(node);
        assertTrue(node instanceof Repetition);
    }

    @Test
    public void testMultiConcatenation() {
        Node node = RegexParser.parse("(ab)*a");
        assertNotNull(node);
        assertTrue(node instanceof Concatenation);
    }

    @Test
    public void testConcatenatedAlternations() {
        Node node = RegexParser.parse("(a|b)(c|d)");
        assertNotNull(node);
        assertTrue(node instanceof Concatenation);
    }

    @Test
    public void testComplexRegex() {
        Node node = RegexParser.parse("(ab)*a(a|b)(a|b)(a|b)");
        assertNotNull(node);
        assertTrue(node instanceof Concatenation);
    }

    @Test
    public void testCharRange() {
        Node node = RegexParser.parse("[0-9]");
        assertNotNull(node);
        assertTrue(node instanceof CharRangeNode);
    }

    @Test
    public void testMultiCharRange() {
        Node node = RegexParser.parse("[0-9A-Z]");
        assertNotNull(node);
        assertTrue(node instanceof Alternation);
    }

    @Test
    public void testConcatenatedCharRange() {
        Node node = RegexParser.parse("[ab]");
        assertNotNull(node);
        CharRangeNode charRangeNode = (CharRangeNode) node;
        assertTrue(charRangeNode.range().getStart() == 'a');
        assertTrue(charRangeNode.range().getEnd() == 'b');
    }

    @Test
    public void testConcatenatedParensWithRepetition() {
        Node node = RegexParser.parse("(AB)(CD)*");
        assertNotNull(node);
        assertTrue(node instanceof Concatenation);
    }

    @Test
    public void testConcatenatedParens() {
        Node node = RegexParser.parse("(AB)(CD)");
        assertNotNull(node);
        assertTrue(node instanceof Concatenation);
    }

    @Test
    public void testConcatenatedRegex() {
        Node node = RegexParser.parse("[A-Za-z][A-Za-z0-9]*");
        assertNotNull(node);
        assertTrue(node instanceof Concatenation);
    }
}
