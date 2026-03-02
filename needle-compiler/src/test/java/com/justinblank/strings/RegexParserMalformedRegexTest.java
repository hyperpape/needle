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
        assertThrows(PatternSyntaxException.class, () ->
            parse("["));
    }

    @Test
    void leftMismatchedBracketWithChars() {
        assertThrows(PatternSyntaxException.class, () ->
            parse("[ab"));
    }

    @Test
    void leftUnbalancedBrackets() {
        assertThrows(PatternSyntaxException.class, () ->
            parse("[[]"));
    }

    @Test
    void doubleBrackets() {
        assertThrows(PatternSyntaxException.class, () -> {
            parse("[[]]");
        });
    }

    @Test
    void reversedRange() {
        assertThrows(PatternSyntaxException.class, () -> {
            parse("[c-a]");
        });
    }

    @Test
    void insideOutBrackets() {
        assertThrows(PatternSyntaxException.class, () ->
            parse("]["));
    }

    @Test
    void escapeInCharRange() {
        assertThrows(PatternSyntaxException.class, () ->
            parse("[a-\\c]"));
    }

    @Test
    void badRepetitionCountRight() {
        assertThrows(PatternSyntaxException.class, () ->
            parse("a{1,b}"));
    }

    @Test
    void badRepetitionLeft() {
        assertThrows(PatternSyntaxException.class, () ->
            parse("a{b,1}"));
    }

    @Test
    void badRepetitionNoCommaOrBracket() {
        assertThrows(PatternSyntaxException.class, () ->
            parse("a{b"));
    }

    @Test
    void badRepetitionNoCommaOrSecondNumber() {
        assertThrows(PatternSyntaxException.class, () ->
            parse("a{b}"));
    }

    @Test
    void badRepetitionOnlyOneNumberThenBadChar() {
        assertThrows(PatternSyntaxException.class, () ->
            parse("a{1a}"));
    }

    @Test
    void badRepetitionNoComma() {
        assertThrows(PatternSyntaxException.class, () ->
            parse("a{b1}"));
    }

    @Test
    void badRepetitionNoBracket() {
        assertThrows(PatternSyntaxException.class, () ->
            parse("a{b,}"));
    }

    @Test
    void badRepetitionBoth() {
        assertThrows(PatternSyntaxException.class, () ->
            parse("a{b,1}"));
    }

    @Test
    void badRepetitionTrailingChar() {
        assertThrows(PatternSyntaxException.class, () ->
            parse("a{1,2a}"));
    }

    @Test
    void badRepetitionTrailingCharNoBrace() {
        assertThrows(PatternSyntaxException.class, () ->
            parse("a{1,2a"));
    }

    @Test
    void emptyBrace() {
        assertThrows(PatternSyntaxException.class, () ->
            parse("{}"));
    }

    @Test
    void badRepetitionMissingBrace() {
        assertThrows(PatternSyntaxException.class, () ->
            parse("a{1,2"));
    }

    @Test
    void leftUnbalancedParens() {
        assertThrows(PatternSyntaxException.class, () -> {
            Node node = parse("(((");
        });
    }

    @Test
    void rightUnbalancedParens() {
        assertThrows(PatternSyntaxException.class, () -> {
            Node node = parse(")))");
        });
    }

    @Test
    void mixedUnbalancedParens() {
        assertThrows(PatternSyntaxException.class, () -> {
            Node node = parse("((())");
        });
    }

    @Test
    void sequenceOfBalancedParensWithExtraLeft() {
        assertThrows(PatternSyntaxException.class, () ->
            parse("(()()"));
    }

    @Test
    void sequenceOfBalancedParensWithExtraRight() {
        assertThrows(PatternSyntaxException.class, () ->
            parse("()())"));
    }

    @Test
    void malformedQuestionMarkEmpty() {
        assertThrows(PatternSyntaxException.class, () -> {
            Node node = parse("?");
        });
    }

    @Test
    void malformedQuestionMarkLeading() {
        assertThrows(PatternSyntaxException.class, () -> {
            Node node = parse("?a");
        });
    }

    @Test
    void plusEmpty() {
        assertThrows(PatternSyntaxException.class, () -> {
            Node node = parse("+");
        });
    }

    @Test
    void charClassWithTrailingDash() {
        assertThrows(PatternSyntaxException.class, () ->
            parse("[-"));
        assertThrows(PatternSyntaxException.class, () ->
                parse("[a-"));
        assertThrows(PatternSyntaxException.class, () ->
                parse("[[a-]"));
    }

    @Test
    void starEmpty() {
        assertThrows(PatternSyntaxException.class, () -> {
            Node node = parse("*");
        });
    }

    @Test
    void plusLeading() {
        assertThrows(PatternSyntaxException.class, () -> {
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
    void rejectsBackReferences() {
        parseErrorOnJavaAcceptedRegex("(abc)\\1");
    }

    @Test
    void rejectsJavaQuotation() {
        parseErrorOnJavaAcceptedRegex("\\Q");
        parseErrorOnJavaAcceptedRegex("\\E");
    }

    @Test
    void rejectsReluctantQuantifiers() {
        parseErrorOnJavaAcceptedRegex("X??");
        parseErrorOnJavaAcceptedRegex("X+?");
        parseErrorOnJavaAcceptedRegex("X*?");
        parseErrorOnJavaAcceptedRegex("X{4,5}?");
    }

    @Test
    void rejectsPossessiveQuantifiers() {
        parseErrorOnJavaAcceptedRegex("X?+");
        parseErrorOnJavaAcceptedRegex("X++");
        parseErrorOnJavaAcceptedRegex("X*+");
        parseErrorOnJavaAcceptedRegex("X{4,5}+");
    }

    @Test
    void bareReluctantQuantifiers() {
        assertThrows(PatternSyntaxException.class, () -> {
            parse("??");
        });
        assertThrows(PatternSyntaxException.class, () -> {
            parse("*?");
        });
        assertThrows(PatternSyntaxException.class, () -> {
            parse("+?");
        });
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
        } catch (PatternSyntaxException e) {
            return;
        }
        fail("Expected an error parsing '" + s + "'");
    }

    public static void expectError(String regexString) {
        try {
            parse(regexString);
        }
        catch (PatternSyntaxException e) {
            return;
        }
        fail("Expected RegexSyntaxException from regex: '" + regexString + "'");
    }
}
