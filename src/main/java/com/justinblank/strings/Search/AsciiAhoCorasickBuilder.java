package com.justinblank.strings.Search;

import java.util.ArrayList;
import java.util.List;

public class AsciiAhoCorasickBuilder {

    ASCIITrie root;
    private List<ASCIITrie> nodes = new ArrayList<>();

    protected static SearchMethod buildAhoCorasick(List<String> strings) {
        return new AsciiAhoCorasickBuilder().build(strings);
    }

    protected SearchMethod build(List<String> strings) {
        ASCIITrie trie = new ASCIITrie(0);
        root = trie;
        root.root = root;
        nodes.add(root);
        buildTrieStructure(strings, trie);
        addFullTransitions();
        return new ASCIIAhoCorasick(trie);
    }

    private void buildTrieStructure(List<String> strings, ASCIITrie trie) {
        for (String s : strings) {
            ASCIITrie current = trie;
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                ASCIITrie next = current.followers[(int) c];
                if (next == null) {
                    next = new ASCIITrie(i);
                    nodes.add(next);
                    next.root = root;
                    current.addFollower(c, next);
                }
                if (i == s.length() - 1) {
                    next.markAccepting();
                }
                current = next;
            }
        }
    }


    private void addFullTransitions() {
        for (ASCIITrie trie : nodes) {
            for (int c = 0; c < 128; c++) {
                if (trie.followers[c] != null) {
                    addSupplier(trie, trie.followers[c], c);
                }
            }
        }
        assert allHaveSuppliers(nodes);
        nodes.stream().forEach(this::addFullTransitions);
    }

    private boolean allHaveSuppliers(List<ASCIITrie> nodes) {
        for (ASCIITrie node : nodes) {
            assert node == root || node.supplier != null;
        }
        return true;
    }

    private void addFullTransitions(ASCIITrie trie) {
        for (int c = 0; c < 128; c++) {
            if (trie.followers[c] == null) {
                if (trie == root) {
                    trie.followers[c] = trie;
                } else {
                    trie.followers[c] = trie.supplier.followers[c];
                }
            }
        }
    }

    private void addSupplier(ASCIITrie trie, ASCIITrie node, int transition) {
        if (trie == root) {
            node.supplier = root;
        } else {
            ASCIITrie down = trie.supplier;
            while (down != null) {
                if (down.followers[transition] != null) {
                    node.supplier = down;
                    break;
                }
                down = down.supplier;
            }
            if (node.supplier == null) {
                node.supplier = root;
            }
        }
    }
}
