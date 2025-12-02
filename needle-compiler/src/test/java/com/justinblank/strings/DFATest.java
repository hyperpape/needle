package com.justinblank.strings;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class DFATest {

    @Test
    void afterSingleCharLiteralRegex() {
        var dfa = DFA.createDFA("a");
        assertThat(dfa.after("a")).isPresent();
        assertThat(dfa.after("b")).isEmpty();
    }

    @Test
    void afterMultiCharLiteralRegex() {
        var dfa = DFA.createDFA("abc");
        assertThat(dfa.after("a")).isPresent();
        assertThat(dfa.after("ab")).isPresent();
        assertThat(dfa.after("abc")).isPresent();
        assertThat(dfa.after("b")).isEmpty();
    }


    @Test
    void after() {
        var dfa = DFA.createDFA("ab*[0-9]c");
        assertThat(dfa.after("a")).isPresent();
        assertThat(dfa.after("ab")).isPresent();
        assertThat(dfa.after("abbbb4c")).isPresent();
        assertThat(dfa.after("b")).isEmpty();
    }

    @Test
    void isTerminal() {
        var dfa = DFA.createDFA("abc", ConversionMode.DFA_SEARCH, 0);
        assertFalse(dfa.isTerminal());
        assertFalse(dfa.after("a").get().isTerminal());
        assertFalse(dfa.after("ab").get().isTerminal());
        assertTrue(dfa.after("abc").get().isTerminal());
    }

    @Test
    void calculateOffsetRepetition() {
        var dfa = DFA.createDFA("b*c");
        assertThat(dfa.calculateOffset()).isEmpty();
    }

    @Test
    void calculateOffsetLiteral() {
        var dfa = DFA.createDFA("abc");
        var offset = dfa.calculateOffset();
        check(offset, 2, 'c', 'c');
    }

    @Test
    void calculateOffsetLiteralFollowedByRange() {
        var dfa = DFA.createDFA("ab[c-d]");
        var optOffset = dfa.calculateOffset();
        check(optOffset, 2, 'c', 'd');
    }

    @Test
    void calculateOffsetWithInternalCharRange() {
        var dfa = DFA.createDFA("a[0-9]c");
        var optOffset = dfa.calculateOffset();
        check(optOffset, 2, 'c', 'c');
    }

    @Test
    void calculateOffsetWithInternalUnionWithSameChild() {
        var dfa = DFA.createDFA("a([0-9]|b)c");
        var optOffset = dfa.calculateOffset();
        check(optOffset, 2, 'c', 'c');
    }

    @Test
    void calculateOffsetWithInternalUnionWithDifferentChild() {
        var dfa = DFA.createDFA("a(([0-9]c)|([a-z]d))");
        var optOffset = dfa.calculateOffset();
        assertThat(optOffset).isEmpty();
    }

    @Test
    void literalPrefixGivesOffset() {
        var dfa = DFA.createDFA("abc*");
        var optOffset = dfa.calculateOffset();
        check(optOffset, 1, 'b', 'b');
    }

    @Test
    void offsetForCountedRepetitionWithNoOffset() {
        var dfa = DFA.createDFA("A{0,4}");
        var optOffset = dfa.calculateOffset();
        assertThat(optOffset).isEmpty();
    }

    @Test
    void offsetForCountedRepetitionWithNonZeroOffset() {
        var dfa = DFA.createDFA("A{3,4}");
        var optOffset = dfa.calculateOffset();
        check(optOffset, 2, 'A', 'A');
    }

    @Test
    void chainForCountedRepetition() {
        var dfa = DFA.createDFA("A{3,4}");
        var chain = dfa.chain();
        assertEquals(2, chain.size());
    }

    @Test
    void chainForAlternation() {
        var dfa = DFA.createDFA("(A|B)");
        var chain = dfa.chain();
        assertEquals(0, chain.size());
    }

    @Test
    void chainLiteralFollowedByAlternation() {
        var dfa = DFA.createDFA("AB(A|B)");
        var chain = dfa.chain();
        assertEquals(2, chain.size());
    }

    @Test
    void chainLiteralFollowedByRepetition() {
        var dfa = DFA.createDFA("ABa*");
        var chain = dfa.chain();
        assertEquals(1, chain.size());
    }

    @Test
    void chainLiteralFollowedByRepetitionOfAlternation() {
        var dfa = DFA.createDFA("AB(a|c)*");
        var chain = dfa.chain();
        assertEquals(1, chain.size());
    }

    private void check(Optional<Offset> optOffset, int i, char start, char end) {
        assertTrue(optOffset.map(o -> {
            assertEquals(i, o.length);
            assertEquals(new CharRange(start, end), o.charRange);
            return true;
        }).orElse(false));
    }

    @Test
    void offsetsDiamondWithTail() {
        String regexString = "ab(cd|ef)ghi";
        var dfa = DFA.createDFA(regexString);
        var offsets = dfa.calculateOffsets(RegexParser.parse(regexString, 0).bestFactors());
        // offsets before 'a', 'c', 'e'
        assertEquals(3, offsets.size());
    }

    @Test
    void offsetsDiamond() {
        String regexString = "[a-q][xX]";
        var dfa = DFA.createDFA(regexString);
        var offsets = dfa.calculateOffsets(RegexParser.parse(regexString).bestFactors());
        assertEquals(0, offsets.size());
    }

    @Test
    void offsetsFakeFork() {
        String regexString = "(a|c)efg";
        var dfa = DFA.createDFA(regexString);
        var offsets = dfa.calculateOffsets(RegexParser.parse(regexString).bestFactors());
        assertEquals(1, offsets.size());
    }

    @Test
    void offsetsFork() {
        String regexString = "(ab|cd)efg";
        var dfa = DFA.createDFA(regexString);
        var offsets = dfa.calculateOffsets(RegexParser.parse(regexString).bestFactors());
        assertEquals(2, offsets.size());
    }

    @Test
    void byteClassesLiteral() {
        var dfa = DFA.createDFA("abc");
        var byteClasses = dfa.byteClasses().ranges;
        for (var i = 0; i < 'a'; i++) {
            assertEquals(0, byteClasses[i]);
        }
        assertEquals(1, byteClasses['a']);
        assertEquals(2, byteClasses['b']);
        assertEquals(3, byteClasses['c']);
        for (var i = 'd'; i <= DFA.MAX_CHAR_FOR_BYTECLASSES; i++) {
            assertEquals(0, byteClasses[i]);
        }
    }

    @Test
    void byteClassesTwoDisconnectedRangesFollowedByLiteral() {
        var dfa = DFA.createDFA("[A-Za-z]+ab");
        var byteClasses = dfa.byteClasses().ranges;
        for (var i = 0; i < 'A'; i++) {
            assertEquals(0, byteClasses[i]);
        }
        assertEquals(1, byteClasses['A']);
        assertEquals(1, byteClasses['Z']);
        assertEquals(2, byteClasses['a']);
        assertEquals(3, byteClasses['b']);
        for (var c = 'c'; c <= 'z'; c++) {
            assertEquals(1, byteClasses[c]);
        }
        for (var c = 'z' + 1; c < DFA.MAX_CHAR_FOR_BYTECLASSES; c++) {
            assertEquals(0, byteClasses[c]);
        }
    }

    @Test
    void byteClassesCanBeFormedFromRegexWithDot() {
        var dfa = DFA.createDFA("[A-Za-z]+.b", ConversionMode.BASIC, Pattern.DOTALL);
        var byteClasses = dfa.byteClasses().ranges;
        for (var i = 0; i < 'A'; i++) {
            assertEquals(1, byteClasses[i]);
        }
        assertEquals(2, byteClasses['A']);
        assertEquals(2, byteClasses['Z']);
        assertEquals(2, byteClasses['a']);
        assertEquals(3, byteClasses['b']);
        for (var c = 'c'; c <= 'z'; c++) {
            assertEquals(2, byteClasses[c]);
        }
        for (var c = 'z' + 1; c < DFA.MAX_CHAR_FOR_BYTECLASSES; c++) {
            assertEquals(1, byteClasses[c]);
        }
    }

    @Test
    void newTest() {
        var dfa = DFA.createDFA("http://.+", ConversionMode.BASIC, Pattern.DOTALL);
        var byteClasses = dfa.byteClasses().ranges;
        for (var i = 0; i < '/'; i++) {
            assertEquals(1, byteClasses[i]);
        }
        assertEquals(2, byteClasses['/']);
        for (var i = '0'; i < ':'; i++) {
            assertEquals(1, byteClasses[i]);
        }
        assertEquals(3, byteClasses[':']);
    }

    @Test
    void distinctCharRangesCanBeFormedFromRegexWithDot() {
        var dfa = DFA.createDFA("[A-Za-z]+.b", ConversionMode.BASIC, Pattern.DOTALL);
        var distinguishedRanges = dfa.getDistinctCharRanges(dfa.getSortedTransitions());
        assertThat(distinguishedRanges).isNotEmpty();
        assertEquals(new CharRange('\u0000', '@'), distinguishedRanges.get(0));
        assertEquals(new CharRange('A', 'Z'), distinguishedRanges.get(1));
        assertEquals(new CharRange('[', '`'), distinguishedRanges.get(2));
        assertEquals(new CharRange('a', 'a'), distinguishedRanges.get(3));
        assertEquals(new CharRange('b', 'b'), distinguishedRanges.get(4));
        assertEquals(new CharRange('c', 'z'), distinguishedRanges.get(5));
    }

    @Test
    void distinctCharRangesWithCharRangeSplitMultipleTimes() {
        // the [a-z] range gets subdivided into [a-f][g][h][i][j-m][n][o-z]
        var dfa = DFA.createDFA("[A-Za-z]+ing");
        var distinguishedRanges = dfa.getDistinctCharRanges(dfa.getSortedTransitions());
        assertEquals(new CharRange('A', 'Z'), distinguishedRanges.get(0));
    }

    @Test
    void charRangesForRegexUsing_DOTALL() {
        var dfa = DFA.createDFA("[A-Za-z]+.b", ConversionMode.BASIC, Pattern.DOTALL);
        var rangeGroups = dfa.generateRangeGroups();
        var rangeGroup = rangeGroups.get(0);
        assertEquals(new CharRange('\u0000', '@'), rangeGroup.ranges.get(0));
        assertEquals(new CharRange('[', '`'), rangeGroup.ranges.get(1));
        assertEquals(new CharRange('{', '\uFFFF'), rangeGroup.ranges.get(2));

        rangeGroup = rangeGroups.get(1);
        assertEquals(new CharRange('A', 'Z'), rangeGroup.ranges.get(0));
        assertEquals(new CharRange('a', 'a'), rangeGroup.ranges.get(1));
        assertEquals(new CharRange('c', 'z'), rangeGroup.ranges.get(2));

        rangeGroup = rangeGroups.get(2);
        assertEquals(new CharRange('b', 'b'), rangeGroup.ranges.get(0));

    }

    @Test
    void charRangesForOtherRegexWithDot() {
        var dfa = DFA.createDFA("h:.+", ConversionMode.BASIC, Pattern.DOTALL);
        var charRanges = dfa.getDistinctCharRanges(dfa.getSortedTransitions());
        assertEquals(new CharRange('\u0000', '9'), charRanges.get(0));
        assertEquals(new CharRange(':', ':'), charRanges.get(1));
        assertEquals(new CharRange(';', 'g'), charRanges.get(2));
        assertEquals(new CharRange('h', 'h'), charRanges.get(3));
        assertEquals(new CharRange('i', '\uFFFF'), charRanges.get(4));
    }

    @Test
    void againByteClasses() {
        var dfa = DFA.createDFA("h:.+");
        var byteClasses = dfa.byteClasses();
        assertEquals(1, byteClasses.ranges[0]);
        assertEquals(2, byteClasses.ranges[':']);
        assertEquals(1, byteClasses.ranges[';']);
        assertEquals(3, byteClasses.ranges['h']);
        assertEquals(1, byteClasses.ranges['i']);
    }

    @Test
    void yetAnotherRanges() {
        var dfa = DFA.createDFA("Hol.{0,2}Wat|Wat.{0,2}Hol", ConversionMode.BASIC, Pattern.DOTALL);
        List<CharRange> distinctRanges = dfa.getDistinctCharRanges(dfa.getSortedTransitions());
        List<CharRange> expectedRanges = List.of(
                new CharRange('\u0000', 'G'),
                new CharRange('H', 'H'),
                new CharRange('I', 'V'),
                new CharRange('W', 'W'),
                new CharRange('X', '`'),
                new CharRange('a', 'a'),
                new CharRange('b', 'k'),
                new CharRange('l', 'l'),
                new CharRange('m', 'n'),
                new CharRange('o', 'o'),
                new CharRange('p', 's'),
                new CharRange('t', 't'),
                new CharRange('u', '\uFFFF'));
        assertEquals(expectedRanges, distinctRanges);
    }

    @Test
    void evenMore() {
        var dfa = DFA.createDFA("Hol.{0,2}Wat|Wat.{0,2}Hol");
        var byteClasses = dfa.byteClasses();
        assertEquals(1, byteClasses.ranges[0]);
        assertEquals(2, byteClasses.ranges['H']);
        assertEquals(1, byteClasses.ranges['I']);
        assertEquals(3, byteClasses.ranges['W']);
        assertEquals(1, byteClasses.ranges['X']);
        assertEquals(4, byteClasses.ranges['a']);
        assertEquals(1, byteClasses.ranges['b']);
        assertEquals(5, byteClasses.ranges['l']);
        assertEquals(1, byteClasses.ranges['m']);
        assertEquals(6, byteClasses.ranges['o']);
        assertEquals(1, byteClasses.ranges['p']);
        assertEquals(7, byteClasses.ranges['t']);
    }


    @Test
    void initialBytes() {
        var dfa = DFA.createDFA("Sherlock|Holmes|Adler|Watson");
        boolean[] initialBytes = dfa.initialAsciiBytes().get();
        for (int i = 0; i < initialBytes.length; i++) {
            DFA target = dfa.transition((char) i);
            if (initialBytes[i]) {
                assertNotEquals(0, target.getStateNumber());
            }
            else {
                assertNull(target);
            }
        }
    }
}
