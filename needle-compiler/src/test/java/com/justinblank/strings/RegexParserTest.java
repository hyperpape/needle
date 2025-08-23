package com.justinblank.strings;

import com.justinblank.strings.RegexAST.*;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.Set;

import static com.justinblank.strings.TestUtil.parse;
import static org.junit.jupiter.api.Assertions.*;

class RegexParserTest {

    static final Set<Character> ESCAPED_AS_LITERAL_CHARS = Set.of('*', '(', ')', '[', '$', '^', '+', ':', '?', '{');

    @Test
    void singleChar() {
        Node node = parse("a");
        assertNotNull(node);
        assertTrue(node instanceof LiteralNode);
        check(node, "a");
    }

    @Test
    void twoCharConcatenation() {
        Node node = parse("ab");
        assertNotNull(node);
        assertTrue(node instanceof LiteralNode);
        check(node, "ab");
    }

    @Test
    void multipleCharConcatenation() {
        Node node = parse("abcdef");
        assertNotNull(node);
        check(node, "abcdef");
    }

    @Test
    void singleCharRepetition() {
        Node node = parse("b*");
        assertNotNull(node);
        assertTrue(node instanceof Repetition);
        check(node, "b*");
    }

    @Test
    void curlyRepetition() {
        Node node = parse("b{0,1}");
        assertNotNull(node);
        assertTrue(node instanceof CountedRepetition);
        CountedRepetition cr = (CountedRepetition) node;
        assertEquals(0, cr.min);
        assertEquals(1, cr.max);
        check(node, "b?");
    }

    @Test
    void countedRepetitionNoComma() {
        parse("a{12}");
    }

    @Test
    void questionMarkSimple() {
        Node node = parse("1?");
        assertNotNull(node);
        assertTrue(node instanceof CountedRepetition);
        CountedRepetition cr = (CountedRepetition) node;
        assertEquals(0, cr.min);
        assertEquals(1, cr.max);
        check(node,"1?");
    }

    @Test
    void questionMarkGrouped() {
        Node node = parse("(1|2)?");
        assertNotNull(node);
        assertTrue(node instanceof CountedRepetition);
        CountedRepetition cr = (CountedRepetition) node;
        assertEquals(0, cr.min);
        assertEquals(1, cr.max);
        check(node, "(1|2)?");
    }

    @Test
    void twoCharUnion() {
        Node node = parse("a|b");
        assertNotNull(node);
        check(node, "a|b");
    }

    @Test
    void groupedRepetition() {
        Node node = parse("(ab)*");
        assertNotNull(node);
        check(node, "(ab)*");
    }

    @Test
    void repetitionHasHighPrecedence() {
        Node node = parse("ab*");
        check(node, "ab*");
    }

    @Test
    void plusHasHighPrecedence() {
        Node node = parse("ab+");
        check(node, "abb*");
    }

    @Test
    void backslashInCharRange() {
        check(RegexParser.parse("[L-\\\\]"), "[L-\\\\]");
    }

    @Test
    void bracesHaveHighPrecedence() {
        check(parse("ab{1,2}"), "ab{1,2}");
    }

    @Test
    void unionHasLowPrecedence() {
        Node node = parse("A|BCD|E");
        assertTrue(node instanceof Union);
        assertTrue(((Union) node).left instanceof Union);
        assertTrue(((Union) node).right instanceof LiteralNode);
        check(node, "(A|(BCD))|E");
    }

    @Test
    void multiConcatenation() {
        Node node = parse("(ab)*a");
        assertNotNull(node);
        check(node, "(ab)*a");
    }

    @Test
    void concatenatedUnions() {
        Node node = parse("(a|b)(c|d)");
        assertNotNull(node);
        assertTrue(node instanceof Concatenation);
        check(node, "(a|b)(c|d)");
    }

    @Test
    void bracketsFollowedBySomething() {
        Node node = parse("[Ss]h");
        check(node, "(S|s)h");
    }

    @Test
    void bracketsFollowedBySomethingWrappedInParens() {
        Node node = parse("([Ss]h)");
        check(node, "(S|s)h");
    }

    @Test
    void concatenatedLiteralsWrappedInParens() {
        Node node = parse("(abcdef)");
        check(node, "abcdef");

        node = parse("((abc)(def))");
        check(node, "abcdef");
    }

    @Test
    void complexRegex() {
        Node node = parse("(ab)*a(a|b)(a|b)(a|b)");
        assertNotNull(node);
        assertTrue(node instanceof Concatenation);
    }

    @Test
    void superfluousParentheses() {
        Node node = parse("((1))");
        assertNotNull(node);
        assertTrue(node instanceof LiteralNode);
        check(node, "1");
    }

    @Test
    void countedRepetitionOfParenthesizedUnions() {
        Node node = parse("((12)|(34)){2,3}");
        assertNotNull(node);
        assertTrue(node instanceof CountedRepetition);
        check(node, "((12)|(34)){2,3}");
    }

    @Test
    void charRange() {
        Node node = parse("[0-9]");
        assertNotNull(node);
        assertTrue(node instanceof CharRangeNode);
        check(node, "[0-9]");
    }

    @Test
    void multiCharRange() {
        Node node = parse("[0-9A-Z]");
        assertNotNull(node);
        assertTrue(node instanceof Union);
        check(node, "[0-9]|[A-Z]");
    }

    @Test
    void sillyContiguousRanges() {
        Node node = parse("[0-12-34-56-78-9]");
        assertNotNull(node);
        check(node, "[0-9]");
    }

    @Test
    void repeatedCharRange() {
        Node node = parse("[0-9]*");
        check(node, "[0-9]*");
    }

    @Test
    void concatenatedCharRange() {
        Node node = parse("[ab]");
        assertNotNull(node);
        CharRangeNode charRangeNode = (CharRangeNode) node;
        assertEquals('a', charRangeNode.range().getStart());
        assertEquals('b', charRangeNode.range().getEnd());
        check(node, "[a-b]");
    }

    @Test
    void concatenatedParensWithRepetition() {
        Node node = parse("(AB)(CD)*");
        assertNotNull(node);
        assertTrue(node instanceof Concatenation);
        check(node, "AB(CD)*");
    }

    @Test
    void unionOfGroups() {
        String s = "(AB)|(BC)";
        Node node = parse(s);
        assertNotNull(node);
        assertTrue(node instanceof Union);
        check(node, "(AB)|(BC)");
    }

    @Test
    void unionOfSingleCharsWithCountedRepetition() {
        Node node = parse("(A|B){1,2}");
        assertNotNull(node);
        assertTrue(node instanceof CountedRepetition);
        check(node,"(A|B){1,2}");
    }

    @Test
    void unionOfMultipleCharsWithCountedRepetition() {
        Node node = parse("(A|(BC)){1,2}");
        assertNotNull(node);
        assertTrue(node instanceof CountedRepetition);
        check(node, "(A|(BC)){1,2}");
    }

    @Test
    void unionWithExtraneousParens() {
        Node node = parse("((A)|(BC))");
        assertNotNull(node);
        assertTrue(node instanceof Union);
        Union union = (Union) node;
        assertTrue(union.left instanceof LiteralNode);
        assertTrue(union.right instanceof LiteralNode);
        check(node, "A|(BC)");
    }

    @Test
    void unionOfUnions() {
        Node node = parse("(A|B)|(A|B)");
        assertNotNull(node);
        assertTrue(node instanceof Union);
        check(node, "(A|B)|(A|B)");
    }

    @Test
    void literalBeforeUnionOfLiteralAndUnion() {
        Node node = parse("A(BA|(A|BB))");
        assertNotNull(node);
        assertTrue(node instanceof Concatenation);
    }

    @Test
    void countedRepetitionOfParenthesizedUnionOfLiterals() {
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
    void parseUnionWithCompositeLeftSide() {
        check("(a*tc*|t*ag*)*", "((a*tc*)|(t*ag*))*");
    }

    @Test
    void concatenatedParens() {
        Node node = parse("(AB)(CD)");
        assertNotNull(node);
        assertTrue(node instanceof LiteralNode);
        check(node,"ABCD");
    }

    @Test
    void concatenatedRanges() {
        Node node = parse("[A-Za-z][A-Za-z0-9]*");
        assertNotNull(node);
        assertTrue(node instanceof Concatenation);
    }

    // TODO: very slow--review regex parser code
    @Test
    void parseLongRegexes() {
        String regexString = "a".repeat(30000);
        Node node = parse(regexString);
        assertNotNull(node);
        assertTrue(node instanceof LiteralNode);
        check(node, regexString);
    }

    @Test
    void concatenationAfterCountedReptition() {
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
    void duplicateQuestionMark() {
        // This is allowed by the Java regex library
        check(parse("a??"), "(a?)?");
    }

    @Test
    void singleBracketElement() {
        check( RegexParser.parse("[a]"), "a");
    }

    @Test
    void emptyString() {
        check(parse(""), "");
    }

    @Test
    void emptyParens() {
        check(parse("()"), "");
        check(parse("(())"), "");
    }

    @Test
    void escapedLiterals() {
        for (Character c : ESCAPED_AS_LITERAL_CHARS) {
            check(parse("\\" + c), "\\" + c);
        }
    }

    @Test
    void trivialNestedBrackets() {
        check(parse("[[sa]]"), "a|s");
        check(parse("[[[sa]]]"), "a|s");
    }

    @Test
    void escapedNestedBrackets() {
        check(parse("[\\[a]"), "\\[|a");
    }

    @Test
    void escapedBracket() {
        check("\\[", "\\[");
    }

    @Test
    void escapedBracketInCharRange() {
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
    void bellInBrackets() {
        parse("[\\a]");
    }

    @Test
    void doubleCharEscapeInBrackets() {
        parse("[\\a\\d]");
    }

    @Test
    void tripleCharEscapeInBrackets() {
        parse("[\\a\\d\\D]");
    }

    @Test
    void lParenInBrackets() {
        parse("[(]");
    }

    @Test
    void rParensInBrackets() {
        parse("[)]");
    }

    @Test
    void unbalancedRightBrace() {
        parse("}");
    }

    @Test
    void leadingDashInCharClass() {
        parse("[-]");
    }

    @Test
    void rightUnbalancedBrackets() {
        parse("[]]");
    }

    @Test
    void doubleBrackets_withInternalCharRange() {
        parse("[[a-c]]");
    }

    @Test
    void period() {
        parse(".");
    }

    @Test
    void parseEscapedPeriod() {
        parse("\\.");
    }

    @Test
    void rightMismatchedBracket() {
        parse("]");
    }

    @Test
    void generativeParsingTest() {
        Random random = new Random();
        for (int maxSize = 1; maxSize < 24; maxSize++) {
            for (int i = 0; i < 1000; i++) {
                Node node = new RegexGenerator(random, maxSize).generate();
                String regex = NodePrinter.print(node);
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
    void parsingHasFixedPoint() {
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
    void unionOfUnionFollowedByLiteralWithLiteral() {
        String test = "(a|a)a|a";
        var node = RegexParser.parse(test);
        assertTrue(node instanceof Union);
    }
}

