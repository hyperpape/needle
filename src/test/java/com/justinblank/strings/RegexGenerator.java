package com.justinblank.strings;

import com.justinblank.strings.RegexAST.*;

import java.util.Random;

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
}
