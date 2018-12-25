package com.justinblank.strings.Search;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.SourceDSL.strings;

public class BNDMSearchTest {

    @Test
    public void testBNDM() {
        assertEquals(6, BNDMSearch.prepare("age").findIndex("disengage"));
    }

    @Test
    public void testAgreesWithStringIndexOf() {
        qt().forAll(strings().ascii().ofLengthBetween(0, 50), strings().ascii().ofLengthBetween(1, 50)).
                check((s1, s2) ->
                        BNDMSearch.prepare(s2).findIndex(s1) == s1.indexOf(s2)
                );
    }
}
