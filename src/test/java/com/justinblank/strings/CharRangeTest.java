package com.justinblank.strings;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class CharRangeTest {

    @Test
    public void testMinimizeOneRange() {
        List<CharRange> ranges = new ArrayList<>();
        ranges.add(new CharRange('a', 'e'));

        List<CharRange> minimized = CharRange.minimalCovering(ranges);
        assertEquals(minimized.get(0), new CharRange('a', 'e'));
    }

    @Test
    public void testMinimizeTwoRanges() {
        List<CharRange> ranges = new ArrayList<>();
        ranges.add(new CharRange('a', 'e'));
        ranges.add(new CharRange('b', 'd'));

        List<CharRange> minimized = CharRange.minimalCovering(ranges);
        assertEquals(minimized.get(0), new CharRange('a', 'a'));
        assertEquals(minimized.get(1), new CharRange('b', 'd'));
        assertEquals(minimized.get(2), new CharRange('e', 'e'));
    }

    @Test
    public void testMinimizeMultipleRanges() {
        List<CharRange> ranges = new ArrayList<>();
        ranges.add(new CharRange('a', 'e'));
        ranges.add(new CharRange('b', 'd'));
        ranges.add(new CharRange('f', 'h'));

        List<CharRange> minimized = CharRange.minimalCovering(ranges);
        assertEquals(minimized.get(0), new CharRange('a', 'a'));
        assertEquals(minimized.get(1), new CharRange('b', 'd'));
        assertEquals(minimized.get(2), new CharRange('e', 'e'));
        assertEquals(minimized.get(3), new CharRange('f', 'h'));

    }

    @Test
    public void testThreeRangesWithOneCharRangeContainedInOtherRange() {
        var ranges = List.of(new CharRange('T', '_'), new CharRange('b', 'e'), new CharRange('e', 'e'));
        var minimized = CharRange.minimalCovering(ranges);
        assertEquals(new CharRange('T', '_'), minimized.get(0));
        assertEquals(new CharRange('b', 'd'), minimized.get(1));
        assertEquals(new CharRange('e', 'e'), minimized.get(2));
    }
}
