package com.justinblank.strings.Search;

public interface SearchMethod {

    int findIndex(String s);

    default boolean containedIn(String s) {
        return findIndex(s) != -1;
    }
}
