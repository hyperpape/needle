package com.justinblank.strings;

import java.util.Objects;

public class MatchResult implements Comparable<MatchResult> {

    public final boolean matched;
    public final int start;
    public final int end;

    protected static final MatchResult FAILURE = new MatchResult(false, 0, 0);

    MatchResult(boolean matched, int start, int end) {
        this.matched = matched;
        if (matched) {
            if (start < 0) {
                throw new IllegalArgumentException("Start of a match cannot be less than 0, Start=" + start);
            }
            else if (end < start) {
                throw new IllegalArgumentException("End cannot be less than start, Start=" + start + ", End=" + end);
            }

            this.start = start;
            this.end = end;
        } else {
            // try to break fast if we access meaningless values...
            // TODO: maybe accessors with exception
            this.start = -1000;
            this.end = -1000;
        }
    }

    public static MatchResult failure() {
        return FAILURE;
    }

    public static MatchResult success(int start, int end) {
        return new MatchResult(true, start, end);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MatchResult that = (MatchResult) o;
        return matched == that.matched &&
                start == that.start &&
                end == that.end;
    }

    @Override
    public int hashCode() {
        return Objects.hash(matched, start, end);
    }

    @Override
    public String toString() {
        return "MatchResult{" +
                "matched=" + matched +
                ", start=" + start +
                ", end=" + end +
                '}';
    }

    @Override
    public int compareTo(MatchResult result) {
        if (!result.matched) {
            return this.matched ? 1 : 0;
        }
        if (this.start < result.start) {
            return 1;
        }
        else if (this.start > result.start) {
            return -1;
        }
        if (this.end < result.end) {
            return -1;
        }
        else if (this.end > result.end) {
            return 1;
        }
        return 0;
    }
}
