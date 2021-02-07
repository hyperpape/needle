package com.justinblank.strings;

import org.junit.Test;

import java.util.Optional;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class RegexGeneratorTest {

    private static final int DUMMY = 1;

    @Test
    public void testMinimalGenerationLiteral() {
        var s = new RegexGenerator(new Random(), DUMMY).generateMinimalMatch("abcde");
        assertEquals("abcde", s);
    }

    @Test
    public void testMinimalGenerationRepetition() {
        var generator = new RegexGenerator(new Random(), DUMMY);
        var s = generator.generateMinimalMatch("a*");
        assertEquals("", s);
    }

    @Test
    public void testMinimalGenerationCountedRepetition() {
        var generator = new RegexGenerator(new Random(), DUMMY);
        var s = generator.generateMinimalMatch("a{1,2}");
        assertEquals("a", s);
    }

    @Test
    public void testMinimalGenerationComposite() {
        var generator = new RegexGenerator(new Random(), DUMMY);
        var s = generator.generateMinimalMatch("(a|b)a*");
        assertEquals("a", s);
    }

    @Test
    public void testMaximalGenerationLiteral() {
        var s = new RegexGenerator(new Random(), DUMMY).generateMaximalMatch("abcde");
        assertEquals(Optional.of("abcde"), s);
    }

    @Test
    public void testMaximalGenerationUnion() {
        var s = new RegexGenerator(new Random(), DUMMY).generateMaximalMatch("(abc)|(def)");
        assertEquals(Optional.of("abc"), s);
    }

    @Test
    public void testMaximalGenerationRepetition() {
        var s = new RegexGenerator(new Random(), DUMMY).generateMaximalMatch("a*");
        assertEquals(Optional.empty(), s);
    }

    @Test
    public void testMaximalGenerationCountedRepetition() {
        var s = new RegexGenerator(new Random(), DUMMY).generateMaximalMatch("a{1,2}");
        assertEquals(Optional.of("aa"), s);
    }

}
