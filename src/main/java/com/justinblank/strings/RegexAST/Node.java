package com.justinblank.strings.RegexAST;

import com.justinblank.strings.Factorization;

public abstract class Node {

    protected abstract int minLength();

    public abstract Factorization bestFactors();
}

