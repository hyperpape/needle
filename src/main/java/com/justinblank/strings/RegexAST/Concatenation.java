package com.justinblank.strings.RegexAST;

import com.justinblank.strings.Factorization;

import java.util.Objects;

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

    @Override
    protected int depth() {
        return 1 + Math.max(head.depth(), tail.depth());
    }

    public Factorization bestFactors() {
        Factorization left = head.bestFactors();
        Factorization right = tail.bestFactors();
        left.concatenate(right);
        return left;
    }
}
