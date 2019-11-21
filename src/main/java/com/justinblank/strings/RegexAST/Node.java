package com.justinblank.strings.RegexAST;

import com.justinblank.strings.Factorization;

public abstract class Node {

    protected abstract int minLength();

    protected abstract int depth();

    public abstract Factorization bestFactors();

    /**
     * @return whether the node is an alternation of literals (including the limiting case of a single literal).
     */
    public boolean isAlternationOfLiterals() {
        return false;
    }
}

