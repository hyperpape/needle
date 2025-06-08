package com.justinblank.strings;

import org.junit.jupiter.api.Test;
import org.quicktheories.QuickTheory;
import org.quicktheories.core.Gen;
import org.quicktheories.generators.CharactersDSL;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CharRangeTest {

    @Test
    void minimizeOneRange() {
        List<CharRange> ranges = new ArrayList<>();
        ranges.add(new CharRange('a', 'e'));

        List<CharRange> minimized = CharRange.minimalCovering(ranges);
        assertEquals(minimized.get(0), new CharRange('a', 'e'));
    }

    @Test
    void minimizeTwoRanges() {
        List<CharRange> ranges = new ArrayList<>();
        ranges.add(new CharRange('a', 'e'));
        ranges.add(new CharRange('b', 'd'));

        List<CharRange> minimized = CharRange.minimalCovering(ranges);
        assertEquals(minimized.get(0), new CharRange('a', 'a'));
        assertEquals(minimized.get(1), new CharRange('b', 'd'));
        assertEquals(minimized.get(2), new CharRange('e', 'e'));
    }

    @Test
    void minimizeMultipleRanges() {
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
    void threeRangesWithOneCharRangeContainedInOtherRange() {
        var ranges = List.of(new CharRange('T', '_'), new CharRange('b', 'e'), new CharRange('e', 'e'));
        var minimized = CharRange.minimalCovering(ranges);
        assertEquals(new CharRange('T', '_'), minimized.get(0));
        assertEquals(new CharRange('b', 'd'), minimized.get(1));
        assertEquals(new CharRange('e', 'e'), minimized.get(2));
    }

    @Test
    void cover_all_chars_handles_empty_list() {
        var ranges = CharRange.coverAllChars(List.of());
        assertEquals(CharRange.allChars(), ranges.get(0));
    }

    @Test
    void cover_all_chars_handles_single_range_with_all_chars() {
        var ranges = CharRange.coverAllChars(List.of(CharRange.allChars()));
        assertEquals(CharRange.allChars(), ranges.get(0));
    }

    @Test
    void cover_all_chars_handles_single_range_with_char_subset() {
        var ranges = CharRange.coverAllChars(List.of(new CharRange('B', 'Y')));
        assertEquals(new CharRange(Character.MIN_VALUE, 'A'), ranges.get(0));
        assertEquals(new CharRange('B', 'Y'), ranges.get(1));
        assertEquals(new CharRange('Z', Character.MAX_VALUE), ranges.get(2));
    }

    @Test
    void cover_all_chars_handles_multiple_contiguous_ranges_with_char_subset() {
        var ranges = CharRange.coverAllChars(List.of(new CharRange('B', 'B'), new CharRange('C', 'E')));
        assertEquals(new CharRange(Character.MIN_VALUE, 'A'), ranges.get(0));
        assertEquals(new CharRange('B', 'B'), ranges.get(1));
        assertEquals(new CharRange('C', 'E'), ranges.get(2));
        assertEquals(new CharRange('F', Character.MAX_VALUE), ranges.get(3));
    }

    @Test
    void cover_all_chars_handles_multiple_ranges_with_gaps_and_contiguous_ranges() {
        var ranges = CharRange.coverAllChars(List.of(new CharRange('B', 'C'), new CharRange('F', 'H'), new CharRange('K', 'M'), new CharRange('P', 'S')));
        assertEquals(new CharRange(Character.MIN_VALUE, 'A'), ranges.get(0));
        assertEquals(new CharRange('B', 'C'), ranges.get(1));
        assertEquals(new CharRange('D', 'E'), ranges.get(2));
        assertEquals(new CharRange('F', 'H'), ranges.get(3));
        assertEquals(new CharRange('I', 'J'), ranges.get(4));
        assertEquals(new CharRange('K', 'M'), ranges.get(5));
        assertEquals(new CharRange('N', 'O'), ranges.get(6));
        assertEquals(new CharRange('P', 'S'), ranges.get(7));
        assertEquals(new CharRange('T', Character.MAX_VALUE), ranges.get(8));
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
    void overlapIsReflexive() {
        QuickTheory.qt().forAll(charRanges()).check((a) -> a.overlaps(a));
    }

    @Test
    void overlapIsSymmetric() {
        QuickTheory.qt().forAll(charRanges(), charRanges()).check((a, b) -> a.overlaps(b) == b.overlaps(a));
    }

    @Test
    void overlap() {
        assertTrue(new CharRange('b', 'd').overlaps(new CharRange('b', 'b')));
        assertTrue(new CharRange('b', 'd').overlaps(new CharRange('c', 'c')));
        assertTrue(new CharRange('b', 'd').overlaps(new CharRange('d', 'd')));

        assertTrue(new CharRange('a', 'l').overlaps(new CharRange('b', 'e')));
        assertTrue(new CharRange('a', 'l').overlaps(new CharRange('e', 'j')));

        assertFalse(new CharRange('b', 'd').overlaps(new CharRange('e', 'f')));
    }
}
