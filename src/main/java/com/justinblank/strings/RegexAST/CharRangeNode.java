package com.justinblank.strings.RegexAST;

import com.justinblank.strings.CharRange;
import com.justinblank.strings.Factorization;

import java.util.Optional;

public class CharRangeNode extends Node implements Comparable<CharRangeNode> {

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

    public int minLength() {
        return 1;
    }

    public Optional<Integer> maxLength() {
        return Optional.of(1);
    }

    @Override
    protected int height() {
        return 0;
    }

    public Factorization bestFactors() {
        return Factorization.fromRange(this.range.getStart(), this.range.getEnd());
    }

    @Override
    public int compareTo(CharRangeNode o) {
        return range.compareTo(o.range);
    }

    public Node reversed() {
        return this;
    }
}
