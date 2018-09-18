package com.justinblank.strings;

import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

// This implements Hopcroft's Minimization Algorithm
class MinimizeDFA {

    public static DFA minimizeDFA(DFA dfa) {
        Set<Set<DFA>> partition = createPartition(dfa);
        Map<Set<DFA>, DFA> newDFAMap = new IdentityHashMap<>();

        for (DFA original : dfa.allStates()) {
            DFA minimized = getOrCreateMinimizedState(partition, newDFAMap, original);
            for (Pair<CharRange, DFA> transition : original.getTransitions()) {
                DFA next = getOrCreateMinimizedState(partition, newDFAMap, transition.getRight());
                minimized.addTransition(transition.getLeft(), next);
            }
        }
        DFA minimal = newDFAMap.get(target(partition, dfa));
        assert minimal.allStates().size() == partition.size();
        return minimal;
    }

    private static DFA getOrCreateMinimizedState(Set<Set<DFA>> partition, Map<Set<DFA>, DFA> newDFAMap, DFA original) {
        Set<DFA> set = target(partition, original);
        DFA minimized = newDFAMap.get(set);
        if (minimized == null) {
            minimized = new DFA(original.isAccepting());
            newDFAMap.put(set, minimized);
        }
        return minimized;
    }

    protected static Set<Set<DFA>> createPartition(DFA dfa) {
        Set<Set<DFA>> partition = new HashSet<>();
        Set<DFA> accepting = dfa.acceptingStates();
        Set<DFA> nonAccepting = new HashSet<>(dfa.allStates());
        nonAccepting.removeAll(accepting);
        partition.add(accepting);
        partition.add(nonAccepting);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Set<DFA> set : partition) {
                Optional<List<Set<DFA>>> splitted = split(partition, set);
                if (splitted.isPresent()) {
                    changed = true;
                    partition.remove(set);
                    partition.addAll(splitted.get());
                    break;
                }
            }
        }
        assert allNonEmpty(partition);
        return partition;
    }

    protected static boolean allNonEmpty(Set<Set<DFA>> partition) {
        for (Set<DFA> subset : partition) {
            if (subset.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    protected static Optional<List<Set<DFA>>> split(Set<Set<DFA>> partition, Set<DFA> set) {
        List<Set<DFA>> split = null;
        if (set.size() < 2) {
            return Optional.empty();
        }
        Iterator<DFA> dfa = set.iterator();
        DFA first = dfa.next();
        Set<DFA> other = set.stream().
                    filter(second -> second != first && !equivalent(partition, first, second)).
                    collect(Collectors.toSet());
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

    protected static boolean equivalent(Set<Set<DFA>> partition, DFA first, DFA second) {
        if (first.getTransitions().size() != second.getTransitions().size()) {
            return false;
        }
        for (Pair<CharRange, DFA> transition : first.getTransitions()) {
            boolean matched = false;
            for (Pair<CharRange, DFA> secondTransition : second.getTransitions()) {
                if (transition.getLeft().equals(secondTransition.getLeft())) {
                    DFA target = transition.getRight();
                    DFA secondTarget = transition.getRight();
                    if (target(partition, target) == target(partition, secondTarget)) {
                        matched = true;
                        break;
                    }
                }
            }
            if (!matched) {
                return false;
            }
        }
        return true;
    }

    protected static Set<DFA> target(Set<Set<DFA>> partition, DFA dfa) {
        for (Set<DFA> set : partition) {
            if (set.contains(dfa)) {
                return set;
            }
        }
        return null;
    }
}
