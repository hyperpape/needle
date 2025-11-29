package com.justinblank.strings;

public interface Pattern {

    /**
     * When passed, a '.' character in the regex will match the newline character. Otherwise, the '.' matches all
     * characters except '\n'.
     */
    int DOTALL = 0x01;

    /**
     * Enabled case insensitive matching for ASCII characters.
     */
    int CASE_INSENSITIVE = 0x02;

    int ALL_FLAGS = DOTALL | CASE_INSENSITIVE;

    Matcher matcher(String s);
}
