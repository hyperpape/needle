package com.justinblank.strings;

public class RegexTestSpec {

    final String pattern;
    final String target;
    final boolean successful;
    final int start;
    final int end;
    final Flags flags;

    public RegexTestSpec(String pattern, String target, boolean successful, int start, int end, Flags flags) {
        this.pattern = pattern;
        this.target = target;
        this.successful = successful;
        this.start = start;
        this.end = end;
        this.flags = flags;
    }

    public static class Flags {
        int flags;

        public Flags(int flags) {
            this.flags = flags;
        }
    }
}
