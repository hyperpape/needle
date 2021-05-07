package com.justinblank.strings.RegexAST;

import com.justinblank.strings.Factorization;

import java.util.Optional;

public abstract class Node {

    protected abstract int minLength();

    /**
     * @return the maximum length string this node can match, if defined.
     * Unbouded repetitions have no maximum length.
     */
    protected abstract Optional<Integer> maxLength();

    protected abstract int height();

    public abstract Factorization bestFactors();

    public abstract Node reversed();
}

