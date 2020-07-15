package com.justinblank.strings;

import com.justinblank.strings.RegexAST.*;

import java.util.Random;

class RegexGenerator {

    static final int TYPE_COUNTED = 0;
    static final int TYPE_REPETITION = 1;
    static final int TYPE_ALTERNATION = 2;
    static final int TYPE_CONCATENATION = 3;
    static final int TYPE_LITERAL = 6;
    static final int TYPE_RANGE = 7;

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
                int i1 = random.nextInt(10);
                int i2 = i1 == 0 ? 0 : random.nextInt(i1);
                return new CountedRepetition(child1, i2, i1);
            case 1:
                child1 = generate();
                return new Repetition(child1);
            case 2:
                child1 = generate();
                child2 = generate();
                return new Alternation(child1, child2);
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


}