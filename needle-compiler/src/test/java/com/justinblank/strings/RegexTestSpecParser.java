package com.justinblank.strings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class RegexTestSpecParser {

    private int idx = 0;

    List<RegexTestSpec> readTests() throws Exception {
        var resource = this.getClass().getClassLoader().getResource("matches.txt");
        var text = Files.readAllLines(Path.of(resource.toURI()));
        return text.stream().filter(s -> !s.isBlank()).map(this::readSpec).collect(Collectors.toList());
    }

    private RegexTestSpec readSpec(String s) {
        idx = 0;
        var pattern = chomp(s);
        var target = chomp(s);
        var successful = "y".equals(chomp(s));
        var start = Integer.parseInt(chomp(s));
        var end = Integer.parseInt(chomp(s));
        return new RegexTestSpec(pattern, target, successful, start, end);
    }

    private String chomp(String s) {
        int start = idx;
        boolean seenChar = false;
        boolean inQuote = false;
        while (idx < s.length()) {
            var c = s.charAt(idx);
            if (c == ' ' && !inQuote) {
                if (seenChar) {
                    return s.substring(start, idx).trim();
                }
            }
            else if (c == '\'') {
                if (inQuote) {
                    var substring = s.substring(start, idx++).trim();
                    // remove beginning ' character.
                    return substring.substring(1);
                }
                else {
                    inQuote = true;
                }
            }
            else {
                seenChar = true;
            }
            idx++;
        }
        if (!seenChar) {
            throw new IllegalStateException("Tried to chomp but didn't see anything at idx=" + idx);
        }
        return s.substring(start, idx).trim();
    }

}
