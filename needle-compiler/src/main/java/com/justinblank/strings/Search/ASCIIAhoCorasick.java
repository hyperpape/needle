package com.justinblank.strings.Search;

import com.justinblank.strings.MatchResult;

// Note that the name refers to the patterns this class matches, not the strings that it can search in.
class ASCIIAhoCorasick implements SearchMethod {

    private final ASCIITrie trie;
    private final ASCIITrie partialTrie;

    ASCIIAhoCorasick(ASCIITrie trie, ASCIITrie partialTrie) {
        this.trie = trie;
        this.partialTrie = partialTrie;
    }

    public MatchResult find(String s, int start, int end, boolean anchored) {
        if (anchored) {
            return find(partialTrie, s, start, end);
        }
        else {
            return find(trie, s, start, end);
        }
    }

    private MatchResult find(ASCIITrie trie, String s, int start, int end) {
        SearchMethodUtil.checkIndices(s, start, end);
        ASCIITrie current = trie;
        int lastEnd = -1;
        int lastStart = -1;
        if (current.accepting) {
            lastStart = 0;
        }
        for (int i = start; i < end; i++) {
            if (i > start && current == trie) {
                if (lastEnd > -1) {
                    return MatchResult.success(lastStart, lastEnd + 1);
                }
            }
            char c = s.charAt(i);
            if (((int) c) > 127) {
                current = trie;
            }
            else {
                ASCIITrie next = current.followers[(int) c];
                if (next == null) {
                    next = this.trie;
                }
                current = next;
                if (current != null && current.accepting) {
                    int potentialLastStart = i - current.length + 1;
                    if (lastStart == -1 || potentialLastStart <= lastStart) {
                        lastEnd = i;
                        lastStart = potentialLastStart;
                    }
                }
            }
        }
        if (lastEnd != -1) {
            return MatchResult.success(lastStart, lastEnd + 1);
        }
        else if (current != null && current.accepting) {
            return MatchResult.success(0, 0);
        }
        return MatchResult.failure();
    }
}
