package com.justinblank.strings;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertNotEquals;

public class GraphVizTest {

    @Test
    public void testThatDFAToGraphvizRuns() {
        var dfa = DFA.createDFA("ab[cd][0-9]+");
        var string = GraphViz.toGraphviz(dfa);
        Assert.assertNotNull(string);
        assertNotEquals("", string);
    }
}