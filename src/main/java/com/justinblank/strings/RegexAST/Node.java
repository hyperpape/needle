package com.justinblank.strings.RegexAST;

import com.justinblank.strings.Factorization;

import java.util.List;

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

    // TODO: we're missing some character ranges that we should handle
    public static boolean isAhoCorasickPattern(Node node) {
        return node.isAlternationOfLiterals();
    }

    public static void extractLiterals(Node node, List<String> strings) {
        if (node instanceof LiteralNode) {
            strings.add(((LiteralNode) node).getLiteral());
        }
        else {
            Alternation alt = (Alternation) node;
            extractLiterals(alt.left, strings);
            extractLiterals(alt.right, strings);
        }
    }


}

