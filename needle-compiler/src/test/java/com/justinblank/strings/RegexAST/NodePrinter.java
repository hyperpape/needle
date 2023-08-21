package com.justinblank.strings.RegexAST;

import java.util.Stack;

/**
 * A printer for the parsed representation of a regular expression. Note that the output currently includes many
 * unnecessary parentheses, so most regular expressions do not do a clean round-trip.
 */
public class NodePrinter {

    private final StringBuilder sb = new StringBuilder();
    private final Stack<Object> stack = new Stack<>();

    private NodePrinter(Node node) {
        stack.push(node);
    }

    public static String print(Node node) {
        return new NodePrinter(node)._print();
    }

    private String _print() {
        while (!stack.isEmpty()) {
            Object next = stack.pop();
            if (next instanceof Node) {
                append((Node) next);
            }
            else {
                sb.append(next.toString());
            }
        }
        return sb.toString();
    }

    public void append(Node node) {
        if (node instanceof CharRangeNode) {
            CharRangeNode crNode = (CharRangeNode) node;
            if (crNode.range().getStart() == crNode.range().getEnd()) {
                char start = crNode.range().getStart();
                sb.append(escape(start));
            }
            else {
                sb.append('[');
                char start = crNode.range().getStart();
                if (start == '[' || start == ']' || start == '\\') {
                    sb.append('\\').append(start);
                }
                else {
                    sb.append(start);
                }
                sb.append('-');
                char end = crNode.range().getEnd();
                if (end == '[' || end == ']' || end == '\\') {
                    sb.append('\\').append(end);
                }
                else {
                    sb.append(end);
                }
                sb.append(']');
            }
        }
        else if (node instanceof LiteralNode) {
            String literal = ((LiteralNode) node).getLiteral();
            for (int i = 0; i < literal.length(); i++) {
                sb.append(escape(literal.charAt(i)));
            }
        }
        else if (node instanceof Concatenation) {
            Concatenation c = (Concatenation) node;
            pushChild(node, c.tail);
            pushChild(node, c.head);
        }
        else if (node instanceof Union) {
            Union union = (Union) node;
            pushChild(node, union.right);
            stack.push("|");
            pushChild(node, union.left);
        }
        else if (node instanceof CountedRepetition) {
            CountedRepetition cr = (CountedRepetition) node;
            var child = ((CountedRepetition) node).node;
            stack.push("{" + cr.min + "," + cr.max + "}");
            pushChild(node, child);
        }
        else if (node instanceof Repetition) {
            stack.push("*");
            var child = ((Repetition) node).node;
            pushChild(node, child);
        }

    }

    private void pushChild(Node parent, Node child) {
        if (needsParens(parent, child)) {
            stack.push(")");
        }
        stack.push(child);
        if (needsParens(parent, child)) {
            stack.push("(");
        }
    }

    private boolean needsParens(Node parent, Node child) {
        if (child instanceof CharRangeNode) {
            return false;
        }
        else if (child instanceof LiteralNode) {
            var l = (LiteralNode) child;
            if (l.getLiteral().length() == 1) {
                return false;
            }
        }
        if (parent instanceof Concatenation) {
            return child instanceof Union;
        }
        return true;
    }

    private String escape(char c) {
        switch (c) {
            case '*':
            case '?':
            case '+':
            case '(':
            case ')':
            case '{':
            case '[': // TODO: Nail-down/comment behavior of ']'
            case '$':
            case '^':
            case ':':
            case '|':
            case '\\':
                return "\\" + c;
            default:
                return String.valueOf(c);
        }
    }
}
