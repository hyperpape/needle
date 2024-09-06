package com.justinblank.strings.RegexAST;

import com.justinblank.strings.Factorization;

import java.util.Optional;

public abstract class Node {

    public abstract int minLength();

    /**
     * @return the maximum length string this node can match, if defined.
     * Unbouded repetitions have no maximum length.
     */
    public abstract Optional<Integer> maxLength();

    public boolean isFixedLength() {
        var maxLength = maxLength();
        if (maxLength.isPresent()) {
            return maxLength.get().equals(minLength());
        }
        return false;
    }

    protected abstract int height();

    public abstract Factorization bestFactors();

    public abstract Node reversed();
}

