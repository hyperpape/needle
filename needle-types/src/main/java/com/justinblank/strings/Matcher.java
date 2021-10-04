package com.justinblank.strings;

/**
 * An object construct
 */
public interface Matcher {

    /**
     * Determine whether a string is completely matched by a regex.
     *
     * Given the regex 'abc',
     * @return
     */
    boolean matches();

    // TODO: this is a really weird interface
    boolean containedIn();

    MatchResult find();

    MatchResult find(int start, int end);
}
