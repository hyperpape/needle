package com.justinblank.strings.RegexAST;

import com.justinblank.strings.Factorization;

import java.util.Optional;

/**
 * That this class exists and is a Node is a hack, but I've not thought of a nicer way to handle it.
 */
public class LParenNode extends Node {

    private static final LParenNode NODE = new LParenNode();

    private LParenNode() {};

    public static LParenNode getInstance() {
        return NODE;
    }

    @Override
    public int minLength() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Integer> maxLength() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected int height() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Factorization bestFactors() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node reversed() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean nonAscii() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node toUTF16Bytes() {
        throw new UnsupportedOperationException();
    }
}
