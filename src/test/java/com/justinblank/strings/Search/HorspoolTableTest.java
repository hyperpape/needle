package com.justinblank.strings.Search;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.quicktheories.QuickTheory.qt;
import static org.junit.Assert.assertEquals;
import static org.quicktheories.generators.SourceDSL.integers;
import static org.quicktheories.generators.SourceDSL.strings;

public class HorspoolTableTest {

    private static HorspoolTable mkTable(String pattern) {
        Set<Character> chars = new HashSet<>();
        for (char c : pattern.toCharArray()) {
            chars.add(c);
        }
        return HorspoolTable.mkTable(pattern, chars);
    }

    @Test
    public void testEmptyPattern() {
        assertEquals(mkTable("").findIndex("abc"), "abc".indexOf(""));
    }

    @Test
    public void samplePattern() {
        String testString = "abcdefgabc";

        int result = mkTable(testString).findIndex("azzzzddddfff" + testString);
        assertEquals(result, 12);
    }

    @Test
    public void allPatternsMatchThemselvesWithAnySuffix() {
        qt().forAll(strings().allPossible().ofLengthBetween(0, 50), strings().allPossible().ofLengthBetween(0, 50)).check((s1, s2) ->
                mkTable(s1).findIndex(s1 + s2) == 0
        );
    }

    @Test
    public void noNonEmptyPatternMatchesItsOwnSubstring() {
        // pattern must not be the empty string
        qt().forAll(strings().allPossible().ofLengthBetween(1, 50), integers().between(1, 50)).
                check((s, l) -> {
                    l = Math.min(l, s.length() - 1);
                    return mkTable(s).findIndex(s.substring(0, l)) == -1;
                });
    }

    @Test
    public void allNonEmptyPatternsFailEmptyString() {
        qt().forAll(strings().allPossible().ofLengthBetween(1, 50)).
                check(s -> mkTable(s).findIndex("") == -1);
    }

    @Test
    public void testAgreesWithStringIndexOf() {
        qt().forAll(strings().allPossible().ofLengthBetween(0, 50), strings().allPossible().ofLengthBetween(1, 50)).
            check((s1, s2) ->
                mkTable(s2).findIndex(s1) == s1.indexOf(s2)
            );
    }
}
