package com.justinblank.strings;

import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class CaptureTestSpec {

    final String pattern;
    final String target;
    final int start;
    final int end;
    final List<String> captures;
    final RegexTestSpec.Flags flags;

    public CaptureTestSpec(String pattern, String target, int start, int end, List<String> captures, RegexTestSpec.Flags flags) {
        this.pattern = pattern;
        this.target = target;
        this.start = start;
        this.end = end;
        this.captures = captures;
        this.flags = flags;
    }
}
