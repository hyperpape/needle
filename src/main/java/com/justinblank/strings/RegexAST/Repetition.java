package com.justinblank.strings.RegexAST;

import java.util.Objects;

public class Repetition extends Node {

    public final Node node;

    public Repetition(Node node) {
        Objects.requireNonNull(node, "Cannot repeat nothing");
        this.node = node;
    }
}
