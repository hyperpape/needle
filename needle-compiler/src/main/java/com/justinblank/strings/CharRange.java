package com.justinblank.strings;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class CharRange implements Comparable<CharRange> {
    private final char start;
    private final char end;
    private final boolean empty;

    private static final CharRange ALL_CHARS = new CharRange(Character.MIN_VALUE, Character.MAX_VALUE);

    public CharRange(char start, char end) {
        this.start = start;
        this.end = end;
        if (this.start > this.end) {
            throw new IllegalArgumentException("Tried to create a character range with start=" + start + " larger than end=" + end);
        }
        empty = false;
    }

    public static CharRange allChars() {
        return ALL_CHARS;
    }

    public char getStart() {
        return start;
    }

    public char getEnd() {
        return end;
    }

    public boolean isEmpty() {
        return empty;
    }

    public boolean inRange(char c) {
        return c >= start && c <= end;
    }

    public boolean isSingleCharRange() {
        return start == end;
    }

    public boolean acceptsAllChars() {
        return start == '\u0000' && end == '\uFFFF';
    }

    public boolean treatsAllNonAsciiIdentically() {
        return start <= 127 && end == '\uFFFF';
    }

    @Override
    public String toString() {
        return "CharRange{" +
                "start=" + start +
                ", end=" + end +
                ", empty=" + empty +
                '}';
    }

    /**
     * Given a sorted set of non-overlapping char ranges, expand it to cover all possible characters.
     * @param ranges
     * @return a list of char ranges containing every character
     */
    static List<CharRange> coverAllChars(List<CharRange> ranges) {
        List<CharRange> allChars = new ArrayList<>();
        if (ranges.isEmpty()) {
            allChars.add(ALL_CHARS);
            return allChars;
        }
        CharRange current = null;
        for (var charRange : ranges) {
            if (current == null) {
                current = charRange;
                if (current.getStart() > Character.MIN_VALUE) {
                    allChars.add(new CharRange(Character.MIN_VALUE, decr(current.getStart())));
                }
                allChars.add(current);
            }
            else {
                if (charRange.getStart() > incr(current.getEnd())) {
                    allChars.add(new CharRange(incr(current.getEnd()), decr(charRange.getStart())));
                }
                allChars.add(charRange);
                current = charRange;
            }
        }
        if (current.getEnd() < Character.MAX_VALUE) {
            allChars.add(new CharRange(incr(current.getEnd()), Character.MAX_VALUE));
        }
        return allChars;
    }

    /**
     * Find a minimal covering of a list of char ranges, subject to the constraint that if X and Y are character ranges,
     * and X contains character 'c' and Y does not, then some CharacterRange in the covering includes 'c' and does
     * not intersect Y. // TODO: verify that makes sense
     * @param ranges a list of character ranges
     * @return a minimized list of character ranges
     */
    static List<CharRange> minimalCovering(List<CharRange> ranges) {
        List<CharRange> minimized = new ArrayList<>();
        ranges = new ArrayList<>(ranges);
        ranges.sort(Comparator.comparingInt(CharRange::getStart));
        // ranges = withoutDominatedRanges(ranges);
        if (ranges.size() < 2) {
            return ranges;
        }

        int lastStart = -1;
        int lastEnd = -1;
        for (int i = 0; i < ranges.size(); i++) {
            CharRange current = ranges.get(i);
            while (lastEnd < current.getEnd()) {
                char start = current.getStart();
                char end = current.getEnd();
                if (lastStart >= start) {
                    start = (char) (lastStart + 1);
                }
                if (lastEnd >= start) {
                    start = (char) (lastEnd + 1);
                }
                for (int j = i + 1; j < ranges.size(); j++) {
                    CharRange next = ranges.get(j);
                    if (next.getStart() > start && next.getStart() <= end) {
                        end = (char) (next.getStart() - 1);
                    }
                    if (next.getEnd() >= start && next.getEnd() <= end) {
                        end = next.getEnd();
                    }
                }

                lastStart = start;
                lastEnd = end;
                minimized.add(new CharRange(start, end));
            }
        }

        minimized.sort(Comparator.comparingInt(CharRange::getStart));
        assert checkRep(minimized);
        return minimized;
    }

    public boolean overlaps(CharRange range) {
        return getStart() <= range.getEnd() && getEnd() >= range.getStart();
    }

    static boolean checkRep(List<CharRange> ranges) {
        for (int i = 0; i < ranges.size() - 1; i++) {
            var range1 = ranges.get(i);
            var range2 = ranges.get(i + 1);
            if (range2.getStart() <= range1.getStart()) {
                return false;
            }
            else if (range2.getStart() <= range1.getEnd()) {
                return false;
            }
        }
        return true;
    }

    // Note that this assumes non-overlapping ranges
    public static List<CharRange> compact(List<CharRange> charRanges) {
        charRanges.sort(Comparator.comparingInt(CharRange::getStart));
        List<CharRange> ranges = new ArrayList<>();
        CharRange current = charRanges.get(0);
        for (int i = 1; i < charRanges.size(); i++) {
            CharRange next = charRanges.get(i);
            if (incr(current.getEnd()) == next.getStart()) {
                current = new CharRange(current.getStart(), next.getEnd());
            }
            else {
                ranges.add(current);
                current = next;
            }
        }
        ranges.add(current);
        return ranges;
    }

    private static char incr(char c) {
        return (char) (((int) c) + 1);
    }

    private static char decr(char c) {
        return (char) (((int) c) -1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CharRange charRange = (CharRange) o;
        return start == charRange.start &&
                end == charRange.end;
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }

    @Override
    public int compareTo(CharRange o) {
        if (isEmpty()) {
            return o.isEmpty() ? 0 : -1;
        }
        else if (o.isEmpty()) {
            return 1;
        }
        int cmp = start - o.start;
        if (cmp == 0) {
            cmp = end - o.end;
        }
        return cmp;
    }
}
