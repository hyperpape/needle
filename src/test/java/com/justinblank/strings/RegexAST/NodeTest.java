package com.justinblank.strings.RegexAST;

import com.justinblank.strings.RegexParser;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class NodeTest {

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
}
