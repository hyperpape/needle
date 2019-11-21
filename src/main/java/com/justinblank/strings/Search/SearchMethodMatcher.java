package com.justinblank.strings.Search;

import com.justinblank.strings.Matcher;
import com.justinblank.strings.Pattern;

import java.util.Objects;

public class SearchMethodMatcher implements Matcher {

    private final SearchMethod method;
    private final String s;

    SearchMethodMatcher(SearchMethod method, String s) {
        Objects.requireNonNull(method, s);
        this.method = method;
        this.s = s;
    }

    @Override
    public boolean matches() {
        return method.matches(s);
    }
}
