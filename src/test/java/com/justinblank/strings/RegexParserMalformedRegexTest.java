package com.justinblank.strings;

import com.justinblank.strings.RegexAST.Node;
import org.junit.Test;

import java.util.regex.Pattern;

import static com.justinblank.strings.RegexParser.parse;

public class RegexParserMalformedRegexTest {

    @Test(expected = RegexSyntaxException.class)
    public void testLeftMismatchedBracket() {
        RegexParser.parse("[");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testRightMismatchedBracket() {
        RegexParser.parse("]");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testLeftUnbalancedBrackets() {
        RegexParser.parse("[[]");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testLeadingDashInCharClass() {
        RegexParser.parse("[-]");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testRightUnbalancedBrackets() {
        RegexParser.parse("[]]");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testInsideOutBrackets() {
        RegexParser.parse("][");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testLParenInBrackets() {
        RegexParser.parse("[(]");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testRParensInBrackets() {
        RegexParser.parse("[)]");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testBadRepetitionCountRight() {
        RegexParser.parse("a{1,b}");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testBadRepetitionLeft() {
        RegexParser.parse("a{b,1}");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testEmptyRegexLeading() {
        RegexParser.parse("{}");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testBadReptitionMissingComma() {
        RegexParser.parse("a{12}");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testBadRepetitionBoth() {
        RegexParser.parse("a{b,1}");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testBadRepetitionTrailingChar() {
        RegexParser.parse("a{1,2a}");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testBadRepetitionTrailingCharNoBrace() {
        RegexParser.parse("a{1,2a");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testBadRepetitionMissingBrace() {
        RegexParser.parse("a{1,2");
    }

    @Test(expected  = RegexSyntaxException.class)
    public void testUnbalancedRightBrace() {
        RegexParser.parse("}");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testUnbalancedRightBraceAfterChars() {
        RegexParser.parse("abc}");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testLeftUnbalancedParens() {
        Node node = parse("(((");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testRightUnbalancedParens() {
        Node node = parse(")))");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testMixedUnbalancedParens() {
        Node node = parse("((())");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testSequenceOfBalancedParensWithExtraLeft() {
        parse("(()()");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testSequenceOfBalancedParensWithExtraRight() {
        parse("()())");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testMalformedQuestionMarkEmpty() {
        Node node = parse("?");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testMalformedQuestionMarkLeading() {
        Node node = parse("?a");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testPlusEmpty() {
        Node node = parse("+");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testPlusLeading() {
        Node node = parse("+a");
    }

}
