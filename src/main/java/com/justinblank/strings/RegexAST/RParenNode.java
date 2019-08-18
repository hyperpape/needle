package com.justinblank.strings.RegexAST;

import com.justinblank.strings.Factorization;

public class RParenNode extends Node {

    private RParenNode() {};

    private static final RParenNode NODE = new RParenNode();

    public static RParenNode getInstance() {
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
}
