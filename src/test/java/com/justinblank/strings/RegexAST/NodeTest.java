package com.justinblank.strings.RegexAST;

import com.justinblank.strings.RegexParser;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NodeTest {

    @Test
    public void testMinLengthaSTAR_aORb_() {
        Node node = RegexParser.parse("a*(a|b)");
        assertEquals(1, node.minLength());
    }
}
