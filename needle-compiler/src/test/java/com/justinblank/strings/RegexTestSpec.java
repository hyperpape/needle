package com.justinblank.strings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class RegexTestSpec {

    final String pattern;
    final String target;
    final boolean successful;
    final int start;
    final int end;

    public RegexTestSpec(String pattern, String target, boolean successful, int start, int end) {
        this.pattern = pattern;
        this.target = target;
        this.successful = successful;
        this.start = start;
        this.end = end;
    }
}
