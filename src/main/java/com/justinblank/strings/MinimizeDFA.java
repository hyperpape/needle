package com.justinblank.strings;

import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

// This implements Hopcroft's Minimization Algorithm
class MinimizeDFA {

    private int state = 1; // Account for the fact that root will be 0
    private DFA root;

    protected static DFA minimizeDFA(DFA dfa) {
        MinimizeDFA minimizer = new MinimizeDFA();
        Map<DFA, Set<DFA>> partition = createPartition(dfa);
        Map<Set<DFA>, DFA> newDFAMap = new IdentityHashMap<>();

        for (DFA original : dfa.allStates()) {
            DFA minimized = minimizer.getOrCreateMinimizedState(partition, newDFAMap, original);
            for (Pair<CharRange, DFA> transition : original.getTransitions()) {
                DFA next = minimizer.getOrCreateMinimizedState(partition, newDFAMap, transition.getRight());
                minimized.addTransition(transition.getLeft(), next);
            }
        }
        DFA minimal = newDFAMap.get(partition.get(dfa));
        assert minimal.statesCount() == new HashSet<>(partition.values()).size();
        minimal.checkRep();
        return minimal;
    }

    private DFA getOrCreateMinimizedState(Map<DFA, Set<DFA>> partition, Map<Set<DFA>, DFA> newDFAMap, DFA original) {
        Set<DFA> set = partition.get(original);
        DFA minimized = newDFAMap.get(set);
        if (minimized == null) {
            if (root == null) {
                assert original.isRoot();
                minimized = DFA.root(original.isAccepting());
                root = minimized;
            }
            else {
                minimized = new DFA(root, original.isAccepting(), state++);
            }
            newDFAMap.put(set, minimized);
        }
        return minimized;
    }

    protected static Map<DFA, Set<DFA>> createPartition(DFA dfa) {
        Map<DFA, Set<DFA>> partition = initialPartition(dfa);
        boolean changed = true;
        while (changed) {
            changed = false;
            Set<Set<DFA>> seen = new HashSet<>();
            for (Set<DFA> set : partition.values()) {
                if (set.size() > 1 && !seen.contains(set)) {
                    seen.add(set);
                    Optional<List<Set<DFA>>> splitted = split(partition, set);
                    if (splitted.isPresent()) {
                        changed = true;
                        for (Set<DFA> part : splitted.get()) {
                            for (DFA thing : part) {
                                partition.put(thing, part);
                            }
                        }
                        break;
                    }
                }
            }
        }
        assert allNonEmpty(partition);
        return partition;
    }

    private static Map<DFA, Set<DFA>> initialPartition(DFA dfa) {
        Map<DFA, Set<DFA>> partition = new HashMap<>();
        Set<DFA> accepting = dfa.acceptingStates();
        Set<DFA> nonAccepting = new HashSet<>(dfa.allStates());
        nonAccepting.removeAll(accepting);
        if (!accepting.isEmpty()) {
            for (DFA acceptingDFA : accepting) {
                partition.put(acceptingDFA, accepting);
            }
        }
        if (!nonAccepting.isEmpty()) {
            for (DFA nonAcceptingDFA : nonAccepting) {
                partition.put(nonAcceptingDFA, nonAccepting);
            }
        }
        return partition;
    }

    protected static boolean allNonEmpty(Map<DFA, Set<DFA>> partition) {
        for (Set<DFA> subset : partition.values()) {
            if (subset.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    protected static Optional<List<Set<DFA>>> split(Map<DFA, Set<DFA>>partition, Set<DFA> set) {
        Iterator<DFA> dfa = set.iterator();
        DFA first = dfa.next();
        Set<DFA> other = new HashSet<>(set.size());
        for (DFA second : set) {
            if (second != first && !equivalent(partition, first, second)) {
                other.add(second);
            }
        }
        if (!other.isEmpty()) {
            List<Set<DFA>> splitted = new ArrayList<>();
            splitted.add(other);
            Set<DFA> firstEquivalents = new HashSet<>(set);
            firstEquivalents.removeAll(other);
            splitted.add(firstEquivalents);
            return Optional.of(splitted);
        }
        return Optional.empty();
    }

    protected static boolean equivalent(Map<DFA, Set<DFA>> partition, DFA first, DFA second) {
        if (first.getTransitions().size() != second.getTransitions().size()) {
            return false;
        }
        Iterator<Pair<CharRange, DFA>> firstIter = first.getTransitions().iterator();
        Iterator<Pair<CharRange, DFA>> secondIter = second.getTransitions().iterator();
        while (firstIter.hasNext()) {
            Pair<CharRange, DFA> firstTransition = firstIter.next();
            Pair<CharRange, DFA> secondTransition = secondIter.next();
            if (!firstTransition.getLeft().equals(secondTransition.getLeft())) {
                return false;
            }
            else if (firstTransition.getRight() != secondTransition.getRight()) {
                if (partition.get(firstTransition.getRight()) != partition.get(secondTransition.getRight())) {
                    return false;
                }
            }
        }
        return true;
    }
}
