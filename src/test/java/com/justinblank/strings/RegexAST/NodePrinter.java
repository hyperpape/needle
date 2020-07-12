package com.justinblank.strings.RegexAST;

import java.util.Stack;

/**
 * A printer for the parsed representation of a regular expression. Note that the output currently includes many
 * unnecessary parentheses, so most regular expressions do not do a clean round-trip.
 */
public class NodePrinter {

    private StringBuilder sb = new StringBuilder();
    private Stack<Object> stack = new Stack<>();

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
                if (start == '[' || start == ']') {
                    sb.append('\\').append(start);
                }
                else {
                    sb.append(start);
                }
                sb.append('-');
                char end = crNode.range().getEnd();
                if (end == '[' || end == ']') {
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
            stack.push(")");
            stack.push(c.tail);
            stack.push("(");
            stack.push(")");
            stack.push(c.head);
            stack.push("(");
        }
        else if (node instanceof Alternation) {
            Alternation alt = (Alternation) node;
            stack.push(")");
            stack.push(alt.right);
            stack.push("(");
            stack.push("|");
            stack.push(")");
            stack.push(alt.left);
            stack.push("(");
        }
        else if (node instanceof CountedRepetition) {
            CountedRepetition cr = (CountedRepetition) node;
            stack.push("{" + cr.min + "," + cr.max + "}");
            stack.push(")");
            stack.push(((CountedRepetition) node).node);
            stack.push("(");
        }
        else if (node instanceof Repetition) {
            stack.push("*");
            stack.push(")");
            stack.push(((Repetition) node).node);
            stack.push("(");
        }

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
                return "\\" + c;
            default:
                return String.valueOf(c);
        }
    }
}
