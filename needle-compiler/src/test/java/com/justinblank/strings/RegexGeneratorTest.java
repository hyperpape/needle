package com.justinblank.strings;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegexGeneratorTest {

    private static final int DUMMY = 1;

    @Test
    void minimalGenerationLiteral() {
        var s = new RegexGenerator(new Random(), DUMMY).generateMinimalMatch("abcde");
        assertEquals("abcde", s);
    }

    @Test
    void minimalGenerationRepetition() {
        var generator = new RegexGenerator(new Random(), DUMMY);
        var s = generator.generateMinimalMatch("a*");
        assertEquals("", s);
    }

    @Test
    void minimalGenerationCountedRepetition() {
        var generator = new RegexGenerator(new Random(), DUMMY);
        var s = generator.generateMinimalMatch("a{1,2}");
        assertEquals("a", s);
    }

    @Test
    void minimalGenerationComposite() {
        var generator = new RegexGenerator(new Random(), DUMMY);
        var s = generator.generateMinimalMatch("(a|b)a*");
        assertEquals("a", s);
    }

    @Test
    void maximalGenerationLiteral() {
        var s = new RegexGenerator(new Random(), DUMMY).generateMaximalMatch("abcde");
        assertEquals(Optional.of("abcde"), s);
    }

    @Test
    void maximalGenerationUnion() {
        var s = new RegexGenerator(new Random(), DUMMY).generateMaximalMatch("(abc)|(def)");
        assertEquals(Optional.of("def"), s);
    }

    @Test
    void maximalGenerationRepetition() {
        var s = new RegexGenerator(new Random(), DUMMY).generateMaximalMatch("a*");
        assertEquals(Optional.empty(), s);
    }

    @Test
    void maximalGenerationCountedRepetition() {
        var s = new RegexGenerator(new Random(), DUMMY).generateMaximalMatch("a{1,2}");
        assertEquals(Optional.of("aa"), s);
    }

}
