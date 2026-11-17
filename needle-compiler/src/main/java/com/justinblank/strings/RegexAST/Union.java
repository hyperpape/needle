package com.justinblank.strings.RegexAST;

import com.justinblank.strings.CharRange;
import com.justinblank.strings.Factorization;

import java.util.Objects;

import java.util.*;

public class Union extends Node {

    public final Node left;
    public final Node right;
    public final boolean withPriority;

    Union(Node left, Node right, boolean withPriority) {
        Objects.requireNonNull(left, "Cannot union nothing");
        // Objects.requireNonNull(right, "Cannot union nothing");
        this.left = left;
        this.right = right;
        this.withPriority = withPriority;
    }

    public int minLength() {
        return Math.min(left.minLength(), right.minLength());
    }

    public Optional<Integer> maxLength() {
        return left.maxLength().flatMap(n -> right.maxLength().map(m -> Math.max(n, m)));
    }

    @Override
    public Optional<LengthPolynomial> lengthPolynomial() {
        return Optional.empty();
    }

    @Override
    public Optional<LengthFilter> lengthFilter() {
        return left.lengthFilter().flatMap(l -> right.lengthFilter().flatMap(l::union));
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
        return Union.unorderedChoice(left.reversed(), right.reversed());
    }

    public static Node orderedChoice(Node left, Node right) {
        return of(left, right, true);
    }

    public static Node unorderedChoice(Node left, Node right) {
        return of(left, right, false);
    }

    static Node of(Node left, Node right, boolean withPriority) {
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
        return new Union(left, right, withPriority);
    }

    public static Node ofChars(String s) {
        if (s.isEmpty()) {
            throw new IllegalArgumentException("Cannot create a union of zero characters");
        }
        if (s.length() == 1) {
            return new CharRangeNode(CharRange.of(s.charAt(0), s.charAt(0)));
        }
        List<Character> sorted = sortedChars(s);
        Node union = null;
        int i = 0;
        int start = 0;
        while (i < sorted.size()) {
            char current = sorted.get(i);
            if (i < sorted.size() - 1) {
                char next = sorted.get(i + 1);
                if (current + 1 != next) {
                    var range = new CharRangeNode(CharRange.of(sorted.get(start), sorted.get(i)));
                    if (union == null) {
                        union = range;
                    }
                    else {
                        union = Union.unorderedChoice(union, range);
                    }
                    start = i + 1;
                }
            }
            else {
                var range = new CharRangeNode(CharRange.of(sorted.get(start), sorted.get(i)));
                if (union == null) {
                    union = range;
                }
                else {
                    union = Union.unorderedChoice(union, range);
                }
            }
            i++;
        }
        return union;
    }

    public static Node complement(List<CharRangeNode> ranges) {
        if (ranges.isEmpty()) {
            throw new IllegalArgumentException("Can't complement empty set of ranges");
        }
        ranges = new ArrayList<>(ranges);
        Collections.sort(ranges);
        List<CharRangeNode> complementedNodes = new ArrayList<>();
        int next = Character.MIN_VALUE;
        for (CharRangeNode current : ranges) {
            int start = current.range().getStart();
            int end = current.range().getEnd();
            if (start > next) {
                complementedNodes.add(new CharRangeNode((char) next, (char) (start - 1)));
            }
            next = Math.max(next, end + 1);
        }
        if (next <= Character.MAX_VALUE) {
            complementedNodes.add(new CharRangeNode((char) next, Character.MAX_VALUE));
        }
        if (complementedNodes.isEmpty()) {
            throw new IllegalArgumentException("Can't complement the set of all characters");
        }
        Node union = complementedNodes.get(0);
        for (int i = 1; i < complementedNodes.size(); i++) {
            union = Union.unorderedChoice(union, complementedNodes.get(i));
        }
        return union;
    }

    public static Node complement(String s) {
        if (s.length() < 2) {
            // TODO: why did I write this? Maybe to work around a silly limitation in my complement method?
            // If so, I should fix that
            throw new IllegalArgumentException("Silly short complement");
        }
        List<Character> chars = sortedChars(s);
        List<CharRangeNode> nodes = new ArrayList<>();
        for (Character c : chars) {
            nodes.add(new CharRangeNode(c, c));
        }
        return Union.complement(nodes);
    }

    private static List<Character> sortedChars(String s) {
        List<Character> chars = new ArrayList<>();
        for (int i = 0; i < s.length(); i++) {
            chars.add(s.charAt(i));
        }
        Collections.sort(chars);
        return chars;
    }
}
