package com.justinblank.strings.Search;

// Note that the name refers to the patterns this class works with. It should match non-ASCII strings.
class ASCIIAhoCorasick implements SearchMethod {

    private final ASCIITrie trie;

    ASCIIAhoCorasick(ASCIITrie trie) {
        this.trie = trie;
    }

    @Override
    public int findIndex(String s) {
        int length = s.length();
        ASCIITrie current = this.trie;
        for (int i = 0; i < length; i++) {
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
                if (current.accepting) {
                    return i - current.length;
                }
            }
        }

        return -1;
    }
}
