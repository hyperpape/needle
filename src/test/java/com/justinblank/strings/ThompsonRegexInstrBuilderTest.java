package com.justinblank.strings;

import org.junit.Test;

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
}
