package com.justinblank.strings.Search;

import com.justinblank.strings.MatchResult;

class UnicodeAhoCorasick implements SearchMethod {

    private final Trie trie;

    UnicodeAhoCorasick(Trie trie) {
        this.trie = trie;
    }

    public boolean matches(String s) {
        // TODO: this isn't actually good for perf...could search entire string for matching
        MatchResult result = find(s, 0, s.length(), true);
        return result.matched && result.start == 0 && result.end == s.length();
    }

    @Override
    public MatchResult find(String s) {
        return find(s, 0, s.length());
    }

    @Override
    public MatchResult find(String s, int start, int end) {
        return find(s, start, end, false);
    }

    public MatchResult find(String s, int start, int end, boolean anchored) {
        SearchMethodUtil.checkIndices(s, start, end);
        Trie current = this.trie;
        int lastStart = -1;
        int lastEnd = -1;
        for (int i = start; i < end; i++) {
            if (anchored && i > start && current == trie) {
                if (lastEnd > -1) {
                    return MatchResult.success(lastStart, lastEnd);
                }
                return MatchResult.failure();
            }
            char c = s.charAt(i);
                Trie next = current.next(c);
                while (next == null) {
                    current = current.supplier;
                    if (current != null && current != trie) {
                        next = current.next(c);
                    }
                    else {
                        next = trie.next(c);
                        if (next == null) {
                            next = trie;
                        }
                    }
                }
                current = next;
                if (current.accepting) {
                    int potentialLastStart = i - current.length + 1;
                    if (lastStart == -1 || potentialLastStart <= lastStart) {
                        lastEnd = i;
                        lastStart = potentialLastStart;
                    }
                }
        }
        if (lastEnd > -1) {
            return MatchResult.success(lastStart, lastEnd + 1);
        }

        return MatchResult.failure();
    }
}
