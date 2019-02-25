package com.justinblank.strings.RegexAST;

import com.justinblank.strings.CharRange;

public class CharRangeNode extends Node {

    private final CharRange range;

    public CharRange range() {
        return range;
    }

    public CharRangeNode(CharRange range ) {
        this.range = range;
    }

    public CharRangeNode(char start, char end) {
        this.range = new CharRange(start, end);
    }

    protected int minLength() {
        return 1;
    }
}
