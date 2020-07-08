package com.justinblank.strings.RegexAST;

import java.util.Stack;

/**
 * A printer for the parsed representation of a regular expression. Note that the output currently includes many
 * unnecessary parentheses, so most regular expressions do not do a clean round-trip.
 */
public class NodePrinter {

    private StringBuilder sb = new StringBuilder();
    private Stack<Node> stack = new Stack<>();

    private NodePrinter(Node node) {
        stack.push(node);
    }

    private void push(char c) {
        sb.append(escape(c));
    }

    public static String print(Node node) {
        return new NodePrinter(node)._print();
    }

    private String _print() {
        while (!stack.isEmpty()) {
            append(stack.pop());
        }
        return sb.toString();
    }

    public void append(Node node) {
        if (node instanceof CharRangeNode) {
            CharRangeNode crNode = (CharRangeNode) node;
            if (crNode.range().getStart() == crNode.range().getEnd()) {
                sb.append(crNode.range().getStart());
            }
            else {
                sb.append('[');
                sb.append(escape(crNode.range().getStart()));
                sb.append('-');
                sb.append(escape(crNode.range().getEnd()));
                sb.append(']');
            }
        }
        else if (node instanceof LiteralNode) {
            sb.append(((LiteralNode) node).getLiteral());
        }
        else if (node instanceof Concatenation) {
            Concatenation c = (Concatenation) node;
            stack.push(LiteralNode.fromChar(')'));
            stack.push(c.tail);
            stack.push(LiteralNode.fromChar('('));
            stack.push(LiteralNode.fromChar(')'));
            stack.push(c.head);
            stack.push(LiteralNode.fromChar('('));
        }
        else if (node instanceof Alternation) {
            Alternation alt = (Alternation) node;
            stack.push(LiteralNode.fromChar(')'));
            stack.push(alt.right);
            stack.push(LiteralNode.fromChar('('));
            stack.push(LiteralNode.fromChar('|'));
            stack.push(LiteralNode.fromChar(')'));
            stack.push(alt.left);
            stack.push(LiteralNode.fromChar('('));
        }
        else if (node instanceof CountedRepetition) {
            CountedRepetition cr = (CountedRepetition) node;
            stack.push(new LiteralNode("{" + cr.min + "," + cr.max + "}"));
            stack.push(LiteralNode.fromChar(')'));
            stack.push(((CountedRepetition) node).node);
            stack.push(LiteralNode.fromChar('('));
        }
        else if (node instanceof Repetition) {
            stack.push(LiteralNode.fromChar('*'));
            stack.push(LiteralNode.fromChar(')'));
            stack.push(((Repetition) node).node);
            stack.push(LiteralNode.fromChar('('));
        }

    }

    private String escape(char c) {
        switch (c) {
            case '*':
            case '(':
            case ')':
            case '[':
            case ']':
            case '$':
            case '^':
                return "\\" + c;
            default:
                return String.valueOf(c);
        }
    }
}
