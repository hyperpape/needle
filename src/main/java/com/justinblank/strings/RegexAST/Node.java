package com.justinblank.strings.RegexAST;

import com.justinblank.strings.Factorization;

public abstract class Node {

    protected abstract int minLength();

    protected abstract int depth();

    public abstract Factorization bestFactors();

    public abstract Node reversed();
}

