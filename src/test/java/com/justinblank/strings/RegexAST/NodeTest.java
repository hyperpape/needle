package com.justinblank.strings.RegexAST;

import com.justinblank.strings.RegexParser;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class NodeTest {

    @Test
    public void testMinMaxLengthLiteral() {
        Node node = RegexParser.parse("abcdef");
        assertEquals(6, node.minLength());
        assertEquals(Optional.of(6), node.maxLength());
    }

    @Test
    public void testMinMaxLengthEmptyString() {
        Node node = RegexParser.parse("");
        assertEquals(0, node.minLength());
        assertEquals(Optional.of(0), node.maxLength());
    }

    @Test
    public void testMinMaxLengthCharRange() {
        Node node = RegexParser.parse("[0-9]");
        assertEquals(1, node.minLength());
        assertEquals(Optional.of(1), node.maxLength());
    }

    @Test
    public void testMinMaxLengthaSTAR_aORb_() {
        Node node = RegexParser.parse("a*(a|b)");
        assertEquals(1, node.minLength());
        assertEquals(Optional.empty(), node.maxLength());
    }

    @Test
    public void testMinMaxLengthCountedRepetition_() {
        Node node = RegexParser.parse("(ab){2,3}");
        assertEquals(4, node.minLength());
        assertEquals(Optional.of(6), node.maxLength());
    }

    @Test
    public void testMinMaxLengthUnionContainingRepetition() {
        Node node = RegexParser.parse("(ab)|(a*)");
        assertEquals(0, node.minLength());
        assertEquals(Optional.empty(), node.maxLength());
    }

    @Test
    public void testMinMaxLengthUnionFollowedByChar() {
        Node node = RegexParser.parse("(a|b)a");
        assertEquals(2, node.minLength());
        assertEquals(Optional.of(2), node.maxLength());
    }

    @Test
    public void testMinMaxLengthUnionOfDifferentLengths() {
        Node node = RegexParser.parse("(ab)|b");
        assertEquals(1, node.minLength());
        assertEquals(Optional.of(2), node.maxLength());
    }
}
