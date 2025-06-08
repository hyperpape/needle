package com.justinblank.strings;

import com.justinblank.strings.RegexAST.Node;
import org.junit.jupiter.api.Test;

import static com.justinblank.strings.TestUtil.parse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

public class RegexParserMalformedRegexTest {

    @Test
    void leftMismatchedBracket() {
        assertThrows(RegexSyntaxException.class, () ->
            parse("["));
    }

    @Test
    void leftMismatchedBracketWithChars() {
        assertThrows(RegexSyntaxException.class, () ->
            parse("[ab"));
    }

    @Test
    void leftUnbalancedBrackets() {
        assertThrows(RegexSyntaxException.class, () ->
            parse("[[]"));
    }

    @Test
    void insideOutBrackets() {
        assertThrows(RegexSyntaxException.class, () ->
            parse("]["));
    }

    @Test
    void escapeInCharRange() {
        assertThrows(RegexSyntaxException.class, () ->
            parse("[a-\\c]"));
    }

    @Test
    void badRepetitionCountRight() {
        assertThrows(RegexSyntaxException.class, () ->
            parse("a{1,b}"));
    }

    @Test
    void badRepetitionLeft() {
        assertThrows(RegexSyntaxException.class, () ->
            parse("a{b,1}"));
    }

    @Test
    void badRepetitionNoCommaOrBracket() {
        assertThrows(RegexSyntaxException.class, () ->
            parse("a{b"));
    }

    @Test
    void badRepetitionNoCommaOrSecondNumber() {
        assertThrows(RegexSyntaxException.class, () ->
            parse("a{b}"));
    }

    @Test
    void badRepetitionOnlyOneNumberThenBadChar() {
        assertThrows(RegexSyntaxException.class, () ->
            parse("a{1a}"));
    }

    @Test
    void badRepetitionNoComma() {
        assertThrows(RegexSyntaxException.class, () ->
            parse("a{b1}"));
    }

    @Test
    void badRepetitionNoBracket() {
        assertThrows(RegexSyntaxException.class, () ->
            parse("a{b,}"));
    }

    @Test
    void emptyRegexLeading() {
        assertThrows(RegexSyntaxException.class, () ->
            parse("{}"));
    }

    @Test
    void badRepetitionBoth() {
        assertThrows(RegexSyntaxException.class, () ->
            parse("a{b,1}"));
    }

    @Test
    void badRepetitionTrailingChar() {
        assertThrows(RegexSyntaxException.class, () ->
            parse("a{1,2a}"));
    }

    @Test
    void badRepetitionTrailingCharNoBrace() {
        assertThrows(RegexSyntaxException.class, () ->
            parse("a{1,2a"));
    }

    @Test
    void emptyBrace() {
        assertThrows(RegexSyntaxException.class, () ->
            parse("{}"));
    }

    @Test
    void badRepetitionMissingBrace() {
        assertThrows(RegexSyntaxException.class, () ->
            parse("a{1,2"));
    }

    @Test
    void leftUnbalancedParens() {
        assertThrows(RegexSyntaxException.class, () -> {
            Node node = parse("(((");
        });
    }

    @Test
    void rightUnbalancedParens() {
        assertThrows(RegexSyntaxException.class, () -> {
            Node node = parse(")))");
        });
    }

    @Test
    void mixedUnbalancedParens() {
        assertThrows(RegexSyntaxException.class, () -> {
            Node node = parse("((())");
        });
    }

    @Test
    void sequenceOfBalancedParensWithExtraLeft() {
        assertThrows(RegexSyntaxException.class, () ->
            parse("(()()"));
    }

    @Test
    void sequenceOfBalancedParensWithExtraRight() {
        assertThrows(RegexSyntaxException.class, () ->
            parse("()())"));
    }

    @Test
    void malformedQuestionMarkEmpty() {
        assertThrows(RegexSyntaxException.class, () -> {
            Node node = parse("?");
        });
    }

    @Test
    void malformedQuestionMarkLeading() {
        assertThrows(RegexSyntaxException.class, () -> {
            Node node = parse("?a");
        });
    }

    @Test
    void plusEmpty() {
        assertThrows(RegexSyntaxException.class, () -> {
            Node node = parse("+");
        });
    }

    @Test
    void charClassWithTrailingDash() {
        assertThrows(RegexSyntaxException.class, () ->
            parse("[-"));
    }

    @Test
    void starEmpty() {
        assertThrows(RegexSyntaxException.class, () -> {
            Node node = parse("*");
        });
    }

    @Test
    void plusLeading() {
        assertThrows(RegexSyntaxException.class, () -> {
            Node node = parse("+a");
        });
    }

    @Test
    void badOctal0999() {
        expectError("\\0999");
    }

    @Test
    void badEscapes() {
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
            fail("Expected syntax exception");
        }
    }

    @Test
    void failsOddNumberOfBackslashes() {
        for (int i = 1; i < 20; i += 2) {
            String escapes = "\\".repeat(i);
            expectError(escapes);
            expectError("abcd" + escapes);

        }
    }

    @Test
    void literalAlternationLeftParen() {
        expectError("a|(");
    }

    public static void expectError(String regexString) {
        try {
            parse(regexString);
        }
        catch (RegexSyntaxException e) {
            return;
        }
        fail("Expected RegexSyntaxException from regex: '" + regexString + "'");
    }

}
