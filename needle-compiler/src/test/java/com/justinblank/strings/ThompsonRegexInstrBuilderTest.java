package com.justinblank.strings;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ThompsonRegexInstrBuilderTest {

    @Test
    public void testCompilesRange() {
        assertNotNull(RegexInstrBuilder.createNFA(RegexParser.parse("[A-Z]")));
    }

    @Test
    public void testCompilesMultiRange() {
        assertNotNull(RegexInstrBuilder.createNFA(RegexParser.parse("[A-Za-z]")));
    }

    @Test
    public void testCompilesUnions() {
        assertNotNull(RegexInstrBuilder.createNFA(RegexParser.parse("(123)|(234){0,1}")));
    }

    @Test
    public void testPrioritiesUnionOfLiterals() {
        var instrs = RegexInstrBuilder.createNFA(RegexParser.parse("Sam|Samwise"));
        assertEquals(1, instrs[0].priority); // split instr
        assertEquals(1, instrs[1].priority); // S in first alternation
        assertEquals(2, instrs[5].priority); // S in second alternation
    }

    @Test
    public void testPrioritiesAplusA() {
        var instrs = RegexInstrBuilder.createNFA(RegexParser.parse("a+a"));
        assertEquals(1, instrs[0].priority);

    }
}
