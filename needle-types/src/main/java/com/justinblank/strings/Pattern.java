package com.justinblank.strings;

public interface Pattern {

    /**
     * When passed, a '.' character in the regex will match the newline character. Otherwise, the '.' matches all
     * characters except '\n'.
     */
    int DOTALL = 1;

    Matcher matcher(String s);
}
