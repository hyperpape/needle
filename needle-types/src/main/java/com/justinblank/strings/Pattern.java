package com.justinblank.strings;

public interface Pattern {

    /**
     * When passed, a '.' character in the regex will match the newline character. Otherwise, the '.' matches all
     * characters except '\n'.
     */
    int DOTALL = java.util.regex.Pattern.DOTALL;

    /**
     * Enabled case insensitive matching for ASCII characters.
     */
    int CASE_INSENSITIVE = java.util.regex.Pattern.CASE_INSENSITIVE;

    int ALL_FLAGS = DOTALL | CASE_INSENSITIVE;

    Matcher matcher(String s);
}
