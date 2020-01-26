package com.justinblank.strings;

import com.justinblank.strings.RegexAST.*;
import org.junit.Test;

import static com.justinblank.strings.RegexParser.*;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RegexParserTest {

    @Test
    public void testSingleChar() {
        Node node = parse("a");
        assertNotNull(node);
        assertTrue(node instanceof LiteralNode);
        check(node, "a");
    }

    @Test
    public void testTwoCharConcatenation() {
        Node node = parse("ab");
        assertNotNull(node);
        assertTrue(node instanceof LiteralNode);
        check(node, "ab");
    }

    @Test
    public void testMultipleCharConcatenation() {
        Node node = parse("abcdef");
        assertNotNull(node);
        check(node, "abcdef");
    }

    @Test
    public void testSingleCharRepetition() {
        Node node = parse("b*");
        assertNotNull(node);
        assertTrue(node instanceof Repetition);
        check(node, "(b)*");
    }

    @Test
    public void testCurlyRepetition() {
        Node node = parse("b{0,1}");
        assertNotNull(node);
        assertTrue(node instanceof CountedRepetition);
        CountedRepetition cr = (CountedRepetition) node;
        assertEquals(0, cr.min);
        assertEquals(1, cr.max);
        check(node, "(b){0,1}");
    }

    @Test
    public void testQuestionMarkSimple() {
        Node node = parse("1?");
        assertNotNull(node);
        assertTrue(node instanceof CountedRepetition);
        CountedRepetition cr = (CountedRepetition) node;
        assertEquals(0, cr.min);
        assertEquals(1, cr.max);
        check(node,"(1){0,1}");
    }

    @Test
    public void testQuestionMarkGrouped() {
        Node node = parse("(1|2)?");
        assertNotNull(node);
        assertTrue(node instanceof CountedRepetition);
        CountedRepetition cr = (CountedRepetition) node;
        assertEquals(0, cr.min);
        assertEquals(1, cr.max);
        check(node, "((1)|(2)){0,1}");
    }

    @Test
    public void testTwoCharAlternation() {
        Node node = parse("a|b");
        assertNotNull(node);
        check(node, "(a)|(b)");
    }

    @Test
    public void testGroupedRepetition() {
        Node node = parse("(ab)*");
        assertNotNull(node);
        check(node, "(ab)*");
    }

    @Test
    public void testRepetitionHasHighPrecedence() {
        Node node = parse("ab*");
        check(node, "(a)((b)*)");
    }

    @Test
    public void testPlusHasHighPrecedence() {
        Node node = parse("ab+");
        check(node, "(a)((b)((b)*))");
    }

    @Test
    public void testBracesHaveHighPrecedence() {
        check(parse("ab{1,2}"), "(a)((b){1,2})");
    }

    @Test
    public void testAlternationHasLowPrecedence() {
        Node node = parse("A|BCD|E");
        assertTrue(node instanceof Alternation);
        assertTrue(((Alternation) node).left instanceof Alternation);
        assertTrue(((Alternation) node).right instanceof LiteralNode);
        check(node, "((A)|(BCD))|(E)");
    }

    @Test
    public void testMultiConcatenation() {
        Node node = parse("(ab)*a");
        assertNotNull(node);
        check(node, "((ab)*)(a)");
    }

    @Test
    public void testConcatenatedAlternations() {
        Node node = parse("(a|b)(c|d)");
        assertNotNull(node);
        assertTrue(node instanceof Concatenation);
        check(node, "((a)|(b))((c)|(d))");
    }

    @Test
    public void testBracketsFollowedBySomething() {
        Node node = parse("[Ss]h");
        check(node, "((S)|(s))(h)");
    }

    @Test
    public void testBracketsFollowedBySomethingWrappedInParens() {
        Node node = parse("([Ss]h)");
        check(node, "((S)|(s))(h)");
    }

    @Test
    public void testConcatenatedLiteralsWrappedInParens() {
        Node node = parse("(abcdef)");
        check(node, "abcdef");

        node = parse("((abc)(def))");
        check(node, "abcdef");
    }

    @Test
    public void testComplexRegex() {
        Node node = parse("(ab)*a(a|b)(a|b)(a|b)");
        assertNotNull(node);
        assertTrue(node instanceof Concatenation);
    }

    @Test
    public void testSuperfluousParentheses() {
        Node node = parse("((1))");
        assertNotNull(node);
        assertTrue(node instanceof LiteralNode);
        check(node, "1");
    }

    @Test
    public void testCountedRepetitionOfParenthesizedAlternations() {
        Node node = parse("((12)|(34)){2,3}");
        assertNotNull(node);
        assertTrue(node instanceof CountedRepetition);
        check(node, "((12)|(34)){2,3}");
    }

    @Test
    public void testCharRange() {
        Node node = parse("[0-9]");
        assertNotNull(node);
        assertTrue(node instanceof CharRangeNode);
        check(node, "[0-9]");
    }

    @Test
    public void testMultiCharRange() {
        Node node = parse("[0-9A-Z]");
        assertNotNull(node);
        assertTrue(node instanceof Alternation);
        check(node, "([0-9])|([A-Z])");
    }

    @Test
    public void testSillyContiguousRanges() {
        Node node = parse("[0-12-34-56-78-9]");
        assertNotNull(node);
        check(node, "[0-9]");
    }

    @Test
    public void testConcatenatedCharRange() {
        Node node = parse("[ab]");
        assertNotNull(node);
        CharRangeNode charRangeNode = (CharRangeNode) node;
        assertTrue(charRangeNode.range().getStart() == 'a');
        assertTrue(charRangeNode.range().getEnd() == 'b');
        check(node, "[a-b]");
    }

    @Test
    public void testConcatenatedParensWithRepetition() {
        Node node = parse("(AB)(CD)*");
        assertNotNull(node);
        assertTrue(node instanceof Concatenation);
        check(node, "(AB)((CD)*)");
    }

    @Test
    public void testAlternationOfGroups() {
        String s = "(AB)|(BC)";
        Node node = parse(s);
        assertNotNull(node);
        assertTrue(node instanceof Alternation);
        check(node, "(AB)|(BC)");
    }

    @Test
    public void testAlternationOfSingleCharsWithCountedRepetition() {
        Node node = parse("(A|B){1,2}");
        assertNotNull(node);
        assertTrue(node instanceof CountedRepetition);
        check(node,"((A)|(B)){1,2}");
    }

    @Test
    public void testAlternationOfMultipleCharsWithCountedRepetition() {
        Node node = parse("(A|(BC)){1,2}");
        assertNotNull(node);
        assertTrue(node instanceof CountedRepetition);
        check(node, "((A)|(BC)){1,2}");
    }

    @Test
    public void testAlternationWithExtraneousParens() {
        Node node = parse("((A)|(BC))");
        assertNotNull(node);
        assertTrue(node instanceof Alternation);
        Alternation alt = (Alternation) node;
        assertTrue(alt.left instanceof LiteralNode);
        assertTrue(alt.right instanceof LiteralNode);
        check(node, "(A)|(BC)");
    }

    @Test
    public void testSomething() {
        String regex = "((A)|(B)|(CD)){1,2}";
        Node node = parse(regex);
        assertNotNull(node);
        assertTrue(node instanceof CountedRepetition);
        CountedRepetition cr = (CountedRepetition) node;
        assertEquals(1, cr.min);
        assertEquals(2, cr.max);
        assertTrue(cr.node instanceof Alternation);
        Alternation alt = (Alternation) cr.node;
    }

    @Test
    public void testConcatenatedParens() {
        Node node = parse("(AB)(CD)");
        assertNotNull(node);
        assertTrue(node instanceof LiteralNode);
        check(node,"ABCD");
    }

    @Test
    public void testConcatenatedRanges() {
        Node node = parse("[A-Za-z][A-Za-z0-9]*");
        assertNotNull(node);
        assertTrue(node instanceof Concatenation);
    }

    @Test
    public void testParseLongRegexes() {
        String regexString = "a".repeat(30000);
        Node node = parse(regexString);
        assertNotNull(node);
        assertTrue(node instanceof LiteralNode);
        check(node, regexString);
    }

    @Test
    public void testConcatenationAfterCountedReptition() {
        Node node = parse("(1|2){2,3}abc");
        assertNotNull(node);
        assertTrue(node instanceof Concatenation);
        Node head = ((Concatenation) node).head;
        Node tail = ((Concatenation) node).tail;
        assertTrue(head instanceof CountedRepetition);
        assertTrue(tail instanceof LiteralNode);
        check(node, "(((1)|(2)){2,3})(abc)");
    }

    @Test
    public void testDuplicateQuestionMark() {
        // This is allowed by the Java regex library
        check(parse("a??"), "((a){0,1}){0,1}");
    }

    @Test
    public void testSingleBracketElement() {
        check( RegexParser.parse("[a]"), "a");
    }

    @Test
    public void testEmptyString() {
        check(parse(""), "");
    }

    @Test
    public void testEmptyParens() {
        check(parse("()"), "");
        check(parse("(())"), "");
    }

    private static void check(Node node, String representation) {
        assertEquals(representation, NodePrinter.print(node));
        assertNotNull(java.util.regex.Pattern.compile(representation));
    }
}
