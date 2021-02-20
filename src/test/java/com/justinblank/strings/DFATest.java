package com.justinblank.strings;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class DFATest {

    @Test
    public void testCalculateOffsetRepetition() {
        var dfa = DFA.createDFA("b*c");
        assertThat(dfa.calculateOffset()).isNotPresent();
    }

    @Test
    public void testCalculateOffsetLiteral() {
        var dfa = DFA.createDFA("abc");
        var optOffset = dfa.calculateOffset();
        assertEquals(Optional.of(Pair.of(2, 'c')), optOffset);
    }

    @Test
    public void testCalculateOffsetWithInternalCharRange() {
        var dfa = DFA.createDFA("a[0-9]c");
        var optOffset = dfa.calculateOffset();
        assertEquals(Optional.of(Pair.of(2, 'c')), optOffset);
    }

    @Test
    public void testCalculateOffsetWithInternalUnionWithSameChild() {
        var dfa = DFA.createDFA("a([0-9]|b)c");
        var optOffset = dfa.calculateOffset();
        assertEquals(Optional.of(Pair.of(2, 'c')), optOffset);
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
        assertEquals(Optional.of(Pair.of(1, 'b')), optOffset);
    }

}
