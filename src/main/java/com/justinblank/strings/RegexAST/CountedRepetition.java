package com.justinblank.strings.RegexAST;

import com.justinblank.strings.Factorization;

import java.util.Objects;

public class CountedRepetition extends Node {
    public final Node node;
    public final int min;
    public final int max;

    public CountedRepetition(Node node, int min, int max) {
        Objects.requireNonNull(node, "Cannot repeat nothing");
        if (min > max) {
            throw new IllegalArgumentException("Repetition with range of " + min + "," + max + " is invalid");
        }
        this.node = node;
        this.min = min;
        this.max = max;
    }

    protected int minLength() {
        return min;
    }

    @Override
    protected int depth() {
        return 1 + node.depth();
    }

    @Override
    public Factorization bestFactors() {
        return Factorization.empty();
    }
}
