package com.justinblank.strings.Search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class UnicodeAhoCorasickBuilder{

    private Trie root;
    private List<Trie> nodes = new ArrayList<>();

    protected static SearchMethod buildAhoCorasick(Collection<String> strings) {
        return new UnicodeAhoCorasickBuilder().build(strings);
    }

    protected SearchMethod build(Collection<String> strings) {
        Trie trie = new Trie(0);
        root = trie;
        root.root = root;
        nodes.add(root);
        buildTrieStructure(strings, trie);
        nodes.sort(Comparator.comparing(Trie::length));
        addSuppliers();
        return new UnicodeAhoCorasick(trie);
    }

    private void buildTrieStructure(Collection<String> strings, Trie trie) {
        for (String s : strings) {
            Trie current = trie;
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                Trie next = current.next(c);
                if (next == null) {
                    next = new Trie(i + 1);
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


    private void addSuppliers() {
        for (Trie node : nodes) {
            node.followers.forEach((key, value) -> {
                addSupplier(node, value, key);
            });
        }
        assert allHaveSuppliers(nodes);
    }

    private boolean allHaveSuppliers(List<Trie> nodes) {
        for (Trie node : nodes) {
            assert node == root || node.supplier != null;
        }
        return true;
    }

    private void addSupplier(Trie trie, Trie node, char transition) {
        if (trie == root) {
            node.supplier = root;
        } else {
            Trie down = trie.supplier;
            while (down != null) {
                if (down.next(transition) != null) {
                    node.supplier = down.next(transition);
                    if (node.supplier.accepting && !node.accepting) {
                        node.accepting = true;
                        node.length = node.supplier.length;
                    }
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
