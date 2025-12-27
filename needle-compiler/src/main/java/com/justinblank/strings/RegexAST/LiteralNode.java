package com.justinblank.strings.RegexAST;

import com.justinblank.strings.Factorization;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.Optional;

public class LiteralNode extends Node {

    private StringBuilder string = new StringBuilder();

    public LiteralNode(String string) {
        this.string.append(string);
    }

    public static Node fromChar(char c) {
        return new LiteralNode(String.valueOf(c));
    }

    @Override
    public int minLength() {
        return string.length();
    }

    @Override
    public Optional<Integer> maxLength() {
        return Optional.of(string.length());
    }

    @Override
    protected int height() {
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

    @Override
    public String toString() {
        return "LiteralNode{" + string +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        LiteralNode that = (LiteralNode) o;
        return Objects.equals(string.toString(), that.string.toString());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(string);
    }
}
