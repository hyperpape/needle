package com.justinblank.strings.RegexAST;

import com.justinblank.strings.Factorization;

import java.util.Optional;

public class LParenNode extends Node {

    private static final LParenNode NODE = new LParenNode();

    private LParenNode() {};

    public static LParenNode getInstance() {
        return NODE;
    }

    @Override
    protected int minLength() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Optional<Integer> maxLength() {
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
}
