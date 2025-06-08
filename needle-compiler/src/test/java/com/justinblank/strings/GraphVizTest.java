package com.justinblank.strings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GraphVizTest {

    @Test
    void thatDFAToGraphvizRuns() {
        var dfa = DFA.createDFA("ab[cd][0-9]+");
        var string = GraphViz.toGraphviz(dfa);
        assertNotNull(string);
        assertNotEquals("", string);
    }
}