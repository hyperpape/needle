package com.justinblank.strings;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MatchResultTest {

    @Test
    void compareTo() {
        assertEquals(0, MatchResult.failure().compareTo(MatchResult.failure()));
        assertEquals(0, MatchResult.success(0, 1).compareTo(MatchResult.success(0, 1)));
        assertEquals(-1, MatchResult.success(0, 1).compareTo(MatchResult.success(0, 2)));
        assertEquals(1, MatchResult.success(0, 2).compareTo(MatchResult.success(0, 1)));
        assertEquals(1, MatchResult.success(0, 1).compareTo(MatchResult.success(1, 2)));
        assertEquals(-1, MatchResult.success(1, 2).compareTo(MatchResult.success(0, 1)));
    }

    @Test
    void testHashCode() {
        Set<MatchResult> results = new HashSet<>();
        results.add(MatchResult.success(0, 1));
        results.add(MatchResult.success(0, 1));
        assertEquals(1, results.size());
        results.add(MatchResult.failure());
        results.add(MatchResult.failure());
        assertEquals(2, results.size());
    }
}
