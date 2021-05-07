package com.justinblank.strings.RegexAST;

import com.justinblank.strings.Factorization;

import java.util.Objects;
import java.util.Optional;

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

    public int minLength() {
        return min * node.minLength();
    }

    public Optional<Integer> maxLength() {
        return node.maxLength().map(n -> n * max);
    }

    @Override
    protected int height() {
        return 1 + node.height();
    }

    @Override
    public Factorization bestFactors() {
        return node.bestFactors().countedRepetition(min, max);
    }

    @Override
    public Node reversed() {
        return new CountedRepetition(node.reversed(), min, max);
    }
}
