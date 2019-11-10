package com.justinblank.strings.Search;

class ASCIITrie {

    protected int length;
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

    /**
     * // TODO: this seems like a hack, and unclear if it will work in all cases...
     * Get the length of the node. Note that where the node is accepting, the length may not be depth of the node in
     * the trie, but rather the length of the string that the node accepts. So, for instance, in the trie constructed from "a", "aaa", the
     * node reached after encountering "aa" will have length 1, because it only matched "a".
     * @return the length of the string matched by the node, if any
     */
    int length() {
        return length;
    }
}
