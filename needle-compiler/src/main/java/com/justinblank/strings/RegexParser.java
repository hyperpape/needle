package com.justinblank.strings;

import com.justinblank.strings.RegexAST.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

class RegexParser {

    /**
     * The set of single character escapes not supported by this library that are recognized by the standard library.
     * Most of these should be supported in the future, but the backreferences will likely never be supported because
     * of limitations on efficiently matching them.
     */
    public static final char[] UNSUPPORTED_STANDARD_LIBRARY_ESCAPE_CHARACTERS = new char[]{'A', 'B', 'G', 'Q', 'R', 'X', 'Z', 'b', 'z', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    private static final String RELUCTANT_QUANTIFIERS_ARE_NOT_SUPPORTED = "Reluctant quantifiers are not supported";
    private static final String POSSESSIVE_QUANTIFIERS_ARE_NOT_SUPPORTED = "Possessive quantifiers are not supported";


    private int index = 0;
    private int charRangeDepth = 1;
    private final boolean dotAll;
    private final boolean caseInsensitive;
    private final boolean unicodeCaseInsensitive;

    private final String regex;
    private final Stack<Node> nodes = new Stack<>();

    protected RegexParser(String regex, int flags) {
        this.regex = regex;
        this.dotAll = (flags & Pattern.DOTALL) != 0;
        this.caseInsensitive = (flags & Pattern.CASE_INSENSITIVE) != 0;
        this.unicodeCaseInsensitive = (flags & Pattern.UNICODE_CASE) != 0;
    }

    public static Node parse(String regex) {
        return parse(regex, 0);
    }

    public static Node parse(String regex, int flags) {
        Objects.requireNonNull(regex, "regex string cannot be null");
        try {
            return new RegexParser(regex, flags)._parse();
        }
        catch (RegexSyntaxException e) {
            throw e;
        }
        catch (Exception e) {
            // Any other exception is a bug, wrap and rethrow
            throw new RegexSyntaxException("Unknown error while parsing regex '" + regex + "' ", e);
        }
    }

    private Node _parse() {
        while (index < regex.length()) {
            char c = takeChar();

            switch (c) {
                case '.':
                    if (dotAll) {
                        nodes.push(new CharRangeNode(CharRange.ALL_CHARS));
                    }
                    else {
                        nodes.push(new Union(new CharRangeNode((char) 0, '\u0009'), new CharRangeNode('\u000B', '\uFFFF')));
                    }
                    break;
                case '^':
                    throw parseError("'^' not supported yet");
                case '$':
                    throw parseError("'$' not supported yet");
                case '(':
                    nodes.push(LParenNode.getInstance());
                    break;
                case '{':
                    if (nodes.isEmpty()) {
                        throw parseError("Found '{' at position " + index + " with no preceding regex.");
                    }
                    int left = consumeInt();
                    char next = takeChar();
                    if (next == '}') {
                        nodes.push(new CountedRepetition(nodes.pop(), left, left));
                        if (peekChar('?')) {
                            throw parseError(RELUCTANT_QUANTIFIERS_ARE_NOT_SUPPORTED);
                        }
                        if (peekChar('+')) {
                            throw parseError(POSSESSIVE_QUANTIFIERS_ARE_NOT_SUPPORTED);
                        }
                        break;
                    }
                    else if (next != ',') {
                        throw parseError("Expected ',' at " + index + ", but found " + next);
                    }
                    int right = consumeInt();
                    nodes.push(new CountedRepetition(nodes.pop(), left, right));
                    next = takeChar();
                    if (next != '}') {
                        throw parseError("Found unclosed brackets at position " + index);
                    }
                    if (peekChar('?')) {
                        throw parseError(RELUCTANT_QUANTIFIERS_ARE_NOT_SUPPORTED);
                    }
                    if (peekChar('+')) {
                        throw parseError(POSSESSIVE_QUANTIFIERS_ARE_NOT_SUPPORTED);
                    }
                    break;
                case '?':
                    if (nodes.isEmpty()) {
                        throw parseError("");
                    }
                    if (peekChar('?')) {
                        throw parseError(RELUCTANT_QUANTIFIERS_ARE_NOT_SUPPORTED);
                    }
                    if (peekChar('+')) {
                        throw parseError(POSSESSIVE_QUANTIFIERS_ARE_NOT_SUPPORTED);
                    }
                    nodes.push(new CountedRepetition(nodes.pop(), 0, 1));
                    break;
                case '[':
                    buildCharSet().ifPresent(nodes::push);
                    break;
                case '+':
                    if (nodes.isEmpty()) {
                        throw parseError("Found '+' with no preceding regex");
                    }
                    if (peekChar('?')) {
                        throw parseError(RELUCTANT_QUANTIFIERS_ARE_NOT_SUPPORTED);
                    }
                    if (peekChar('+')) {
                        throw parseError(POSSESSIVE_QUANTIFIERS_ARE_NOT_SUPPORTED);
                    }
                    Node lastNode = nodes.pop();
                    nodes.push(concatenate(lastNode, new Repetition(lastNode)));
                    break;
                case '*':
                    if (nodes.isEmpty()) {
                        throw parseError("Found '*' with no preceding regex");
                    }
                    if (peekChar('?')) {
                        throw parseError(RELUCTANT_QUANTIFIERS_ARE_NOT_SUPPORTED);
                    }
                    if (peekChar('+')) {
                        throw parseError(POSSESSIVE_QUANTIFIERS_ARE_NOT_SUPPORTED);
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
                    if (caseInsensitive) {
                        if (unicodeCaseInsensitive) {
                            Set<Character> characters = new HashSet<>();
                            addCaseInsensitiveMatches(characters, c);
                            if (characters.size() > 1) {
                                Node union = null;
                                for (var caseChar : characters) {
                                    if (union == null) {
                                        union = LiteralNode.fromChar(caseChar);
                                    }
                                    else {
                                        union = Union.of(union, LiteralNode.fromChar(caseChar));
                                    }
                                }
                                nodes.push(union);
                            }
                            else {
                                var node = LiteralNode.fromChar(c);
                                nodes.push(node);
                            }
                        } else {
                            var node = LiteralNode.fromChar(c);
                            if ('A' <= c && c <= 'Z') {
                                char other = (char) (((int) c) + 32);
                                nodes.push(new Union(node, LiteralNode.fromChar(other)));
                            } else if ('a' <= c && c <= 'z') {
                                char other = (char) (((int) c) - 32);
                                nodes.push(new Union(node, LiteralNode.fromChar(other)));
                            } else {
                                nodes.push(node);
                            }
                        }
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
            throw parseError("Unbalanced '(' found at index=" + index);
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
                throw parseError("Unbalanced '(' found at index=" + index);
            }
            else {
                node = concatenate(next, node);
            }
        }
        // node = concatenateAllLiterals(node);
        return node;
    }

    private static void addCaseInsensitiveMatches(Set<Character> characters, char c) {
        characters.add(c);
        char lower = Character.toLowerCase(Character.toUpperCase(c));
        char candidate = Character.MIN_VALUE;
        if (lower != Character.toUpperCase(c)) {
            // TODO: this could probably be a lot faster--I'm told RE2 uses some precomputed ranges
            // to speed it up
            while (candidate < Character.MAX_VALUE) {
                if (lower == Character.toLowerCase(Character.toUpperCase(candidate))) {
                    characters.add(candidate);
                }
                candidate++;
            }
        }
    }

    private RegexSyntaxException parseError(String message) {
        return new RegexSyntaxException(message + ". Regex=" + regex);
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
                    last = Concatenation.concatenate(nodes.pop(), last);
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
                    node = Concatenation.concatenate(union, node);
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
        return Concatenation.concatenate(next, node);
    }

    private void assertNonEmpty(String s) {
        if (nodes.isEmpty()) {
            throw parseError(s);
        }
    }

    private Node parseEscapeSequence() {
        if (index >= regex.length()) {
            throw parseError("'\\' character with nothing following it at index" + (index - 1));
        }
        char c = takeChar();
        switch (c) {
            case 'a': {
                return new CharRangeNode('\u0007', '\u0007');
            }
            case 'A': {
                throw parseError("\\A not supported yet");
            }
            case 'B': {
                throw parseError("\\B not supported yet");
            }
            case 'b': {
                throw parseError("\\b not supported yet");
            }
            case 'c': {
                throw parseError("\\c not supported yet");
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
                throw parseError("\\G not supported yet");
            }
            case 'H': {
                return Union.complement(" \t\u00A0\u1680\u180e\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007\u2008\u2009\u200a\u202f\u205f\u3000");
            }
            case 'h': {
                return Union.ofChars(" \t\u00A0\u1680\u180e\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007\u2008\u2009\u200a\u202f\u205f\u3000");
            }
            case 'n': {
                return new CharRangeNode('\n', '\n');
            }
            case 'p': {
                throw parseError("\\p not supported yet");
            }
            case 'r': {
                return new CharRangeNode('\r', '\r');
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
            case 'V': {
                return Union.complement("\n\u000B\f\r\u0085\u2028\u2029");
            }
            case 'v': {
                return Union.ofChars("\n\u000B\f\r\u0085\u2028\u2029");
            }
            case 'Z': {
                throw parseError("\\Z not supported yet");
            }
            case 'z': {
                throw parseError("\\z not supported yet");
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
        if (c >= '1' && c <= '9') {
            throw parseError("Can't parse \\" + c + ". Backreferences are not supported");
        }
        if (c < 'A') {
            return new CharRangeNode(c, c);
        }
        else if (c > 'Z' && c < 'a') {
            return new CharRangeNode(c, c);
        }
        else if (c > 'z') {
            return new CharRangeNode(c, c);
        }
        throw parseError("Escape with unrecognized escaped character: '" + c + "'");
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
            throw parseError("Expected number, found " + regex.substring(initialIndex, index) + " in regex " + regex);
        }
        throw parseError("Expected number, found " + regex.substring(initialIndex, index) + " in regex " + regex);
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
            throw parseError("Illegal octal escape at index " + (index - 1));
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
            throw parseError("Wrong number of hex chars: " + count);
        }
        int i = Integer.decode("0x" + str);
        return new CharRangeNode((char) i, (char) i);
    }

    private Optional<Node> buildCharSet() {
        charRangeDepth++;
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
                    ranges.add(CharRange.of(last, last));
                }
                charRangeDepth--;
                var node = buildNode(ranges, complemented);
                return withAlternate(node, alternateNode);
            } else if (c == '-') {
                if (index == regex.length()) {
                    throw parseError("Unterminated character range");
                }
                else if (peekChar(']')) {
                    if (last != null) {
                        ranges.add(CharRange.of(last, last));
                    }
                    last = '-';
                    continue;
                }
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
                if (next < last) {
                    throw parseError("Start of range must be less than the end, start=" + last + ", end=" + next);
                }
                var range = CharRange.of(last, next);
                if (caseInsensitive) {
                    if (unicodeCaseInsensitive) {
                        Set<Character> characters = new HashSet<>();
                        for (char rangeChar = last; rangeChar <= next; rangeChar++) {
                            addCaseInsensitiveMatches(characters, rangeChar);
                        }
                        // This should get normalized later
                        for (char rangeChar : characters) {
                            ranges.add(CharRange.of(rangeChar, rangeChar));
                        }
                    } else {
                        if (next < 'A' || 'z' < last) {
                            ranges.add(range);
                        } else {
                            var upperCaseRange = range.intersection(CharRange.of('A', 'Z'));
                            var lowerCaseRange = range.intersection(CharRange.of('a', 'z'));
                            upperCaseRange.ifPresent(r -> {
                                ranges.add(r);
                                ranges.add(r.translate(32));
                            });
                            lowerCaseRange.ifPresent(r -> {
                                ranges.add(r);
                                ranges.add(r.translate(-32));
                            });
                            ranges.add(range);
                        }
                    }
                }
                else {
                    ranges.add(range);
                }
                last = null;
            } else if (c == '[') {
                if (!ranges.isEmpty()) {
                    Node rangeNode = null;
                    for (var range : ranges) {
                        if (rangeNode == null) {
                            rangeNode = new CharRangeNode(range);
                        }
                        else {
                            rangeNode = new Union(rangeNode, new CharRangeNode(range));
                        }
                    }
                    ranges.clear();
                    alternateNode = withAlternate(Optional.of(rangeNode), alternateNode).orElse(null);
                }
                int currentIndex = index;
                Optional<Node> maybeNode = buildCharSet();
                if (maybeNode.isEmpty()) {
                    throw parseError("Unbalanced [ token at index=" + currentIndex);
                }
                if (peekChar(']')) {
                    takeChar();
                    charRangeDepth--;
                    return withAlternate(maybeNode, alternateNode);
                }
                else {
                    alternateNode = withAlternate(maybeNode, alternateNode).orElse(null);
                }
            } else if (c == '\\') {
                char next;
                if (peekChar('[') || peekChar(']') || peekChar('\\')) {
                    next = takeChar();
                    ranges.add(CharRange.of(next, next));
                    last = next;
                }
                else {
                    alternateNode = parseEscapeSequence();
                }
            } else {
                if (last != null) {
                    ranges.add(CharRange.of(last, last));
                }
                last = c;
            }
        }
        throw parseError("Parsing failed, unmatched [");
    }

    private Optional<Node> withAlternate(Optional<Node> node, Node alternate) {
        var union = node.map(n -> {
            if (alternate != null) {
                return new Union(alternate, n);
            } else {
                return n;
            }
        }).orElse(alternate);
        return Optional.ofNullable(union);
    }

    private Optional<Node> buildNode(Set<CharRange> ranges, boolean complemented) {
        if (ranges.isEmpty()) {
            return Optional.empty();
        } else if (ranges.size() == 1) {
            CharRange range = ranges.iterator().next();
            CharRangeNode rangeNode = new CharRangeNode(range);
            if (complemented) {
                return Optional.of(Union.complement(List.of(rangeNode)));
            }
            return Optional.of(rangeNode);
        } else {
            return Optional.of(buildRanges(ranges, complemented));
        }
    }

    private Node buildRanges(Set<CharRange> ranges, boolean complemented) {
        List<CharRange> sortedCharRanges = CharRange.compact(new ArrayList<>(ranges));
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
