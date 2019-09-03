package com.justinblank.strings.Search;

class ASCIITrie {

    protected final int length;
    protected boolean accepting;
    final ASCIITrie[] followers = new ASCIITrie[128];
    ASCIITrie supplier;
    ASCIITrie root;

    ASCIITrie(int length) {
        this.length = length;
    }

    void addFollower(char c, ASCIITrie trie) {
        this.followers[(int) c] = trie;
    }

    void markAccepting() {
        this.accepting = true;
    }
}
