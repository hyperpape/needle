package com.justinblank.strings;

public class MatchResult {

    public final boolean matched;
    public final int start;
    public final int end;

    MatchResult(boolean matched, int start, int end) {
        this.matched = matched;
        if (matched) {
            this.start = start;
            this.end = end;
        } else {
            // try to break fast if we access meaningless values...
            // TODO: maybe accessors with exception
            this.start = -1000;
            this.end = -1000;
        }
    }
}
