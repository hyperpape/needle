package com.justinblank.strings.RegexAST;

import com.justinblank.strings.Factorization;

import java.util.Objects;

import java.util.*;

public class Union extends Node {

    public final Node left;
    public final Node right;

    public Union(Node left, Node right) {
        Objects.requireNonNull(left, "Cannot union nothing");
        // Objects.requireNonNull(right, "Cannot union nothing");
        this.left = left;
        this.right = right;
    }

    public int minLength() {
        return Math.min(left.minLength(), right.minLength());
    }

    public Optional<Integer> maxLength() {
        return left.maxLength().flatMap(n -> right.maxLength().map(m -> Math.max(n, m)));
    }

    @Override
    protected int height() {
        return 1 + Math.max(left.height(), right.height());
    }

    @Override
    public Factorization bestFactors() {
        Factorization leftFactors = left.bestFactors();
        Factorization rightFactors = right.bestFactors();
        leftFactors.union(rightFactors);
        return leftFactors;
    }

    @Override
    public Node reversed() {
        return new Union(left.reversed(), right.reversed());
    }

    public static Node of(Node left, Node right) {
        if (left.equals(right)) {
            return left;
        }
        if (left instanceof Union) {
            Union lUnion = (Union) left;
            if (lUnion.left.equals(right) || lUnion.right.equals(right)) {
                return left;
            }
        }
        if (right instanceof Union) {
            Union rUnion = (Union) right;
            if (rUnion.left.equals(left) || rUnion.right.equals(left)) {
                return right;
            }
        }
        return new Union(left, right);
    }

    public static Union ofChars(String s) {
        if (s.length() < 2) {
            throw new IllegalArgumentException("silly union");
        }
        char s1 = s.charAt(0);
        char s2 = s.charAt(1);
        Union union = new Union(new CharRangeNode(s1, s1), new CharRangeNode(s2, s2));
        for (int i = 2; i < s.length(); i++) {
            char c = s.charAt(i);
            union = new Union(new CharRangeNode(c, c), union);
        }
        return union;
    }

    public static Union complement(List<CharRangeNode> ranges) {
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
                complementedNodes.add(new CharRangeNode('\u0000', high));
            }
            last = current;
        }
        char low = (char) (((int) last.range().getEnd()) + 1);
        complementedNodes.add(new CharRangeNode(low, '\uFFFF'));
        CharRangeNode first = complementedNodes.get(0);
        CharRangeNode second = complementedNodes.get(1);
        Union union = new Union(first, second);
        for (int i = 2; i < complementedNodes.size(); i++) {
            union = new Union(union, complementedNodes.get(i));
        }
        return union;
    }

    public static Union complement(String s) {
        if (s.length() < 2) {
            // TODO: why did I write this? Maybe to work around a silly limitation in my complement method?
            // If so, I should fix that
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
        return Union.complement(nodes);
    }
}
