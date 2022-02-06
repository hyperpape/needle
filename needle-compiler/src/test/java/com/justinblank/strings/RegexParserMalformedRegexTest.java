package com.justinblank.strings;

import com.justinblank.strings.RegexAST.Node;
import org.junit.Assert;
import org.junit.Test;

import static com.justinblank.strings.TestUtil.parse;
import static org.junit.Assert.assertTrue;

public class RegexParserMalformedRegexTest {

    @Test(expected = RegexSyntaxException.class)
    public void testLeftMismatchedBracket() {
        parse("[");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testLeftUnbalancedBrackets() {
        parse("[[]");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testInsideOutBrackets() {
        parse("][");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testEscapeInCharRange() {
        parse("[a-\\c]");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testBadRepetitionCountRight() {
        parse("a{1,b}");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testBadRepetitionLeft() {
        parse("a{b,1}");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testBadRepetitionNoCommaOrBracket() {
        parse("a{b");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testBadRepetitionNoCommaOrSecondNumber() {
        parse("a{b}");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testBadRepetitionOnlyOneNumberThenBadChar() {
        parse("a{1a}");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testBadRepetitionNoComma() {
        parse("a{b1}");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testBadRepetitionNoBracket() {
        parse("a{b,}");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testEmptyRegexLeading() {
        parse("{}");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testBadRepetitionBoth() {
        parse("a{b,1}");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testBadRepetitionTrailingChar() {
        parse("a{1,2a}");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testBadRepetitionTrailingCharNoBrace() {
        parse("a{1,2a");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testEmptyBrace() {
        parse("{}");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testBadRepetitionMissingBrace() {
        parse("a{1,2");
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
    public void testCharClassWithTrailingDash() {
        parse("[-");
    }

    @Test(expected = RegexSyntaxException.class)
    public void testStarEmpty() { Node node = parse("*"); }

    @Test(expected = RegexSyntaxException.class)
    public void testPlusLeading() {
        Node node = parse("+a");
    }

    @Test
    public void testBadOctal0999() {
        expectError("\\0999");
    }

    @Test
    public void testBadEscapes() {
        char[] badChars = new char[]{'A', 'b', 'B', 'c', 'C', 'E', 'F', 'g', 'G', 'H', 'i', 'I', 'j', 'J', 'k', 'K',
                'l', 'L', 'm', 'M', 'n', 'N', 'o', 'O', 'p', 'P', 'q', 'Q', 'r', 'R', 'T', 'u', 'U', 'v', 'V', 'X',
                'y', 'Y', 'z', 'Z'};
        for (char c : badChars) {
            try {
                RegexParser.parse("\\" + c);
            }
            catch (RegexSyntaxException e) {
                continue;
            }
            Assert.fail("Expected syntax exception");
        }
    }

    @Test
    public void testFailsOddNumberOfBackslashes() {
        for (int i = 1; i < 20; i += 2) {
            String escapes = "\\".repeat(i);
            expectError(escapes);
            expectError("abcd" + escapes);

        }
    }

    @Test
    public void testLiteralAlternationLeftParen() {
        expectError("a|(");
    }

    public static void expectError(String regexString) {
        try {
            parse(regexString);
        }
        catch (RegexSyntaxException e) {
            return;
        }
        Assert.fail("Expected RegexSyntaxException from regex: '" + regexString + "'");
    }

}
