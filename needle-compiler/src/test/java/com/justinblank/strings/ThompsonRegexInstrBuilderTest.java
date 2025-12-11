package com.justinblank.strings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ThompsonRegexInstrBuilderTest {

    @Test
    void compilesRange() {
        assertNotNull(RegexInstrBuilder.createNFA(RegexParser.parse("[A-Z]")));
    }

    @Test
    void compilesMultiRange() {
        assertNotNull(RegexInstrBuilder.createNFA(RegexParser.parse("[A-Za-z]")));
    }

    @Test
    void compilesUnions() {
        assertNotNull(RegexInstrBuilder.createNFA(RegexParser.parse("(123)|(234){0,1}")));
    }

    @Test
    void prioritiesUnionOfLiterals() {
        var instrs = RegexInstrBuilder.createNFA(RegexParser.parse("Sam|Samwise"), false);
        assertEquals(1, instrs[0].priority); // split instr
        assertEquals(1, instrs[1].priority); // S in first alternation
        assertEquals(2, instrs[5].priority); // S in second alternation
    }

    @Test
    void prioritiesAplusA() {
        var instrs = RegexInstrBuilder.createNFA(RegexParser.parse("a+a"));
        assertEquals(1, instrs[0].priority);

    }
}
