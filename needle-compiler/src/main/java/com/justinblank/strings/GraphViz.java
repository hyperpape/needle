package com.justinblank.strings;

import java.util.HashSet;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

public class GraphViz {

    public static void main(String[] args) {
        System.out.println(printRepresentation(args[0]));
    }

    public static String printRepresentation(String regex) {
        DFA dfa = DFA.createDFA(regex);
        return toGraphviz(dfa);
    }


    public static String toGraphviz(DFA dfa) {
        // TODO: before making this part of the public API/tooling, find proper way to test this code
        var graphSb = new StringBuilder();
        graphSb.append("digraph {\n");
        graphSb.append("rankdir=LR\n");

        List<Integer> acceptingStates = dfa.acceptingStates().stream().map(DFA::getStateNumber).sorted().collect(Collectors.toList());
        graphSb.append("node [shape = doublecircle]; ");
        for (var stateNumber : acceptingStates) {
            graphSb.append(stateNumber).append(" ");
        }
        graphSb.append(";\n");
        graphSb.append("node [shape = circle];").append("\n");

        var seen = new HashSet<Integer>();
        var pending = new Stack<DFA>();
        pending.push(dfa);
        seen.add(dfa.getStateNumber());

        while (!pending.isEmpty()) {
            var current = pending.pop();
            for (var transition : current.getTransitions()) {
                var targetDfa = transition.getRight();
                if (!seen.contains(targetDfa.getStateNumber())) {
                    pending.push(targetDfa);
                    seen.add(targetDfa.getStateNumber());
                }
                graphSb.append(current.getStateNumber()).append(" -> ").append(targetDfa.getStateNumber());
                graphSb.append("[label=\"");
                var charRange = transition.getLeft();
                if (charRange.isSingleCharRange()) {
                    graphSb.append(graphVizEncode(transition.getLeft().getStart()));
                }
                else {
                    graphSb.append(graphVizEncode(transition.getLeft().getStart())).append("-").append(graphVizEncode(transition.getLeft().getEnd()));
                }
                graphSb.append("\"]").append(";\n");
            }
        }

        graphSb.append("}");
        return graphSb.toString();
    }

    static String graphVizEncode(char start) {
        switch(start) {
            case '\n':
                return "\\n";
            case '\r':
                return "\\r";
            case '\f':
                return "\\f";
            case '\"':
                return "\\\"";
            case '\b':
                return "\\b";
            case '\\':
                return "\\\\";
            case '\t':
                return "\\t";


            default:
                if (start < ' ') {
                    return "\\u00" + (Integer.toHexString((int) start));
                }
                return String.valueOf(start);
        }
    }
}
