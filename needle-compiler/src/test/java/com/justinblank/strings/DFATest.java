package com.justinblank.strings;

import org.junit.Test;

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
        var byteClasses = dfa.byteClasses();
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
        var byteClasses = dfa.byteClasses();
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
    public void testDistinctCharRanges_canBeFormed_fromRegexWithDot() {
        var dfa = DFA.createDFA("[A-Za-z]+.b");
        var byteClasses = dfa.getDistinctCharRanges();
        assertThat(byteClasses).isNotEmpty();
    }
}
