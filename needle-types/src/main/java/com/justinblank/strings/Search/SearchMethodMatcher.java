package com.justinblank.strings.Search;

import com.justinblank.strings.MatchResult;
import com.justinblank.strings.Matcher;

import java.util.Objects;

public class SearchMethodMatcher implements Matcher {

    private final SearchMethod method;
    private final String s;
    private int start;
    private int end;

    public SearchMethodMatcher(SearchMethod method, String s) {
        Objects.requireNonNull(method, s);
        this.method = method;
        this.s = s;
    }

    @Override
    public boolean matches() {
        return method.matches(s);
    }

    @Override
    public boolean containedIn() {
        return method.containedIn(s);
    }

    @Override
    public boolean find() {
        MatchResult matchResult = method.find(s);
        if (matchResult.matched) {
            this.start = matchResult.start;
            this.end = matchResult.end;
        }
        else {
            this.start = -1;
            this.end = -1;
        }
        return matchResult.matched;
    }

    @Override
    public boolean find(int start, int end) {
        MatchResult matchResult = method.find(s, start, end);
        if (matchResult.matched) {
            this.start = matchResult.start;
            this.end = matchResult.end;
        }
        else {
            this.start = -1;
            this.end = -1;
        }
        return matchResult.matched;
    }

    @Override
    public int start() {
        return start;
    }

    @Override
    public int end() {
        return end;
    }
}
