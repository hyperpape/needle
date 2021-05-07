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

    protected int minLength() {
        return 0;
    }

    protected Optional<Integer> maxLength() {
        return Optional.empty();
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
