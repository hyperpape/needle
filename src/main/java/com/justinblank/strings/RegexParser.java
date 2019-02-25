package com.justinblank.strings;

import com.justinblank.strings.RegexAST.*;

import java.util.*;

public class RegexParser {

    private int index = 0;
    private int parenDepth = 0;
    private String regex;

    protected RegexParser(String regex) {
        this.regex = regex;
    }

    public static Node parse(String regex) {
        return new RegexParser(regex)._parse();
    }

    protected Node _parse() {
        Node last = null;
        while (index < regex.length()) {
            char c = takeChar();
            Node current = null;

            switch (c) {
                case '(':
                    parenDepth++;
                    current = _parse();
                    if (parenDepth > 0 && peekRightParen()) {
                        return current;
                    }
                    if (peekStar()) {
                        current = new Repetition(current);
                    }
                    else if (peekPlus()) {
                        current = new Concatenation(current, new Repetition(current));
                    }
                    if (last != null) {
                        last = new Concatenation(last, current);
                    } else {
                        last = current;
                    }
                    break;
                case '{':
                    int left = consumeInt();
                    char next = takeChar();
                    if (next != ',') {
                        throw new IllegalStateException("Expected ',' at " + index + ", but found " + next);
                    }
                    int right = consumeInt();
                    last = new CountedRepetition(last, left, right);
                    // TODO: error handling
                    next = takeChar();
                    if (next != '}') {
                        throw new IllegalStateException("Expected '}' at " + index + ", but found " + next);
                    }
                    break;
                case '?':
                    if (last == null) {
                        throw new IllegalStateException("? cannot be the first character in a regex");
                    }
                    last = new CountedRepetition(last, 0, 1);
                    break;
                case '[':
                    current = buildCharSet();
                    if (peekStar()) {
                        current = new Repetition(current);
                    }
                    else if (peekPlus()) {
                        current = new Concatenation(current, new Repetition(current));
                    }
                    if (last != null) {
                        last = new Concatenation(last, current);
                    } else {
                        last = current;
                    }
                    break;
                case '+':
                    last = new Concatenation(last, new Repetition(last));
                    break;
                case '*':
                    last = new Repetition(last);
                    if (peekRightParen()) {
                        return last;
                    }
                    break;
                case '|':
                    last = new Alternation(last, _parse());
                    break;
                case '}':
                    throw new IllegalStateException("Encountered unmatched '}' char at index" + index);
                case ')':
                    if (parenDepth == 0) {
                        throw new IllegalStateException("Encountered unmatched ')' char at index " + index);
                    }
                    else if (last != null) {
                        return last;
                    }
                    // TODO: this fall-through is intentional?
                default:
                    current = new CharRangeNode(c, c);
                    if (last != null) {
                        last = new Concatenation(last, current);
                    } else {
                        last = current;
                    }
                    if (peekRightParen()) {
                        return last;
                    }
            }
        }
        if (parenDepth > 0) {
            throw new IllegalStateException("Parsing failed: unmatched (");
        }
        // TODO: handle empty regex
        else if (last == null) {
            throw new IllegalStateException("Parsing failed: empty regex");
        }
        return last;
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
}
