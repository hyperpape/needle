package com.justinblank.strings;

import com.justinblank.strings.RegexAST.*;

import java.util.Optional;
import java.util.Random;

/**
 * Generates random {@link Node}s, and generates strings matching a given Node.
 */
class RegexGenerator {

    private final Random random;
    private int count;
    private final int maxSize;

    public RegexGenerator(Random random, int maxMaxSize) {
        this.random = random;
        this.maxSize = random.nextInt(maxMaxSize);
        this.count = 0;
    }

    public Node generate() {
        if (count + 1 >= maxSize) {
            return makeCharRangeNode();
        }
        count++;
        int nodeType = random.nextInt(8);
        Node child1;
        Node child2;
        switch (nodeType) {
            case 0:
                child1 = generate();
                // Choosing higher numbers here can occasionally lead to very large regexes/haystacks.
                // I wrapped IntegrationTest#generativeNFAMatchingTest in a timing loop to measure:
                // At 7, IntegrationTest#generativeNFAMatchingTest had 1 iteration > 10 seconds out of 1000.
                // At 8, I had at least 5 out of a 1000, including a 5 minute run.
                int i1 = random.nextInt(7);
                int i2 = i1 == 0 ? 0 : random.nextInt(i1);
                return new CountedRepetition(child1, i2, i1);
            case 1:
                child1 = generate();
                return new Repetition(child1);
            case 2:
                child1 = generate();
                child2 = generate();
                return new Union(child1, child2);
            case 3:
            case 4:
            case 5:
                child1 = generate();
                child2 = generate();
                return new Concatenation(child1, child2);
            case 6:
                char c = safeChar((char) random.nextInt(128));
                return new LiteralNode(String.valueOf(c));
            case 7:
                return makeCharRangeNode();
            default:
                throw new IllegalArgumentException("");
        }
    }
    
    private Node makeCharRangeNode() {
        char c1 = safeChar((char) (32 + random.nextInt(128 - 32)));
        int bound = 128 - c1;
        char c2 = safeChar((char) (random.nextInt(bound) + c1));
        if (c1 > c2) {
            if (c1 < 128) {
                c2 = (char) (((int) c1) + 1);
            }
            else {
                c2 = c1;
            }
        }
        return new CharRangeNode(c1, c2);
    }

    private char safeChar(char c) {
        if (RegexParserTest.ESCAPED_AS_LITERAL_CHARS.contains(c)) {
            return 'B';
        }
        return c;
    }

    String generateString(Node node) {
        StringBuilder sb = new StringBuilder();
        addToString(node, sb);
        return sb.toString();
    }

    public String generateMinimalMatch(String regex) {
        var sb = new StringBuilder();
        var node = RegexParser.parse(regex);
        addMinimalMatchToString(node, sb);
        return sb.toString();
    }

    public Optional<String> generateMaximalMatch(String regex) {
        var sb = new StringBuilder();
        var node = RegexParser.parse(regex);
        return addMaximalMatchToString(node, sb).map(StringBuilder::toString);
    }

    void addMinimalMatchToString(Node node, StringBuilder sb) {
        if (node instanceof LiteralNode) {
            sb.append(((LiteralNode) node).getLiteral());
        }
        else if (node instanceof CharRangeNode) {
            var range = (CharRangeNode) node;
            sb.append(range.range().getStart());
        }
        else if (node instanceof Concatenation) {
            var c = (Concatenation) node;
            addMinimalMatchToString(c.head, sb);
            addMinimalMatchToString(c.tail, sb);
        }
        else if (node instanceof Union) {
            var a = (Union) node;
            var left = new StringBuilder();
            var right = new StringBuilder();
            addMinimalMatchToString(a.left, left);
            addMinimalMatchToString(a.right, right);
            if (compare(left, right) > 0) {
                sb.append(right);
            }
            else {
                sb.append(left);
            }
        }
        else if (node instanceof CountedRepetition) {
            var cr = ((CountedRepetition) node);
            int count = cr.min;
            for (int i = 0; i < count; i++) {
                addToString(cr.node, sb);
            }
        }
    }

    private void addToString(Node node, StringBuilder sb) {
        if (node instanceof LiteralNode) {
            sb.append(((LiteralNode) node).getLiteral());
        }
        else if (node instanceof CharRangeNode) {
            var range = (CharRangeNode) node;
            if (range.range().getStart() == range.range().getEnd()) {
                sb.append(range.range().getStart());
            }
            else {
                var x = random.nextInt(range.range().getEnd() - range.range().getStart());
                var c = (char) ((int) (range.range().getStart()) + x);
                sb.append(c);
            }
        }
        else if (node instanceof Concatenation) {
            var c = (Concatenation) node;
            addToString(c.head, sb);
            addToString(c.tail, sb);
        }
        else if (node instanceof Union) {
            var a = (Union) node;
            var b = random.nextBoolean();
            addToString(b ? a.left : a.right, sb);
        }
        else if (node instanceof Repetition) {
            var n = ((Repetition) node).node;
            var count = random.nextInt(8);
            for (int i = 0; i < count; i++) {
                addToString(n, sb);
            }
        }
        else if (node instanceof CountedRepetition) {
            var cr = ((CountedRepetition) node);
            int count;
            if (cr.max == cr.min) {
                count = cr.max;
            }
            else {
                count = cr.min + random.nextInt(cr.max - cr.min);
            }
            for (int i = 0; i < count; i++) {
                addToString(cr.node, sb);
            }
        }
    }

    private Optional<StringBuilder> addMaximalMatchToString(Node node, StringBuilder sb) {
        if (node instanceof LiteralNode) {
            sb.append(((LiteralNode) node).getLiteral());
            return Optional.of(sb);
        }
        else if (node instanceof CharRangeNode) {
            var range = (CharRangeNode) node;
            if (range.range().getStart() == range.range().getEnd()) {
                sb.append(range.range().getStart());
            }
            else {
                var x = random.nextInt(range.range().getEnd() - range.range().getStart());
                var c = (char) ((int) (range.range().getStart()) + x);
                sb.append(c);
            }
            return Optional.of(sb);
        }
        else if (node instanceof Concatenation) {
            var c = (Concatenation) node;
            var left = addMaximalMatchToString(c.head, new StringBuilder());
            var right = addMaximalMatchToString(c.tail, new StringBuilder());
            var result = left.map(sb::append);
            return right.flatMap(r -> result.flatMap(x -> Optional.of(r.append(x))));
        }
        else if (node instanceof Union) {
            var a = (Union) node;
            var left = addMaximalMatchToString(a.left, new StringBuilder());
            var right = addMaximalMatchToString(a.right, new StringBuilder());
            if (left.isPresent() && right.isPresent()) {
                var lString = left.get();
                var rString = right.get();
                if (compare(lString, rString) < 0) {
                    sb.append(rString);
                }
                else {
                    sb.append(lString);
                }
                return Optional.of(sb);
            }
            else {
                return left.map(sb::append).or(() -> right.map(sb::append));
            }
        }
        else if (node instanceof Repetition) {
            return Optional.empty();
        }
        else if (node instanceof CountedRepetition) {
            var cr = ((CountedRepetition) node);
            for (int i = 0; i < cr.max; i++) {
                addToString(cr.node, sb);
            }
            return Optional.of(sb);
        }
        else {
            throw new IllegalStateException("Encountered an ");
        }
    }

    private static int compare(StringBuilder s1, StringBuilder s2) {
        if (s1.length() < s2.length()) {
            return -1;
        }
        else if (s1.length() > s2.length()) {
            return 1;
        }
        else {
            return s1.compareTo(s2);
        }
    }
}
