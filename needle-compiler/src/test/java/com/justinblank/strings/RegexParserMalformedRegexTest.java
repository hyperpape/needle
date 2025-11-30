package com.justinblank.strings;

import com.justinblank.strings.RegexAST.Node;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.justinblank.strings.TestUtil.parse;
import static org.junit.jupiter.api.Assertions.*;

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
    void doubleBrackets() {
        assertThrows(RegexSyntaxException.class, () -> {
            parse("[[]]");
        });
    }

    @Test
    void reversedRange() {
        assertThrows(RegexSyntaxException.class, () -> {
            parse("[c-a]");
        });
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
        assertThrows(RegexSyntaxException.class, () ->
                parse("[a-"));
        assertThrows(RegexSyntaxException.class, () ->
                parse("[[a-]"));
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

    @ParameterizedTest
    @ValueSource(strings ={ "\\p{IsLatin}", "\\p{InGreek}", "\\p{Lu}", "\\p{IsAlphabetic}", "\\p{Sc}" })
    void pBrackets(String s) {
        parseErrorOnJavaAcceptedRegex(s);
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

    private void parseErrorOnJavaAcceptedRegex(String s) {
        try {
            Node node = RegexParser.parse(s);
        } catch (RegexSyntaxException e) {
            return;
        }
        fail("Expected an error parsing '" + s + "'");
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
