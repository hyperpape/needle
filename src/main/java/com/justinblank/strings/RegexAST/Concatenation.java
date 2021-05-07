package com.justinblank.strings.RegexAST;

import com.justinblank.strings.Factorization;

import java.util.Objects;
import java.util.Optional;

public class Concatenation extends Node {

    public final Node head;
    public final Node tail;

    public Concatenation(Node head, Node tail) {
        Objects.requireNonNull(head, "Cannot concatenate nothing");
        Objects.requireNonNull(tail, "Cannot concatenate nothing");
        this.head = head;
        this.tail = tail;
    }

    protected int minLength() {
        return head.minLength() + tail.minLength();
    }

    protected Optional<Integer> maxLength() {
        return head.maxLength().flatMap(n -> tail.maxLength().map(m -> Math.max(n, m)));
    }

    @Override
    protected int height() {
        return 1 + Math.max(head.height(), tail.height());
    }

    public Factorization bestFactors() {
        Factorization left = head.bestFactors();
        Factorization right = tail.bestFactors();
        left.concatenate(right);
        return left;
    }

    @Override
    public Node reversed() {
        return new Concatenation(tail.reversed(), head.reversed());
    }


}
