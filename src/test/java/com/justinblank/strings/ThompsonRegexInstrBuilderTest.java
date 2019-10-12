package com.justinblank.strings;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class ThompsonRegexInstrBuilderTest {

    @Test
    public void testCompilesRange() {
        assertNotNull(ThompsonRegexInstrBuilder.createNFA(RegexParser.parse("[A-Z]")));
    }

    @Test
    public void testCompilesMultiRange() {
        assertNotNull(ThompsonRegexInstrBuilder.createNFA(RegexParser.parse("[A-Za-z]")));
    }
}
