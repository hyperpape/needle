package com.justinblank.strings.RegexAST;

import com.justinblank.strings.Factorization;

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
    protected int depth() {
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
