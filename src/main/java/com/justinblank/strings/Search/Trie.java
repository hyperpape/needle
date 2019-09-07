package com.justinblank.strings.Search;

import java.util.TreeMap;

class Trie {

    protected int length;
    protected boolean accepting;
    protected Trie root;
    protected Trie supplier;
    protected TreeMap<Character, Trie> followers = new TreeMap<>();

    protected Trie(int length) {
        this.length = length;
    }

    protected Trie next(char c) {
        return this.followers.get(c);
    }

    void addFollower(char c, Trie trie) {
        this.followers.put(c, trie);
    }

    void markAccepting() {
        this.accepting = true;
    }
}
