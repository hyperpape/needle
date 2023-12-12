package com.justinblank.strings;

import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

public class DFATest {

    @Test
    public void testAfterSingleCharLiteralRegex() {
        var dfa = DFA.createDFA("a");
        assertThat(dfa.after("a")).isPresent();
        assertThat(dfa.after("b")).isEmpty();
    }

    @Test
    public void testAfterMultiCharLiteralRegex() {
        var dfa = DFA.createDFA("abc");
        assertThat(dfa.after("a")).isPresent();
        assertThat(dfa.after("ab")).isPresent();
        assertThat(dfa.after("abc")).isPresent();
        assertThat(dfa.after("b")).isEmpty();
    }



    @Test
    public void testAfter() {
        var dfa = DFA.createDFA("ab*[0-9]c");
        assertThat(dfa.after("a")).isPresent();
        assertThat(dfa.after("ab")).isPresent();
        assertThat(dfa.after("abbbb4c")).isPresent();
        assertThat(dfa.after("b")).isEmpty();
    }

    @Test
    public void testIsTerminal() {
        var dfa = DFA.createDFA("abc");
        assertFalse(dfa.isTerminal());
        assertFalse(dfa.after("a").get().isTerminal());
        assertFalse(dfa.after("ab").get().isTerminal());
        assertTrue(dfa.after("abc").get().isTerminal());
    }

    @Test
    public void testCalculateOffsetRepetition() {
        var dfa = DFA.createDFA("b*c");
        assertThat(dfa.calculateOffset()).isEmpty();
    }

    @Test
    public void testCalculateOffsetLiteral() {
        var dfa = DFA.createDFA("abc");
        var offset = dfa.calculateOffset();
        check(offset, 2, 'c', 'c');
    }

    @Test
    public void testCalculateOffsetLiteralFollowedByRange() {
        var dfa = DFA.createDFA("ab[c-d]");
        var optOffset = dfa.calculateOffset();
        check(optOffset, 2, 'c', 'd');
    }

    @Test
    public void testCalculateOffsetWithInternalCharRange() {
        var dfa = DFA.createDFA("a[0-9]c");
        var optOffset = dfa.calculateOffset();
        check(optOffset, 2, 'c', 'c');
    }

    @Test
    public void testCalculateOffsetWithInternalUnionWithSameChild() {
        var dfa = DFA.createDFA("a([0-9]|b)c");
        var optOffset = dfa.calculateOffset();
        check(optOffset, 2, 'c', 'c');
    }

    @Test
    public void testCalculateOffsetWithInternalUnionWithDifferentChild() {
        var dfa = DFA.createDFA("a(([0-9]c)|([a-z]d))");
        var optOffset = dfa.calculateOffset();
        assertThat(optOffset).isEmpty();
    }

    @Test
    public void testLiteralPrefixGivesOffset() {
        var dfa = DFA.createDFA("abc*");
        var optOffset = dfa.calculateOffset();
        check(optOffset, 1, 'b', 'b');
    }

    @Test
    public void testOffsetForCountedRepetitionWithNoOffset() {
        var dfa = DFA.createDFA("A{0,4}");
        var optOffset = dfa.calculateOffset();
        assertThat(optOffset).isEmpty();
    }

    @Test
    public void testOffsetForCountedRepetitionWithNonZeroOffset() {
        var dfa = DFA.createDFA("A{3,4}");
        var optOffset = dfa.calculateOffset();
        check(optOffset, 2, 'A', 'A');
    }

    @Test
    public void testChainForCountedRepetition() {
        var dfa = DFA.createDFA("A{3,4}");
        var chain = dfa.chain();
        assertEquals(2, chain.size());
    }

    @Test
    public void testChainForAlternation() {
        var dfa = DFA.createDFA("(A|B)");
        var chain = dfa.chain();
        assertEquals(0, chain.size());
    }

    @Test
    public void testChainLiteralFollowedByAlternation() {
        var dfa = DFA.createDFA("AB(A|B)");
        var chain = dfa.chain();
        assertEquals(2, chain.size());
    }

    @Test
    public void testChainLiteralFollowedByRepetition() {
        var dfa = DFA.createDFA("ABa*");
        var chain = dfa.chain();
        assertEquals(1, chain.size());
    }

    @Test
    public void testChainLiteralFollowedByRepetitionOfAlternation() {
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
    public void testOffsetsDiamond() {
        String regexString = "ab(cd|ef)ghi";
        var dfa = DFA.createDFA(regexString);
        var offsets = dfa.calculateOffsets(RegexParser.parse(regexString).bestFactors());
        // offsets before 'a', 'c', 'e'
        assertEquals(3, offsets.size());
    }

    @Test
    public void testOffsetsFakeFork() {
        String regexString = "(a|c)efg";
        var dfa = DFA.createDFA(regexString);
        var offsets = dfa.calculateOffsets(RegexParser.parse(regexString).bestFactors());
        assertEquals(1, offsets.size());
    }

    @Test
    public void testOffsetsFork() {
        String regexString = "(ab|cd)efg";
        var dfa = DFA.createDFA(regexString);
        var offsets = dfa.calculateOffsets(RegexParser.parse(regexString).bestFactors());
        assertEquals(2, offsets.size());
    }

    @Test
    public void testByteClassesLiteral() {
        var dfa = DFA.createDFA("abc");
        var byteClasses = dfa.byteClasses().ranges;
        for (var i = 0; i < 'a'; i++) {
            assertEquals(0, byteClasses[i]);
        }
        assertEquals(1, byteClasses['a']);
        assertEquals(2, byteClasses['b']);
        assertEquals(3, byteClasses['c']);
        for (var c = 'd'; c <= 128; c++) {
            assertEquals(0, byteClasses[c]);
        }
    }

    @Test
    public void testByteClassesTwoDisconnectedRangesFollowedByLiteral() {
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
        for (var c = 'z' + 1; c < 128; c++) {
            assertEquals(0, byteClasses[c]);
        }
    }

    @Test
    public void testByteClasses_canBeFormed_fromRegexWithDot() {
        var dfa = DFA.createDFA("[A-Za-z]+.b");
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
        for (var c = 'z' + 1; c < 128; c++) {
            assertEquals(1, byteClasses[c]);
        }
    }

    @Test
    public void newTest() {
        var dfa = DFA.createDFA("http://.+");
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
    public void testDistinctCharRanges_canBeFormed_fromRegexWithDot() {
        var dfa = DFA.createDFA("[A-Za-z]+.b");
        var distinguishedRanges = dfa.getDistinctCharRanges(dfa.getSortedTransitions());
        assertThat(distinguishedRanges).isNotEmpty();
        assertEquals(new CharRange('\u0000', '@'), distinguishedRanges.get(0));
    }

    @Test
    public void testDistinctCharRanges_withCharRangeSplitMultipleTimes() {
        // the [a-z] range gets subdivided into [a-f][g][h][i][j-m][n][o-z]
        var dfa = DFA.createDFA("[A-Za-z]+ing");
        var distinguishedRanges = dfa.getDistinctCharRanges(dfa.getSortedTransitions());
        assertEquals(new CharRange('A', 'Z'), distinguishedRanges.get(0));
    }

    @Test
    public void testCharRanges_forRegexWithDot() {
        // not isolating the behavior
        var dfa = DFA.createDFA("[A-Za-z]+.b");
        var rangeGroups = dfa.generateRangeGroups();
        var rangeGroup = rangeGroups.get(0);
        assertEquals(new CharRange('\u0000', '@'), rangeGroup.ranges.get(0));
        // TODO: fill out test case
    }

    @Test
    public void testCharRanges_forOtherRegexWithDot() {
        // not isolating the behavior
        var dfa = DFA.createDFA("h:.+");
        var rangeGroups = dfa.generateRangeGroups();
        var rangeGroup = rangeGroups.get(0);
        assertEquals(new CharRange('\u0000', '9'), rangeGroup.ranges.get(0));
        // TODO: fill out test case
    }

    @Test
    public void testAgainAgain() {
        var dfa = DFA.createDFA("h:.+");
        var charRanges = dfa.getDistinctCharRanges(dfa.getSortedTransitions());
        assertEquals(new CharRange('\u0000', '9'), charRanges.get(0));
        assertEquals(new CharRange(':', ':'), charRanges.get(1));
        assertEquals(new CharRange(';', 'g'), charRanges.get(2));
        assertEquals(new CharRange('h', 'h'), charRanges.get(3));
        assertEquals(new CharRange('i', '\uFFFF'), charRanges.get(4));
    }

    @Test
    public void testAgainByteClasses() {
        var dfa = DFA.createDFA("h:.+");
        var byteClasses = dfa.byteClasses();
        assertEquals(1, byteClasses.ranges[0]);
        assertEquals(2, byteClasses.ranges[':']);
        assertEquals(1, byteClasses.ranges[';']);
        assertEquals(3, byteClasses.ranges['h']);
        assertEquals(1, byteClasses.ranges['i']);
    }

    @Test
    public void testYetAnotherRanges() {
        var dfa = DFA.createDFA("Hol.{0,2}Wat|Wat.{0,2}Hol");
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
    public void testEvenMore() {
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
}
