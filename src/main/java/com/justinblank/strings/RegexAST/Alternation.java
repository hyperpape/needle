package com.justinblank.strings.RegexAST;

import com.justinblank.strings.Factorization;

import java.util.Objects;

import java.util.*;

public class Alternation extends Node {

    public final Node left;
    public final Node right;

    public Alternation(Node left, Node right) {
        Objects.requireNonNull(left, "Cannot alternate nothing");
        // Objects.requireNonNull(right, "Cannot alternate nothing");
        this.left = left;
        this.right = right;
    }

    protected int minLength() {
        return Math.min(left.minLength(), right.minLength());
    }

    @Override
    protected int depth() {
        return 1 + Math.max(left.depth(), right.depth());
    }

    @Override
    public Factorization bestFactors() {
        Factorization leftFactors = left.bestFactors();
        Factorization rightFactors = right.bestFactors();
        leftFactors.alternate(rightFactors);
        return leftFactors;
    }

    @Override
    public Node reversed() {
        return new Alternation(left.reversed(), right.reversed());
    }

    public static Alternation ofChars(String s) {
        if (s.length() < 2) {
            throw new IllegalArgumentException("silly alternation");
        }
        char s1 = s.charAt(0);
        char s2 = s.charAt(1);
        Alternation alternation = new Alternation(new CharRangeNode(s1, s1), new CharRangeNode(s2, s2));
        for (int i = 2; i < s.length(); i++) {
            char c = s.charAt(i);
            alternation = new Alternation(new CharRangeNode(c, c), alternation);
        }
        return alternation;
    }

    public static Alternation complement(List<CharRangeNode> ranges) {
        if (ranges.isEmpty()) {
            throw new IllegalArgumentException("Can't complement empty set of ranges");
        }
        ranges = new ArrayList<>(ranges);
        Collections.sort(ranges);
        Iterator<CharRangeNode> it = ranges.iterator();
        List<CharRangeNode> complementedNodes = new ArrayList<>();
        CharRangeNode last = null;
        while (it.hasNext()) {
            CharRangeNode current = it.next();
            if (last != null) {
                char low = (char) (((int) last.range().getEnd()) + 1);
                char high = (char) (((int) current.range().getStart() - 1));
                if (low <= high) {
                    complementedNodes.add(new CharRangeNode(low, high));
                }
            }
            else {
                char high = (char) (((int) current.range().getStart()) -1);
                complementedNodes.add(new CharRangeNode('\u0001', high));
            }
            last = current;
        }
        char low = (char) (((int) last.range().getEnd()) + 1);
        complementedNodes.add(new CharRangeNode(low, '\uFFFF'));
        CharRangeNode first = complementedNodes.get(0);
        CharRangeNode second = complementedNodes.get(1);
        Alternation alternation = new Alternation(first, second);
        for (int i = 2; i < complementedNodes.size(); i++) {
            alternation = new Alternation(alternation, complementedNodes.get(i));
        }
        return alternation;
    }

    public static Alternation complement(String s) {
        if (s.length() < 2) {
            throw new IllegalArgumentException("Silly short complement");
        }
        List<Character> chars = new ArrayList<>();
        for (int i = 0; i < s.length(); i++) {
            chars.add(s.charAt(i));
        }
        Collections.sort(chars);
        List<CharRangeNode> nodes = new ArrayList<>();
        for (Character c : chars) {
            nodes.add(new CharRangeNode(c, c));
        }
        return Alternation.complement(nodes);
    }
}
