package com.justinblank.strings;

public interface Matcher {

    boolean matches();

    // TODO: this is a really weird interface
    boolean containedIn();

    MatchResult find();

    MatchResult find(int start, int end);
}
