package com.justinblank.strings.RegexAST;

import com.justinblank.strings.Factorization;
import org.apache.commons.lang3.StringUtils;

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

    @Override
    public Node reversed() {
        // is this...the one and only time it's ok to reverse a unicode string? Or is this bad?
        return new LiteralNode(StringUtils.reverse(this.string.toString()));
    }

    public String getLiteral() {
        return string.toString();
    }

    public void append(char c) {
        this.string.append(c);
    }

    public void append(LiteralNode node) {
        this.string.append(node.string);
    }
}
