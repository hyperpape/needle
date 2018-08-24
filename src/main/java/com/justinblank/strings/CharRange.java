package com.justinblank.strings;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class CharRange {
    private final char start;
    private final char end;
    private final boolean empty;

    public static CharRange emptyRange() {
        return new CharRange();
    }

    public CharRange(char start, char end) {
        this.start = start;
        this.end = end;
        empty = false;
    }

    private CharRange() {
        start = '0';
        end = '0';
        empty = true;
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

    public boolean contains(CharRange charRange) {
        return getStart() <= charRange.getStart() && charRange.getEnd() <= getEnd();
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
     * Find a minimal covering of a list of char ranges, subject to the constraint that if X and Y are character ranges,
     * and X contains character 'c' and Y does not, then some CharacterRange in the covering includes 'c' and does
     * not intersect Y. // TODO: verify that makes sense
     * @param ranges a list of character ranges
     * @return a minimized list of character ranges
     */
    public static List<CharRange> minimize(List<CharRange> ranges) {
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
                    if (next.getStart() > start && next.getStart() < end) {
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
        return minimized;
    }

    private static List<CharRange> withoutDominatedRanges(List<CharRange> ranges) {
        List<CharRange> simplified = new ArrayList<>();
        if (!ranges.isEmpty()) {
            simplified.add(ranges.get(0));
        }
        for (int i = 0; i < ranges.size() - 1; i++) {
            CharRange current = ranges.get(i);
            CharRange next = ranges.get(i + 1);
            if (!current.contains(next)) {
                simplified.add(next);
            }
        }

        return simplified;
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
}
