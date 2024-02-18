package com.justinblank.strings;

import com.justinblank.strings.RegexAST.*;

import java.util.*;
import java.util.stream.Collectors;

class RegexParser {

    private int index = 0;
    private int charRangeDepth = 1;
    private String regex;
    private Stack<Node> nodes = new Stack<>();

    protected RegexParser(String regex) {
        this.regex = regex;
    }

    public static Node parse(String regex) {
        Objects.requireNonNull(regex, "regex string cannot be null");
        try {
            return new RegexParser(regex)._parse();
        }
        catch (RegexSyntaxException e) {
            throw e;
        }
        catch (Exception e) {
            // Any other exception is a bug, wrap and rethrow
            throw new RegexSyntaxException("Unexpected error while parsing regex '" + regex + "' ", e);
        }
    }

    private Node _parse() {
        while (index < regex.length()) {
            char c = takeChar();

            switch (c) {
                case '.':
                    nodes.push(new CharRangeNode((char) 0, (char) 65535));
                    break;
                case '^':
                    throw new RegexSyntaxException("'^' not supported yet");
                case '$':
                    throw new RegexSyntaxException("'$' not supported yet");
                case '(':
                    nodes.push(LParenNode.getInstance());
                    break;
                case '{':
                    if (nodes.isEmpty()) {
                        throw new RegexSyntaxException("Found '{' at position " + index + " with no preceding regex");
                    }
                    int left = consumeInt();
                    char next = takeChar();
                    if (next == '}') {
                        nodes.push(new CountedRepetition(nodes.pop(), left, left));
                        break;
                    }
                    else if (next != ',') {
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
                    buildCharSet().ifPresent(nodes::push);
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
                    nodes.push(new Union(last, null));
                    break;
                case '\\':
                    nodes.push(parseEscapeSequence());
                    break;
                case ')':
                    collapseParenNodes();
                    break;
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
            throw new RegexSyntaxException("Unbalanced '(' found parsing regex=" + regex);
        }
        while (!nodes.isEmpty()) {
            Node next = nodes.pop();
            if (next instanceof Union && ((Union) next).right == null) {
                Union union = (Union) next;
                node = new Union(union.left, node);
            }
            else if (next instanceof LiteralNode && node instanceof LiteralNode) {
                node = new LiteralNode(((LiteralNode) next).getLiteral() + ((LiteralNode) node).getLiteral());
            }
            else if (next instanceof LParenNode) {
                throw new RegexSyntaxException("Unbalanced '(' found parsing regex=" + regex);
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
            if (!(previous instanceof Union) && !(previous instanceof LParenNode)) {
                previous = nodes.pop();
                last = concatenate(previous, last);
            }
            else if (previous instanceof Union) {
                Union union = (Union) previous;
                if (union.right == null) {
                    nodes.pop();
                    last = new Union(union.left, last);
                }
                else {
                    last = new Concatenation(nodes.pop(), last);
                }
            }
            else if (previous instanceof LParenNode) {
                break;
            }
        }
        nodes.push(last);
    }

    private void collapseParenNodes() {
        assertNonEmpty("found unbalanced ')'");
        Node node = null;
        while (!(nodes.peek() instanceof LParenNode)) {
            Node previous = nodes.pop();
            if (node == null) {
                node = previous;
            }
            else if (previous instanceof Union) {
                Union union = (Union) previous;
                if (union.left != null && union.right != null) {
                    node = new Concatenation(union, node);
                    continue;
                }
                assertNonEmpty("found '|' with no preceding content");
                Node nextNext = nodes.peek();
                if (nextNext instanceof LParenNode) {
                    nodes.pop(); // Remove lParen
                    nodes.push(new Union(union.left, node));
                    return;
                }
                node = new Union(union.left, node);
            }
            else {
                node = concatenate(previous, node);
            }
            assertNonEmpty("found unbalanced ')'");
        }
        nodes.pop(); // remove the left paren
        if (node == null) {
            node = new LiteralNode(""); // this is needed for constructs like "()|abc"
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

    private Node parseEscapeSequence() {
        if (index >= regex.length()) {
            throw new RegexSyntaxException("'\\' character with nothing following it at index" + (index - 1));
        }
        char c = takeChar();
        switch (c) {
            case 'a': {
                return new CharRangeNode('\u0007', '\u0007');
            }
            case 'A': {
                throw new RegexSyntaxException("\\A not supported yet");
            }
            case 'B': {
                throw new RegexSyntaxException("\\B not supported yet");
            }
            case 'b': {
                throw new RegexSyntaxException("\\b not supported yet");
            }
            case 'c': {
                throw new RegexSyntaxException("\\c not supported yet");
            }
            case 'd': {
                return new CharRangeNode('0', '9');
            }
            case 'D': {
                return Union.complement(List.of(new CharRangeNode('0', '9')));
            }
            case 'e': {
                return new CharRangeNode('\u001B', '\u001B');
            }
            case 'f': {
                return new CharRangeNode('\u000C', '\u000C');
            }
            case 'G': {
                throw new RegexSyntaxException("\\G not supported yet");
            }
            case 'h': {
                return Union.ofChars(" \u00A0\u1680\u180e\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007\u2008\u2009\200a\u202f\u205f\u3000");
            }
            case 'p': {
                throw new RegexSyntaxException("\\p not supported yet");
            }
            case 's': {
                return Union.ofChars(" \t\n\u000B\f\r");
            }
            case 'S': {
                return Union.complement(" \t\n\u000B\f\r");
            }
            case 't': {
                return new CharRangeNode('\t', '\t');
            }
            case 'w': {
                var digits = new CharRangeNode('0', '9');
                var alpha1 = new CharRangeNode('a', 'z');
                var alpha2 = new CharRangeNode('A', 'Z');
                var underscore = new CharRangeNode('_', '_');
                return new Union(digits, new Union(underscore, new Union(alpha1, alpha2)));
            }
            case 'W': {
                var digits = new CharRangeNode('0', '9');
                var alpha1 = new CharRangeNode('a', 'z');
                var alpha2 = new CharRangeNode('A', 'Z');
                var underscore = new CharRangeNode('_', '_');
                return Union.complement(List.of(digits, underscore, alpha1, alpha2));
            }
            case 'x': {
                return parseHexadecimal();
            }
            case 'Z': {
                throw new RegexSyntaxException("\\Z not supported yet");
            }
            case 'z': {
                throw new RegexSyntaxException("\\z not supported yet");
            }
            case '0': {
                return parseOctal();
            }
            case '\\': {
                return new CharRangeNode('\\', '\\');
            }
            case '[': {
                if (charRangeDepth > 0) {
                    return new CharRangeNode('[', '[');
                }
            }
            case '|':
            case '(':
            case ')':
            case '$':
            case '*':
            case '?':
            case '+':
            case '{':
            case ':':
            case '^':
            case '.': {
                return new CharRangeNode(c, c);
            }
        }
        throw new RegexSyntaxException("Escape with unrecognized escaped character: '" + c + "'");
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
            throw new RegexSyntaxException("Expected number, found " + regex.substring(initialIndex, index) + " in regex " + regex);
        }
        throw new RegexSyntaxException("Expected number, found " + regex.substring(initialIndex, index) + " in regex " + regex);
    }

    private Node parseOctal() {
        int count = 0;
        var str = new StringBuilder();
        while (count < 3 && peekOctal()) {
            if (count == 2 && str.charAt(0) > '3') {
                break;
            }
            var c = takeChar();
            count++;
            str.append(c);
        }
        if (count == 0) {
            throw new RegexSyntaxException("Illegal octal escape at index " + (index - 1));
        }
        int i = Integer.decode("0" + str);
        return new CharRangeNode((char) i, (char) i);
    }

    private Node parseHexadecimal() {
        int count = 0;
        var str = new StringBuilder();
        while (count < 2 && peekHex()) {
            var c = takeChar();
            count++;
            str.append(c);
        }
        if (count != 2) {
            throw new RegexSyntaxException("Wrong number of hex chars: " + count);
        }
        int i = Integer.decode("0x" + str);
        return new CharRangeNode((char) i, (char) i);
    }

    private Optional<Node> buildCharSet() {
        charRangeDepth++;
        Set<Character> characterSet = new HashSet<>();
        Set<CharRange> ranges = new HashSet<>();
        Character last = null;
        int startingIndex = index;
        Node alternateNode = null;
        boolean complemented = false;
        while (index < regex.length()) {
            char c = takeChar();
            if (c == '^' && index == startingIndex + 1) {
                complemented = true;
            } else if (c == ']') {
                if (last != null) {
                    characterSet.add(last);
                }
                charRangeDepth--;
                var node = buildNode(characterSet, ranges, complemented);
                return withAlternate(node, alternateNode);
            } else if (c == '-') {
                if (index == regex.length()) {
                    throw new RegexSyntaxException("");
                }
                // TODO: find out actual semantics
                if (last == null) {
                    last = c;
                    continue;
                }
                char next = takeChar();
                if (next == '\\') {
                    if (peekChar('[')) {
                        next = takeChar();
                    }
                    else if (peekChar(']')) {
                        next = takeChar();
                    }
                    else if (peekChar('\\')) {
                        next = takeChar();
                    }
                }
                ranges.add(new CharRange(last, next));
                last = null;
            } else if (c == '[') {
                int currentIndex = index;
                Optional<Node> maybeNode = buildCharSet();
                if (maybeNode.isEmpty()) {
                    throw new RegexSyntaxException("Unbalanced [ token at index=" + currentIndex);
                }
                char next = takeChar();
                if (next != ']') {
                    throw new RegexSyntaxException("Unbalanced [ token at index=" + currentIndex);
                }
                charRangeDepth--;
                return withAlternate(maybeNode, alternateNode);
            } else if (c == '\\') {
                char next;
                if (peekChar('[') || peekChar(']') || peekChar('\\')) {
                    next = takeChar();
                    characterSet.add(next);
                    last = next;
                }
                else {
                    alternateNode = parseEscapeSequence();
                }
            } else {
                if (last != null) {
                    characterSet.add(last);
                }
                last = c;
            }
        }
        throw new RegexSyntaxException("Parsing failed, unmatched [");
    }

    private Optional<Node> withAlternate(Optional<Node> node, Node alternate) {
        var union = node.map(n -> {
            if (alternate != null) {
                return new Union(n, alternate);
            } else {
                return n;
            }
        }).orElse(alternate);
        return Optional.ofNullable(union);
    }

    private Optional<Node> buildNode(Set<Character> characterSet, Set<CharRange> ranges, boolean complemented) {
        if (ranges.isEmpty() && characterSet.isEmpty()) {
            return Optional.empty();
        } else if (characterSet.isEmpty() && ranges.size() == 1) {
            CharRange range = ranges.iterator().next();
            CharRangeNode rangeNode = new CharRangeNode(range);
            if (complemented) {
                return Optional.of(Union.complement(List.of(rangeNode)));
            }
            return Optional.of(rangeNode);
        } else if (ranges.isEmpty() && characterSet.size() == 1) {
            Character character = characterSet.iterator().next();
            CharRangeNode rangeNode = new CharRangeNode(character, character);
            if (complemented) {
                return Optional.of(Union.complement(List.of(rangeNode)));
            }
            return Optional.of(rangeNode);
        } else {
            return Optional.of(buildRanges(characterSet, ranges, complemented));
        }
    }

    private Node buildRanges(Set<Character> characterSet, Set<CharRange> ranges, boolean complemented) {
        List<CharRange> sortedCharRanges = buildSortedCharRanges(characterSet, ranges);
        if (sortedCharRanges.size() == 1) {
            CharRangeNode rangeNode = new CharRangeNode(sortedCharRanges.get(0));
            if (complemented) {
                return Union.complement(List.of(rangeNode));
            }
            return rangeNode;
        } else {
            if (complemented) {
                return Union.complement(sortedCharRanges.stream().map(CharRangeNode::new).collect(Collectors.toList()));
            }
            CharRangeNode first = new CharRangeNode(sortedCharRanges.get(0));
            CharRangeNode second = new CharRangeNode(sortedCharRanges.get(1));
            Node node = new Union(first, second);
            for (int i = 2; i < sortedCharRanges.size(); i++) {
                node = new Union(node, new CharRangeNode(sortedCharRanges.get(i)));
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

    private boolean peekChar(char c) {
        return (index < regex.length() && regex.charAt(index) == c);
    }

    private boolean peekOctal() {
        if (index < regex.length()) {
            char c = regex.charAt(index);
            return c >= '0' && c <= '7';
        }
        return false;
    }

    private boolean peekHex() {
        if (index < regex.length()) {
            char c = regex.charAt(index);
            if (isHexChar(c)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isHexChar(char c) {
        if (c >= '0' && c <= '9') {
            return true;
        }
        if (c >= 'A' && c <= 'F') {
            return true;
        }
        return false;
    }
}
