package com.justinblank.strings.RegexAST;

import com.justinblank.strings.Factorization;

public class LiteralNode extends Node {

    private StringBuilder string = new StringBuilder();

    public LiteralNode(String string) {
        this.string.append(string);
    }

    public static Node fromChar(char c) {
        return new LiteralNode(String.valueOf(c));
    }

    @Override
    protected int minLength() {
        return string.length();
    }

    @Override
    protected int depth() {
        return 0;
    }

    @Override
    public Factorization bestFactors() {
        return Factorization.fromString(string.toString());
    }

    public String getLiteral() {
        return string.toString();
    }

    public void append(char c) {
        this.string.append(c);
    }

    @Override
    public boolean isAlternationOfLiterals() {
        return true;
    }
}
