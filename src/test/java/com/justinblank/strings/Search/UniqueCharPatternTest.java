package com.justinblank.strings.Search;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.SourceDSL.strings;

public class UniqueCharPatternTest {

    @Test
    public void testEqualToStringIndexOf() {
        UniqueCharPattern pattern = new UniqueCharPattern("abcdef");
        String s = "efabcdefghi";
        assertEquals(pattern.findIndex(s), 2);
    }

    @Test
    public void testPrefixAlwaysSucceeds() {
        qt().forAll(strings().allPossible().ofLengthBetween(0, 50), strings().allPossible().ofLengthBetween(0, 50)).check((s1, s2) ->
        {
            String s1Unique = makeUniquePattern(s1);
            return new UniqueCharPattern(s1Unique).findIndex(s1Unique + s2) == 0;
        });
    }

    private String makeUniquePattern(String s1) {
        Set<Character> chars = new HashSet<>();
        for (char c : s1.toCharArray()) {
            chars.add(c);
        }
        List<Character> charList = new ArrayList<>(chars);
        Collections.shuffle(charList);
        StringBuilder sb = new StringBuilder();
        for (Character c : charList) {
            sb.append(c);
        }
        return sb.toString();
    }

    @Test
    public void testAgreesWithStringIndexOf() {
        qt().forAll(strings().allPossible().ofLengthBetween(0, 50), strings().allPossible().ofLengthBetween(1, 50)).
                check((s1, s2) -> {
                    String s1Unique = makeUniquePattern(s1);
                    return new UniqueCharPattern(s1Unique).findIndex(s2) == s2.indexOf(s1Unique);
                });
    }

    @Test
    public void allNonEmptyPatternsFailEmptyString() {
        qt().forAll(strings().allPossible().ofLengthBetween(1, 50)).
                check(s -> {
                    String sUnique = makeUniquePattern(s);
                    return new UniqueCharPattern(sUnique).findIndex("") == -1;
                });
    }
}
