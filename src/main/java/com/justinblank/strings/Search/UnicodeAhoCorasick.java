package com.justinblank.strings.Search;

class UnicodeAhoCorasick implements SearchMethod {

    private final Trie trie;

    UnicodeAhoCorasick(Trie trie) {
        this.trie = trie;
    }

    @Override
    public int findIndex(String s) {
        int length = s.length();
        Trie current = this.trie;
        for (int i = 0; i < length; i++) {
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
                    return i - current.length;
                }
        }

        return -1;
    }
}
