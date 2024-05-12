package com.justinblank.strings;

import com.justinblank.strings.RegexAST.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

import static com.justinblank.strings.TestUtil.parse;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

public class RegexParserTest {

    static final Set<Character> ESCAPED_AS_LITERAL_CHARS = Set.of('*', '(', ')', '[', '$', '^', '+', ':', '?', '{');

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
        check(node, "b*");
    }

    @Test
    public void testCurlyRepetition() {
        Node node = parse("b{0,1}");
        assertNotNull(node);
        assertTrue(node instanceof CountedRepetition);
        CountedRepetition cr = (CountedRepetition) node;
        assertEquals(0, cr.min);
        assertEquals(1, cr.max);
        check(node, "b?");
    }

    @Test
    public void testCountedRepetitionNoComma() {
        parse("a{12}");
    }

    @Test
    public void testQuestionMarkSimple() {
        Node node = parse("1?");
        assertNotNull(node);
        assertTrue(node instanceof CountedRepetition);
        CountedRepetition cr = (CountedRepetition) node;
        assertEquals(0, cr.min);
        assertEquals(1, cr.max);
        check(node,"1?");
    }

    @Test
    public void testQuestionMarkGrouped() {
        Node node = parse("(1|2)?");
        assertNotNull(node);
        assertTrue(node instanceof CountedRepetition);
        CountedRepetition cr = (CountedRepetition) node;
        assertEquals(0, cr.min);
        assertEquals(1, cr.max);
        check(node, "(1|2)?");
    }

    @Test
    public void testTwoCharUnion() {
        Node node = parse("a|b");
        assertNotNull(node);
        check(node, "a|b");
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
        check(node, "ab*");
    }

    @Test
    public void testPlusHasHighPrecedence() {
        Node node = parse("ab+");
        check(node, "abb*");
    }

    @Test
    public void testBackslashInCharRange() {
        check(RegexParser.parse("[L-\\\\]"), "[L-\\\\]");
    }

    @Test
    public void testBracesHaveHighPrecedence() {
        check(parse("ab{1,2}"), "ab{1,2}");
    }

    @Test
    public void testUnionHasLowPrecedence() {
        Node node = parse("A|BCD|E");
        assertTrue(node instanceof Union);
        assertTrue(((Union) node).left instanceof Union);
        assertTrue(((Union) node).right instanceof LiteralNode);
        check(node, "(A|(BCD))|E");
    }

    @Test
    public void testMultiConcatenation() {
        Node node = parse("(ab)*a");
        assertNotNull(node);
        check(node, "(ab)*a");
    }

    @Test
    public void testConcatenatedUnions() {
        Node node = parse("(a|b)(c|d)");
        assertNotNull(node);
        assertTrue(node instanceof Concatenation);
        check(node, "(a|b)(c|d)");
    }

    @Test
    public void testBracketsFollowedBySomething() {
        Node node = parse("[Ss]h");
        check(node, "(S|s)h");
    }

    @Test
    public void testBracketsFollowedBySomethingWrappedInParens() {
        Node node = parse("([Ss]h)");
        check(node, "(S|s)h");
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
    public void testCountedRepetitionOfParenthesizedUnions() {
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
        assertTrue(node instanceof Union);
        check(node, "[0-9]|[A-Z]");
    }

    @Test
    public void testSillyContiguousRanges() {
        Node node = parse("[0-12-34-56-78-9]");
        assertNotNull(node);
        check(node, "[0-9]");
    }

    @Test
    public void testRepeatedCharRange() {
        Node node = parse("[0-9]*");
        check(node, "[0-9]*");
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
        check(node, "AB(CD)*");
    }

    @Test
    public void testUnionOfGroups() {
        String s = "(AB)|(BC)";
        Node node = parse(s);
        assertNotNull(node);
        assertTrue(node instanceof Union);
        check(node, "(AB)|(BC)");
    }

    @Test
    public void testUnionOfSingleCharsWithCountedRepetition() {
        Node node = parse("(A|B){1,2}");
        assertNotNull(node);
        assertTrue(node instanceof CountedRepetition);
        check(node,"(A|B){1,2}");
    }

    @Test
    public void testUnionOfMultipleCharsWithCountedRepetition() {
        Node node = parse("(A|(BC)){1,2}");
        assertNotNull(node);
        assertTrue(node instanceof CountedRepetition);
        check(node, "(A|(BC)){1,2}");
    }

    @Test
    public void testUnionWithExtraneousParens() {
        Node node = parse("((A)|(BC))");
        assertNotNull(node);
        assertTrue(node instanceof Union);
        Union union = (Union) node;
        assertTrue(union.left instanceof LiteralNode);
        assertTrue(union.right instanceof LiteralNode);
        check(node, "A|(BC)");
    }

    @Test
    public void testUnionOfUnions() {
        Node node = parse("(A|B)|(A|B)");
        assertNotNull(node);
        assertTrue(node instanceof Union);
        check(node, "(A|B)|(A|B)");
    }

    @Test
    public void testLiteralBeforeUnionOfLiteralAndUnion() {
        Node node = parse("A(BA|(A|BB))");
        assertNotNull(node);
        assertTrue(node instanceof Concatenation);
    }

    @Test
    public void testCountedRepetitionOfParenthesizedUnionOfLiterals() {
        String regex = "((A)|(B)|(CD)){1,2}";
        Node node = parse(regex);
        assertNotNull(node);
        assertTrue(node instanceof CountedRepetition);
        CountedRepetition cr = (CountedRepetition) node;
        assertEquals(1, cr.min);
        assertEquals(2, cr.max);
        assertTrue(cr.node instanceof Union);
        Union union = (Union) cr.node;
    }

    @Test
    public void testParseUnionWithCompositeLeftSide() {
        check("(a*tc*|t*ag*)*", "((a*tc*)|(t*ag*))*");
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

    // TODO: very slow--review regex parser code
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
        check(node, "(1|2){2,3}abc");
    }

    @Test
    public void testDuplicateQuestionMark() {
        // This is allowed by the Java regex library
        check(parse("a??"), "(a?)?");
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

    @Test
    public void testEscapedLiterals() {
        for (Character c : ESCAPED_AS_LITERAL_CHARS) {
            check(parse("\\" + c), "\\" + c);
        }
    }

    @Test
    public void testTrivialNestedBrackets() {
        check(parse("[[sa]]"), "a|s");
        check(parse("[[[sa]]]"), "a|s");
    }

    @Test
    public void testEscapedNestedBrackets() {
        check(parse("[\\[a]"), "\\[|a");
    }

    @Test
    public void testEscapedBracket() {
        check("\\[", "\\[");
    }

    @Test
    public void testEscapedBracketInCharRange() {
        check("[A-\\[]", "[A-\\[]");
    }

    static void check(String regex, String representation) {
        assertEquals(representation, NodePrinter.print(RegexParser.parse(regex)));
        assertNotNull(java.util.regex.Pattern.compile(representation));
    }

    static void check(Node node, String representation) {
        assertEquals(representation, NodePrinter.print(node));
        assertNotNull(java.util.regex.Pattern.compile(representation));
    }

    @Test
    public void testBellInBrackets() {
        parse("[\\a]");
    }

    @Test
    public void testDoubleCharEscapeInBrackets() {
        parse("[\\a\\d]");
    }

    @Test
    public void testTripleCharEscapeInBrackets() {
        parse("[\\a\\d\\D]");
    }

    @Test
    public void testLParenInBrackets() {
        parse("[(]");
    }

    @Test
    public void testRParensInBrackets() {
        parse("[)]");
    }

    @Test
    public void testUnbalancedRightBrace() {
        parse("}");
    }

    @Test
    public void testLeadingDashInCharClass() {
        parse("[-]");
    }

    @Test
    public void testRightUnbalancedBrackets() {
        parse("[]]");
    }

    @Test
    public void testPeriod() {
        parse(".");
    }

    @Test
    public void testParseEscapedPeriod() {
        parse("\\.");
    }

    @Test
    public void testRightMismatchedBracket() {
        parse("]");
    }

    @Test
    public void generativeParsingTest() {
        Random random = new Random();
        for (int maxSize = 1; maxSize < 24; maxSize++) {
            for (int i = 0; i < 1000; i++) {
                Node node = new RegexGenerator(random, maxSize).generate();
                String regex = NodePrinter.print(node);
                // catch any errors in our NodePrinter
                // TODO: remove this check
                try {
                    java.util.regex.Pattern.compile(regex);
                } catch (Exception e) {
                    continue; // throw e;
                }
                try {
                    RegexParser.parse(regex);
                } catch (Exception e) {
                    System.out.println(regex);
                    throw e;
                }
            }
        }
    }

    @Test
    public void testParsingHasFixedPoint() {
        Random random = new Random();
        for (int maxSize = 1; maxSize < 24; maxSize++) {
            for (int i = 0; i < 100; i++) {
                Node node = new RegexGenerator(random, maxSize).generate();
                checkParsingHasFixedPoint(node);
            }
        }
    }

    private void checkParsingHasFixedPoint(Node node) {
        String last = null;
        String regex = NodePrinter.print(node);

        for (int iteration = 0; iteration < 12; iteration++) {
            if (regex.equals(last)) {
                return;
            }
            node = RegexParser.parse(regex);
            last = regex;
            regex = NodePrinter.print(node);
        }
        assertFalse(false);
    }

    @Test
    public void testUnionOfUnionFollowedByLiteralWithLiteral() {
        String test = "(a|a)a|a";
        var node = RegexParser.parse(test);
        assertTrue(node instanceof Union);
    }
}

