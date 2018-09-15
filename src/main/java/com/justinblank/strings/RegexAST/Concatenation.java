package com.justinblank.strings.RegexAST;

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
}
