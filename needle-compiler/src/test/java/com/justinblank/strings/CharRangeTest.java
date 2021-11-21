package com.justinblank.strings;

import org.junit.Test;
import org.quicktheories.QuickTheory;
import org.quicktheories.core.Gen;
import org.quicktheories.generators.CharactersDSL;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

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

    static Gen<CharRange> charRanges() {
        var gen = new CharactersDSL().basicMultilingualPlane();
        return gen.zip(gen, (a, b) -> {
            if (a < b) {
                return new CharRange(a, b);
            }
            else {
                return new CharRange(b, a);
            }
        });
    }

    @Test
    public void testOverlapIsReflexive() {
        QuickTheory.qt().forAll(charRanges()).check((a) -> a.overlaps(a));
    }

    @Test
    public void testOverlapIsSymmetric() {
        QuickTheory.qt().forAll(charRanges(), charRanges()).check((a, b) -> a.overlaps(b) == b.overlaps(a));
    }

    @Test
    public void testOverlap() {
        assertTrue(new CharRange('b', 'd').overlaps(new CharRange('b', 'b')));
        assertTrue(new CharRange('b', 'd').overlaps(new CharRange('c', 'c')));
        assertTrue(new CharRange('b', 'd').overlaps(new CharRange('d', 'd')));

        assertTrue(new CharRange('a', 'l').overlaps(new CharRange('b', 'e')));
        assertTrue(new CharRange('a', 'l').overlaps(new CharRange('e', 'j')));

        assertFalse(new CharRange('b', 'd').overlaps(new CharRange('e', 'f')));
    }
}
