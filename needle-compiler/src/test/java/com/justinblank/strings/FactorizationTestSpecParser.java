package com.justinblank.strings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FactorizationTestSpecParser {

    List<FactorizationTestSpec> readTests() throws Exception {
        var resource = this.getClass().getClassLoader().getResource("factorization.txt");
        var text = Files.readAllLines(Path.of(resource.toURI()));
        return text.stream()
                .filter(s -> !s.isBlank() && !s.startsWith("#"))
                .map(this::readSpec)
                .collect(Collectors.toList());
    }

    private FactorizationTestSpec readSpec(String s) {
        try {
            Chomp chomp = new Chomp(s);
            
            // First field: pattern (up to first '{' or whitespace before brace)
            String pattern = chomp.readPattern();
            chomp.consumeWhitespace();
            
            List<String> prefixes = parseSet(chomp);
            chomp.consumeWhitespace();
            
            List<String> suffixes = parseSet(chomp);
            chomp.consumeWhitespace();
            
            List<String> factors = parseSet(chomp);
            chomp.consumeWhitespace();
            
            List<String> all = parseSet(chomp);
            chomp.consumeWhitespace();
            
            List<String> requiredFactors = parseSet(chomp);
            chomp.consumeWhitespace();
            
            Optional<String> sharedPrefix = parseOptionalString(chomp);
            chomp.consumeWhitespace();
            
            Optional<String> sharedSuffix = parseOptionalString(chomp);

            return new FactorizationTestSpec(pattern, prefixes, suffixes, factors, all, requiredFactors, sharedPrefix, sharedSuffix);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to parse spec: " + s, e);
        }
    }

    private List<String> parseSet(Chomp chomp) {
        chomp.consumeWhitespace();
        
        if (!chomp.hasRemaining()) {
            return new ArrayList<>();
        }
        
        // Check for "null"
        String nullStr = chomp.peek("null");
        if (nullStr != null) {
            chomp.chop(nullStr.length());
            return null;
        }
        
        // Parse {A, B, C} format - read until balanced closing brace
        if (chomp.hasRemaining() && chomp.peekChar() == '{') {
            chomp.chop(1); // consume opening brace
            String inner = chomp.readBalancedContent('{', '}');
            chomp.chop(1); // consume closing brace
            
            if (inner.isEmpty()) {
                return new ArrayList<>(); // Empty set {}
            }
            
            // Split by comma and trim each element
            var parts = Arrays.stream(inner.split("\\s*,\\s*"))
                    .map(String::trim)
                    .collect(Collectors.toList());
            return parts;
        }
        
        // Read a single value (not in braces) - read until whitespace
        int start = chomp.position();
        while (chomp.hasRemaining()) {
            char c = chomp.peekChar();
            if (Character.isWhitespace(c)) {
                break;
            }
            chomp.chop(1);
        }
        String value = chomp.input.substring(start, chomp.position()).trim();
        
        if (value == null || value.isEmpty()) {
            return new ArrayList<>();
        }
        
        return Arrays.asList(value);
    }

    private Optional<String> parseOptionalString(Chomp chomp) {
        chomp.consumeWhitespace();
        
        if (!chomp.hasRemaining()) {
            return Optional.empty();
        }
        
        // Check for "null"
        String nullStr = chomp.peek("null");
        if (nullStr != null) {
            chomp.chop(nullStr.length());
            return Optional.empty();
        }
        
        // Read a single value - read until whitespace
        int start = chomp.position();
        while (chomp.hasRemaining()) {
            char c = chomp.peekChar();
            if (Character.isWhitespace(c)) {
                break;
            }
            chomp.chop(1);
        }
        String value = chomp.input.substring(start, chomp.position()).trim();
        
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        
        // Trim braces if present
        if (value.startsWith("{") && value.endsWith("}")) {
            value = value.substring(1, value.length() - 1).trim();
        }
        
        return Optional.of(value);
    }

    /**
     * A character-by-character parser that "chomps" through a string.
     * Provides methods to peek, read, and consume characters while maintaining position.
     */
    static class Chomp {
        private final String input;
        private int position = 0;

        Chomp(String input) {
            this.input = input != null ? input : "";
        }

        /** Check if there are remaining characters to read. */
        boolean hasRemaining() {
            return position < input.length();
        }

        /** Get the current position in the string. */
        int position() {
            return position;
        }

        /** Consume and discard n characters from the input. */
        void chop(int n) {
            position += n;
        }

        /** Read a single character without consuming it. */
        char peekChar() {
            if (position >= input.length()) {
                return '\0';
            }
            return input.charAt(position);
        }

        /** Peek ahead to see if the string contains the given prefix at current position. */
        String peek(String prefix) {
            int remaining = input.length() - position;
            if (remaining < prefix.length()) {
                return null;
            }
            String segment = input.substring(position, position + prefix.length());
            return segment.equals(prefix) ? prefix : null;
        }

        /** Read characters until we hit whitespace. */
        String readUntilWhitespace() {
            int start = position;
            while (position < input.length()) {
                char c = input.charAt(position);
                if (Character.isWhitespace(c)) {
                    break;
                }
                position++;
            }
            return input.substring(start, position);
        }

        /** Read content until balanced closing delimiter. */
        String readBalancedContent(char openDelim, char closeDelim) {
            StringBuilder result = new StringBuilder();
            int balance = 1;
            
            while (position < input.length() && balance > 0) {
                char c = input.charAt(position);
                if (c == openDelim) {
                    balance++;
                } else if (c == closeDelim) {
                    balance--;
                    if (balance == 0) {
                        // Don't include the closing delimiter
                        break;
                    }
                }
                result.append(c);
                position++;
            }
            
            return result.toString();
        }

        /** Skip over any whitespace characters. */
        void consumeWhitespace() {
            while (position < input.length()) {
                char c = input.charAt(position);
                if (!Character.isWhitespace(c)) {
                    break;
                }
                position++;
            }
        }

        /** Read the pattern field - up to first '{' or whitespace before it.
         * The pattern is followed by 2+ spaces then the factorization data starting with '{'.
         * For patterns with no braces (null values), stop at whitespace.
         */
        String readPattern() {
            int start = position;
            while (position < input.length()) {
                char c = input.charAt(position);
                if (c == ' ') {
                    // Check for 2+ spaces followed by a data field - this marks the end of pattern
                    int savedPos = position;
                    consumeWhitespace();
                    
                    boolean isEndOfPattern = false;
                    if (position - savedPos >= 2 && hasRemaining()) {
                        char nextChar = peekChar();
                        // End of pattern if we see '{', 'n' (null), or any non-whitespace
                        // Note: patterns don't contain spaces, so any non-ws after 2+ spaces is a data field
                        if (nextChar == '{') {
                            isEndOfPattern = true;
                        } else if (nextChar == 'n' && input.substring(position, Math.min(position + 4, input.length())).equals("null")) {
                            isEndOfPattern = true;
                        }
                    }
                    
                    // If we have 2+ spaces and a non-whitespace next char, that's end of pattern
                    if (!isEndOfPattern && position - savedPos >= 2 && hasRemaining() && !Character.isWhitespace(peekChar())) {
                        isEndOfPattern = true;
                    }
                    
                    if (isEndOfPattern) {
                        position = savedPos; // restore to first space
                        break;
                    }
                } else if (c == '{') {
                    // Check if followed by whitespace then another { (factorization data)
                    int savedPos = position;
                    position++;
                    consumeWhitespace();
                    
                    boolean isFactorizationStart = hasRemaining() && peekChar() == '{';
                    
                    if (isFactorizationStart) {
                        // Two consecutive brace fields - this is pattern followed by factorization
                        position = savedPos;
                        break;
                    } else {
                        // Reset and continue reading the pattern
                        position = savedPos + 1;  // Move past the { we just saw
                    }
                } else {
                    position++;
                }
            }
            return input.substring(start, position).trim();
        }
    }
}
