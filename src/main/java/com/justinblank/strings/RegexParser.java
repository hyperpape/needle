package com.justinblank.strings;

import com.justinblank.strings.RegexAST.*;

import java.util.*;

public class RegexParser {

    private int index = 0;
    private int parenDepth = 0;
    private String regex;
    private Stack<Node> nodes = new Stack<>();

    protected RegexParser(String regex) {
        this.regex = regex;
    }

    public static Node parse(String regex) {
        return new RegexParser(regex)._parse();
    }

    private Node _parse() {
        while (index < regex.length()) {
            char c = takeChar();

            switch (c) {
                case '(':
                    nodes.push(LParenNode.getInstance());
                    break;
                case '{':
                    if (nodes.isEmpty()) {
                        throw new RegexSyntaxException("Found '{' at position " + index + " with no preceding regex");
                    }
                    int left = consumeInt();
                    char next = takeChar();
                    if (next != ',') {
                        throw new RegexSyntaxException("Expected ',' at " + index + ", but found " + next);
                    }
                    int right = consumeInt();
                    nodes.push(new CountedRepetition(nodes.pop(), left, right));
                    next = takeChar();
                    if (next != '}') {
                        throw new RegexSyntaxException("Found unclosed brackets at position " + index);
                    }
                    break;
                case '?':
                    if (nodes.isEmpty()) {
                        throw new RegexSyntaxException("");
                    }
                    nodes.push(new CountedRepetition(nodes.pop(), 0, 1));
                    break;
                case '[':
                    nodes.push(buildCharSet());
                    break;
                case '+':
                    if (nodes.isEmpty()) {
                        throw new RegexSyntaxException("Found '+' with no preceding regex");
                    }
                    Node lastNode = nodes.pop();
                    nodes.push(concatenate(lastNode, new Repetition(lastNode)));
                    break;
                case '*':
                    if (nodes.isEmpty()) {
                        throw new RegexSyntaxException("Found '*' with no preceding regex");
                    }
                    nodes.push(new Repetition(nodes.pop()));
                    break;
                case '|':
                    assertNonEmpty("'|' cannot be the final character in a regex");
                    collapseLiterals();
                    Node last = nodes.pop();
                    nodes.push(new Alternation(last, null));
                    break;
                case '}':
                    throw new RegexSyntaxException("Unbalanced '}' character");
                case ')':
                    collapseParenNodes();
                    break;
                case ']':
                    throw new RegexSyntaxException("Unbalanced ']' character");
                default:
                    if (nodes.isEmpty()) {
                        nodes.push(LiteralNode.fromChar(c));
                    }
                    else {
                        nodes.push(LiteralNode.fromChar(c));
                    }
            }
        }
        if (nodes.isEmpty()) {
            return new LiteralNode("");
        }
        Node node = nodes.pop();
        if (node instanceof LParenNode) {
            throw new RegexSyntaxException("Unbalanced '('");
        }
        while (!nodes.isEmpty()) {
            Node next = nodes.pop();
            if (next instanceof Alternation && ((Alternation) next).right == null) {
                Alternation alt = (Alternation) next;
                node = new Alternation(alt.left, node);
            }
            else if (next instanceof LiteralNode && node instanceof LiteralNode) {
                node = new LiteralNode(((LiteralNode) next).getLiteral() + ((LiteralNode) node).getLiteral());
            }
            else if (next instanceof LParenNode) {
                throw new RegexSyntaxException("Unbalanced ( found");
            }
            else {
                node = concatenate(next, node);
            }
        }
        return node;
    }

    private void collapseLiterals() {
        Node last = nodes.pop();

        while (!nodes.isEmpty()) {
            Node previous = nodes.peek();
            if (previous instanceof LiteralNode) {
                previous = nodes.pop();
                last = concatenate(previous, last);
            }
            else if (previous instanceof Alternation) {
                Alternation alt = (Alternation) previous;
                if (alt.right == null) {
                    nodes.pop();
                    last = new Alternation(alt.left, last);
                }
            }
            else {
                break;
            }
        }
        nodes.push(last);
    }

    private void collapseParenNodes() {
        assertNonEmpty("found unbalanced ')'");
        Node node = null;
        while (!(nodes.peek() instanceof LParenNode)) {
            Node next = nodes.pop();
            if (node == null) {
                node = next;
            }
            else if (next instanceof Alternation) {
                Alternation alt = (Alternation) next;
                if (alt.left != null && alt.right != null) {
                    node = new Concatenation(alt, node);
                    continue;
                }
                assertNonEmpty("found '|' with no preceding content");
                Node nextNext = nodes.pop();
                if (nextNext instanceof LParenNode) {
                    nodes.push(new Alternation(alt.left, node));
                    return;
                }
                node = new Alternation(alt.left, node);
            }
            else {
                node = concatenate(next, node);
            }
            assertNonEmpty("found unbalanced ')'");
        }
        nodes.pop(); // remove the left paren
        if (node == null) {
            node = new LiteralNode("");
        }
        nodes.push(node);

    }

    private Node concatenate(Node next, Node node) {
        if (next instanceof LiteralNode && node instanceof LiteralNode) {
            ((LiteralNode) next).append((LiteralNode) node);
            return next;
        }
        return new Concatenation(next, node);
    }

    private void assertNonEmpty(String s) {
        if (nodes.isEmpty()) {
            throw new RegexSyntaxException(s);
        }
    }

    private char takeChar() {
        return regex.charAt(index++);
    }

    private int consumeInt() {
        int initialIndex = index;
        try {
            while (index < regex.length()) {
                char next = regex.charAt(index);
                if (next < '0' || next > '9') {
                    String subString = regex.substring(initialIndex, index);
                    return Integer.parseInt(subString);
                }
                takeChar();
            }
        }
        catch (NumberFormatException e) {
            throw new RegexSyntaxException("Expected number, found " + regex.substring(initialIndex, index));
        }
        throw new RegexSyntaxException("Expected number, found " + regex.substring(initialIndex, index));
    }

    private Node buildCharSet() {
        Set<Character> characterSet = new HashSet<>();
        Set<CharRange> ranges = new HashSet<>();
        Character last = null;
        while (index < regex.length()) {
            char c = takeChar();
            if (c == ']') {
                if (last != null) {
                    characterSet.add(last);
                }
                return buildNode(characterSet, ranges);
            } else if (c == '-') {
                // TODO: find out actual semantics
                if (last == null || index == regex.length()) {
                    throw new RegexSyntaxException("Parsing failed");
                }
                char next = takeChar();
                ranges.add(new CharRange(last, next));
                last = null;
            } else if (c == '(' || c == ')') {
                throw new RegexSyntaxException("Parsing failed");
            } else if (c == '[') {
                throw new RegexSyntaxException("Unexpected '[' inside of character class");
            } else {
                if (last != null) {
                    characterSet.add(last);
                }
                last = c;
            }
        }
        throw new RegexSyntaxException("Parsing failed, unmatched [");
    }

    private Node buildNode(Set<Character> characterSet, Set<CharRange> ranges) {
        if (ranges.isEmpty() && characterSet.isEmpty()) {
            throw new RegexSyntaxException("Parsing failed: empty [] construction");
        } else if (characterSet.isEmpty() && ranges.size() == 1) {
            CharRange range = ranges.iterator().next();
            return new CharRangeNode(range);
        } else if (ranges.isEmpty() && characterSet.size() == 1) {
            Character character = characterSet.iterator().next();
            return new CharRangeNode(character, character);
        } else {
            return buildRanges(characterSet, ranges);
        }
    }

    private Node buildRanges(Set<Character> characterSet, Set<CharRange> ranges) {
        List<CharRange> sortedCharRanges = buildSortedCharRanges(characterSet, ranges);
        if (sortedCharRanges.size() == 1) {
            return new CharRangeNode(sortedCharRanges.get(0));
        } else {
            CharRangeNode first = new CharRangeNode(sortedCharRanges.get(0));
            CharRangeNode second = new CharRangeNode(sortedCharRanges.get(1));
            Node node = new Alternation(first, second);
            for (int i = 2; i < sortedCharRanges.size(); i++) {
                node = new Alternation(node, new CharRangeNode(sortedCharRanges.get(i)));
            }
            return node;
        }
    }

    private List<CharRange> buildSortedCharRanges(Set<Character> characterSet, Set<CharRange> ranges) {
        List<Character> characters = new ArrayList<>(characterSet);
        Collections.sort(characters);
        List<CharRange> charRanges = new ArrayList<>(ranges);
        characters.stream().map(c -> new CharRange(c, c)).forEach(charRanges::add);
        return CharRange.compact(charRanges);
    }
}
