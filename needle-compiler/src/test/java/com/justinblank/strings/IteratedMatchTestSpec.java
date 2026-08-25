package com.justinblank.strings;

import java.util.List;
import java.util.Objects;

/**
 * A spec for iterated matching: the sequence of non-overlapping matches that repeated calls to
 * Matcher#find() should return for a pattern against a haystack.
 */
public class IteratedMatchTestSpec {

    final String pattern;
    final String target;
    final List<Match> matches;
    final RegexTestSpec.Flags flags;

    public IteratedMatchTestSpec(String pattern, String target, List<Match> matches, RegexTestSpec.Flags flags) {
        this.pattern = pattern;
        this.target = target;
        this.matches = matches;
        this.flags = flags;
    }

    public static class Match {

        final int start;
        final int end;

        public Match(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Match match = (Match) o;
            return start == match.start && end == match.end;
        }

        @Override
        public int hashCode() {
            return Objects.hash(start, end);
        }

        @Override
        public String toString() {
            return "(" + start + "," + end + ")";
        }
    }
}
