package com.justinblank.strings.RegexAST;

import com.justinblank.strings.Factorization;

import java.util.Objects;

public class Alternation extends Node {

    public final Node left;
    public final Node right;

    public Alternation(Node left, Node right) {
        Objects.requireNonNull(left, "Cannot alternate nothing");
        // Objects.requireNonNull(right, "Cannot alternate nothing");
        this.left = left;
        this.right = right;
    }

    protected int minLength() {
        return Math.min(left.minLength(), right.minLength());
    }

    @Override
    protected int depth() {
        return 1 + Math.max(left.depth(), right.depth());
    }

    @Override
    public Factorization bestFactors() {
        Factorization leftFactors = left.bestFactors();
        Factorization rightFactors = right.bestFactors();
        leftFactors.alternate(rightFactors);
        return leftFactors;
    }
}
