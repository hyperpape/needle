package com.justinblank.strings;

import java.util.*;
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

        appendStateTransitions(dfa, graphSb);

        graphSb.append("}");
        return graphSb.toString();
    }

    private static void appendStateTransitions(DFA dfa, StringBuilder graphSb) {
        var seen = new HashSet<Integer>();
        var pending = new Stack<DFA>();
        pending.push(dfa);
        seen.add(dfa.getStateNumber());

        List<String> stateTransitionStrings = new ArrayList<>();
        while (!pending.isEmpty()) {
            var current = pending.pop();
            for (var transition : current.getTransitions()) {
                StringBuilder sb = new StringBuilder();
                var targetDfa = transition.getRight();
                if (!seen.contains(targetDfa.getStateNumber())) {
                    pending.push(targetDfa);
                    seen.add(targetDfa.getStateNumber());
                }
                sb.append(current.getStateNumber()).append(" -> ").append(targetDfa.getStateNumber());
                sb.append("[label=\"");
                var charRange = transition.getLeft();
                if (charRange.isSingleCharRange()) {
                    sb.append(graphVizEncode(transition.getLeft().getStart()));
                }
                else {
                    sb.append(graphVizEncode(transition.getLeft().getStart())).append("-").append(graphVizEncode(transition.getLeft().getEnd()));
                }
                sb.append("\"]").append(";\n");
                stateTransitionStrings.add(sb.toString());
            }
        }
        sortStateTransitionStrings(stateTransitionStrings);
        for (String stateTransition : stateTransitionStrings) {
            graphSb.append(stateTransition);
        }
    }

    private static void sortStateTransitionStrings(List<String> stateTransitionStrings) {
        stateTransitionStrings.sort((s1, s2) -> {
            var s1State = takeInt(s1);
            var s2State = takeInt(s2);
            return s1State - s2State;
        });
    }

    private static int takeInt(String s1) {
        for (var i = 0; i < s1.length(); i++) {
            if (s1.charAt(i) < '0' || s1.charAt(i) > '9') {
                return Integer.parseInt(s1.substring(0, i));
            }
        }
        throw new IllegalArgumentException("Passed a string that didn't start with an integer, String='" + s1 + "'");
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
            // TODO: Handle more unicode characters
            case '\uFFFE':
            case '\uFFFF':
                return "\\u" + (Integer.toHexString(start));

            default:
                if (start < ' ') {
                    return "\\u00" + Integer.toHexString((start));
                }
                return String.valueOf(start);
        }
    }
}
