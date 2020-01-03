package com.justinblank.strings;

import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

// This implements Hopcroft's Minimization Algorithm
class MinimizeDFA {

    private int state = 1; // Account for the fact that root will be 0
    private DFA root;
    private static int splitCalls = 0;
    private static int successfulSplits = 0;

    protected static DFA minimizeDFA(DFA dfa) {
        MinimizeDFA minimizer = new MinimizeDFA();
        Map<DFA, Set<DFA>> partition = minimizer.createPartition(dfa);
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
        System.out.println("Split calls: " + splitCalls);
        System.out.println("Successful splits " + successfulSplits);
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

    protected Map<DFA, Set<DFA>> createPartition(DFA dfa) {
        PartitionState partition = initialPartition(dfa);
        boolean changed = true;
        int currentIteration = 0;
        while (changed) {
            currentIteration++;
            changed = false;
            List<DFAGroup> changedItems = new ArrayList<>();
            while (partition.queue.peek() != null) {
                DFAGroup set = partition.queue.poll();
                if (set.size() > 1) {
                    Optional<List<Set<DFA>>> splitted = split(partition.partition, set.dfas);
                    if (splitted.isPresent()) {
                        changed = true;
                        for (Set<DFA> part : splitted.get()) {
                            DFAGroup dfaGroup = new DFAGroup(part);
                            changedItems.add(dfaGroup);
                            for (DFA thing : part) {
                                partition.partition.put(thing, dfaGroup);
                            }
                        }
                        partition.queue.addAll(changedItems);
                        break;
                    } else {
                        set.lastConsidered = currentIteration;
                        changedItems.add(set);
                    }
                }
            }
        }
        assert allNonEmpty(partition.partition);
        return unwrap(partition.partition);
    }

    private Map<DFA, Set<DFA>> unwrap(Map<DFA, DFAGroup> partition) {
        // TODO: cleaner?
        Map<DFA, Set<DFA>> unwrapped = new HashMap<>();
        for (var e : partition.entrySet()) {
            unwrapped.put(e.getKey(), e.getValue().dfas);
        }
        return unwrapped;
    }

    /**
     * Create an initial partition that will subsequently be refined.
     *
     * We create the initial partition by splitting the states by 1) whether or not they are accepting and 2) how many
     * outgoing transitions they have.
     * @param dfa the DFA whose states we're partitioning
     * @return a coarse partition of the DFA states that will be refined
     */
    private static PartitionState initialPartition(DFA dfa) {
        Map<DFA, DFAGroup> partition = new HashMap<>(dfa.statesCount());
        Set<DFA> accepting = dfa.acceptingStates();
        Set<DFA> nonAccepting = new HashSet<>(dfa.allStates());
        nonAccepting.removeAll(accepting);
        List<DFAGroup> dfaGroups = new ArrayList<>();
        if (!accepting.isEmpty()) {
            dfaGroups.addAll(partitionByTransitionCount(partition, accepting));
        }
        if (!nonAccepting.isEmpty()) {
            dfaGroups.addAll(partitionByTransitionCount(partition, nonAccepting));
        }
        return new PartitionState(partition, new PriorityQueue<>(dfaGroups));
    }

    // TODO: Better name
    private static List<DFAGroup> partitionByTransitionCount(Map<DFA, DFAGroup> partition, Set<DFA> accepting) {
        Map<Integer, Set<DFA>> acceptingByTransitionCount = new HashMap<>();
        List<DFAGroup> dfaGroups = new ArrayList<>();
        for (DFA acceptingDFA : accepting) {
            Set<DFA> set = acceptingByTransitionCount.computeIfAbsent(acceptingDFA.getTransitions().size(), (s) -> new HashSet<>());
            set.add(acceptingDFA);
        }
        for (Map.Entry<Integer, Set<DFA>> e : acceptingByTransitionCount.entrySet()) {
            DFAGroup group = new DFAGroup(e.getValue());
            dfaGroups.add(group);
            for (DFA acceptingDFA : e.getValue()) {
                partition.put(acceptingDFA, group);
            }
        }
        return dfaGroups;
    }

    protected static boolean allNonEmpty(Map<DFA, DFAGroup> partition) {
        for (DFAGroup subset : partition.values()) {
            if (subset.dfas.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    protected static Optional<List<Set<DFA>>> split(Map<DFA, DFAGroup> partition, Set<DFA> set) {
        splitCalls++;
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
            set.removeAll(other);
            splitted.add(set);
            successfulSplits++;
            return Optional.of(splitted);
        }
        return Optional.empty();
    }

    protected static boolean equivalent(Map<DFA, DFAGroup> partition, DFA first, DFA second) {
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

    static class DFAGroup implements Comparable<DFAGroup> {
        Set<DFA> dfas;
        int lastConsidered = 0;

        DFAGroup(Set<DFA> dfas) {
            this.dfas = dfas;
        }

        public int size() {
            return dfas.size();
        }

        public int compareTo(DFAGroup other) {
            // Without these size checks, we'd experience a mild-slowdown where long series of very small sets were
            // considered, despite the low probability that they'll provide successful splits. It seems like there
            // should be a lot of room to improve on here
            if (this.lastConsidered > other.lastConsidered && other.size() > 2) {
                return 1;
            }
            else if (this.lastConsidered < other.lastConsidered && this.size() > 2) {
                return -1;
            }
            else if (this.size() > other.size()) {
                return -1;
            }
            else if (this.size() < other.size()) {
                return 1;
            }
            return 0;
        }

        @Override
        public String toString() {
            return "DFAGroup{" +
                    "dfasSize=" + dfas.size() +
                    ", lastConsidered=" + lastConsidered +
                    '}';
        }
    }

    static class PartitionState {
        Map<DFA, DFAGroup> partition;
        PriorityQueue<DFAGroup> queue;

        PartitionState(Map<DFA, DFAGroup> partition, PriorityQueue<DFAGroup> queue) {
            this.partition = partition;
            this.queue = queue;
        }
    }
}
