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
                        throw new IllegalStateException();
                    }
                    int left = consumeInt();
                    char next = takeChar();
                    if (next != ',') {
                        throw new IllegalStateException("Expected ',' at " + index + ", but found " + next);
                    }
                    int right = consumeInt();
                    nodes.push(new CountedRepetition(nodes.pop(), left, right));
                    next = takeChar();
                    if (next != '}') {
                        throw new IllegalStateException();
                    }
                    break;
                case '?':
                    if (nodes.isEmpty()) {
                        throw new IllegalStateException("");
                    }
                    nodes.push(new CountedRepetition(nodes.pop(), 0, 1));
                    break;
                case '[':
                    nodes.push(buildCharSet());
                    break;
                case '+':
                    if (nodes.isEmpty()) {
                        throw new IllegalStateException();
                    }
                    Node lastNode = nodes.pop();
                    nodes.push(new Concatenation(lastNode, new Repetition(lastNode)));
                    break;
                case '*':
                    nodes.push(new Repetition(nodes.pop()));
                    break;
                case '|':
                    assertNonEmpty("");
                    Node last = nodes.peek();
                    if (last instanceof CharRangeNode || last instanceof Concatenation || last instanceof LiteralNode || last instanceof RParenNode) {
                        nodes.push(new Alternation(last, null));
                    }
                    break;
                case '}':
                    throw new IllegalStateException("Unbalanced '}' character");
                case ')':
                    // TODO not strong enough check
                    collapseParenNodes();
                    break;
                default:
                    if (nodes.isEmpty()) {
                        nodes.push(LiteralNode.fromChar(c));
                    }
                    else if (nodes.peek() instanceof LiteralNode) {
                        ((LiteralNode) nodes.peek()).append(c);
                    }
                    else if (nodes.peek() instanceof CharRangeNode || nodes.peek() instanceof Concatenation) {
                        nodes.push(new Concatenation(nodes.pop(), new CharRangeNode(c, c)));
                    }
                    else {
                        nodes.push(LiteralNode.fromChar(c));
                    }
            }
        }
        Node node = nodes.pop();
        if (node instanceof LParenNode) {
            throw new IllegalStateException("Unbalanced '('");
        }
        while (!nodes.isEmpty()) {
            Node next = nodes.pop();
            if (next instanceof Alternation && ((Alternation) next).right == null) {
                Alternation alt = (Alternation) next;
                assertNonEmpty("Alternation needed something to alternate");
                Node nextNext = nodes.pop();
                node = new Alternation(node, nextNext);
            }
            else if (next instanceof LiteralNode && node instanceof LiteralNode) {
                node = new LiteralNode(((LiteralNode) next).getLiteral() + ((LiteralNode) node).getLiteral());
            }
            else {
                node = new Concatenation(next, node);
            }
        }
        return node;
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
                assertNonEmpty("found '|' with no preceding content");
                Node nextNext = nodes.pop();
                if (nextNext instanceof LParenNode) {
                    throw new IllegalStateException("");
                }
                node = new Alternation(alt.left, node);
            }
            else {
                node = new Concatenation(next, node);
            }
            assertNonEmpty("found unbalanced ')'");
        }
        nodes.pop(); // remove the left paren
        if (node != null) {
            nodes.push(node);
        }
    }

    private void assertNonEmpty(String s) {
        if (nodes.isEmpty()) {
            throw new IllegalStateException(s);
        }
    }

    private void addAlternationToNodes(Stack<Node> nodes) {
        if (nodes.isEmpty()) {
            throw new IllegalStateException();
        }
        Node right = nodes.pop();
        if (nodes.isEmpty()) {
            throw new IllegalStateException();
        }
        Node left = nodes.pop();
        nodes.push(new Alternation(left, right));
    }

    private char takeChar() {
        return regex.charAt(index++);
    }

    private int consumeInt() {
        int initialIndex = index;
        while (index < regex.length()) {
            char next = regex.charAt(index);
            if (next < '0' || next > '9') {
                String subString = regex.substring(initialIndex, index);
                return Integer.parseInt(subString);
            }
            takeChar();
        }
        if (index >= regex.length()) {
            throw new IllegalStateException("");
        }
        else {
            throw new IllegalStateException("Expected number, found " + regex.substring(initialIndex, index));
        }
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
                    throw new IllegalStateException("Parsing failed");
                }
                char next = takeChar();
                ranges.add(new CharRange(last, next));
                last = null;
            } else if (c == '(' || c == ')') {
                throw new IllegalStateException("Parsing failed");
            } else {
                if (last != null) {
                    characterSet.add(last);
                }
                last = c;
            }
        }
        throw new IllegalStateException("Parsing failed, unmatched [");
    }

    private Node buildNode(Set<Character> characterSet, Set<CharRange> ranges) {
        if (ranges.isEmpty() && characterSet.isEmpty()) {
            throw new IllegalStateException("Parsing failed: empty [] construction");
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
            for (int i = 0; i < sortedCharRanges.size(); i++) {
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

    private boolean peekStar() {
        if (index < regex.length() && regex.charAt(index) == '*') {
            index++;
            return true;
        }
        return false;
    }

    private boolean peekPlus() {
        if (index < regex.length() && regex.charAt(index) == '+') {
            index++;
            return true;
        }
        return false;
    }

    private boolean peekRightParen() {
        if (parenDepth > 0 && index < regex.length() && regex.charAt(index) == ')') {
            index++;
            parenDepth--;
            return true;
        }
        return false;
    }

    enum ParseContext {
        PAREN,
        ALTERNATION,
        RANGE,
        CHARS,
        COUNTED
    }
}
