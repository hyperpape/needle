package com.justinblank.strings;

public interface Pattern {

    /**
     * When passed, a '.' character in the regex will match the newline character. Otherwise, the '.' matches all
     * characters except '\n'.
     */
    int DOTALL = java.util.regex.Pattern.DOTALL;

    /**
     * Enables case insensitive matching for ASCII characters.
     */
    int CASE_INSENSITIVE = java.util.regex.Pattern.CASE_INSENSITIVE;

    /**
     * Enables case insensitive matching for Unicode characters, if case insensitive matching is enabled. Does nothing by itself.
     */
    int UNICODE_CASE = java.util.regex.Pattern.UNICODE_CASE;


    /**
     * By default, matching (sam|samwise) against the string "samwise" will match "samwise" but with the leftmost first
     * flag, it will match "sam".
     *
     * The default behavior of not setting this flag (aka "leftmost longest") matches the JDK implementation.
     */
    int LEFTMOST_FIRST = 0x800000;

    int ALL_FLAGS = DOTALL | CASE_INSENSITIVE | UNICODE_CASE | LEFTMOST_FIRST;

    Matcher matcher(String s);
}
