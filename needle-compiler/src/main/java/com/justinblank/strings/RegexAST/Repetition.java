package com.justinblank.strings.RegexAST;

import com.justinblank.strings.Factorization;

import java.util.Objects;
import java.util.Optional;

public class Repetition extends Node {

    public final Node node;

    public Repetition(Node node) {
        Objects.requireNonNull(node, "Cannot repeat nothing");
        this.node = node;
    }

    public int minLength() {
        return 0;
    }

    public Optional<Integer> maxLength() {
        return Optional.empty();
    }

    @Override
    public Optional<LengthPolynomial> lengthPolynomial() {
        return node.lengthPolynomial().flatMap(p -> p.multi(0, -1));
    }

    @Override
    public Optional<LengthFilter> lengthFilter() {
        return node.lengthFilter().flatMap(LengthFilter::repeat);
    }

    @Override
    protected int height() {
        return 1 + node.height();
    }

    @Override
    public Factorization bestFactors() {
        return Factorization.empty();
    }

    @Override
    public Node reversed() {
        return new Repetition(node.reversed());
    }
}
