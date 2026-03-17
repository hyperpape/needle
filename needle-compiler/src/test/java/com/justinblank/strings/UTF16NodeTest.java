package com.justinblank.strings;

import com.justinblank.strings.RegexAST.Concatenation;
import com.justinblank.strings.RegexAST.LiteralNode;
import com.justinblank.strings.RegexAST.Union;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UTF16NodeTest {

    @Test
    void canGenerateUTF16RangeNode() {
        var node = RegexParser.parse("[α-β]");
        var transformed = node.toUTF16Bytes();
        assertInstanceOf(Concatenation.class, transformed);
    }

    @Test
    void canGenerateUTF16RangeNode_crossingHighBoundary() {
        var node = RegexParser.parse("[÷-ą]");
        var transformed = node.toUTF16Bytes();
        assertInstanceOf(Union.class, transformed);
        Union union = (Union) transformed;
        assertInstanceOf(Concatenation.class, union.left);
        assertInstanceOf(Concatenation.class, union.right);
    }

    @Test
    void canGenerateUTF16RangeNode_crossingManyHighBoundaries() {
        var node = RegexParser.parse("[÷-ỹ]");
        var transformed = node.toUTF16Bytes();
        assertInstanceOf(Union.class, transformed);
        Union union = (Union) transformed;
        assertInstanceOf(Union.class, union.left);
        assertInstanceOf(Concatenation.class, union.right);
    }

    @Test
    void canGenerateUTF16LiteralNode() {
        var node = RegexParser.parse("α");
        var transformed = node.toUTF16Bytes();
        assertInstanceOf(LiteralNode.class, transformed);
    }

}
