package com.justinblank.strings;

import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class NFA {

    private NFA root;
    private final int state;
    // This will only be non-null on the root
    private List<NFA> states;
    private boolean accepting;
    private List<Pair<CharRange, List<NFA>>> transitions = new ArrayList<>();
    private Set<NFA> epsilonClosure;
    private BitSet epsilonClosureIndices = new BitSet();

    protected NFA(boolean accepting, int index) {
        this.accepting = accepting;
        this.state = index;
    }

    public static NFA createNFA(String regex) {
        return ThompsonNFABuilder.createNFA(RegexParser.parse(regex));
    }

    protected void addTransitions(CharRange charRange, List<NFA> nfas) {
        for (NFA nfa : nfas) {
            nfa.root = this.root;
        }
        transitions.add(Pair.of(charRange, nfas));
        // we trust that our character ranges don't overlap
        transitions.sort(Comparator.comparingInt(p -> p.getLeft().getStart()));
    }

    protected void addEpsilonTransition(NFA end) {
        addTransitions(CharRange.emptyRange(), Collections.singletonList(end));
    }

    protected List<Pair<CharRange, List<NFA>>> getTransitions() {
        return transitions;
    }

    protected NFA getRoot() {
        return root;
    }

    protected void setRoot(NFA root) {
        this.root = root;
    }

    protected boolean isAccepting() {
        return accepting;
    }

    public List<NFA> getStates() {
        return states;
    }

    public void setStates(List<NFA> states) {
        this.states = states;
    }

    protected int getState() {
        return state;
    }

    protected NFA getState(int state) {
        return states.get(state);
    }

    protected BitSet transition(char c) {
        for (Pair<CharRange, List<NFA>> transition : transitions) {
            if (transition.getLeft().inRange(c)) {
                BitSet bitSet = new BitSet();
                for (NFA nfa : transition.getRight()) {
                    bitSet.or(nfa.epsilonClosureIndices);
                }
                return bitSet;
            }
        }
        return new BitSet();
    }

    public boolean matches(String s) {
        int length = s.length();
        BitSet current = new BitSet();
        current.or(epsilonClosureIndices);
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            BitSet newCurrent = new BitSet();
            int state = 0;
            int setBit = current.nextSetBit(0);
            while (setBit != -1) {
                NFA nfa = root.states.get(setBit);
                newCurrent.or(nfa.transition(c));
                setBit = current.nextSetBit(setBit + 1);
            }
            current = newCurrent;
        }
        int setBit = current.nextSetBit(0);
        while (setBit != -1) {
            NFA nfa = root.states.get(setBit);
            if (nfa.accepting) {
                return true;
            }
            setBit = current.nextSetBit(setBit + 1);
        }
        return false;
    }

    public MatchResult search(String s) {
        int length = s.length();
        int lastStart = Integer.MAX_VALUE;
        int lastEnd = -1;
        int i = 0;
        Set<NFA> initial = epsilonClosure();
        Set<NFA> current = initial;
        Map<NFA, Integer> stateMap = new HashMap<>();
        for (; i < length; i++) {
            // If any of our states can accept, we've found a match.
            // Keep searching for a longer match, but store the current start and end indices
            // we will no longer consider anything that starts after this starting location
            boolean accepting = hasAcceptingState(current);
            if (accepting) {
                lastStart = computeLastStart(stateMap, i);
                lastEnd = i;
            }

            current.addAll(initial);
            final int currentIndex = i;
            char c = s.charAt(i);
            Set<NFA> next = new HashSet<>();
            Map<NFA, Integer> newStateMap = new HashMap<>();
            Map<NFA, Integer> existingStateMap = stateMap;
            for (NFA node : current) {
                BitSet bitSet = node.transition(c);
                bitSet.stream().forEach(nfaIndex -> {
                    newStateMap.compute(states.get(nfaIndex), (nfa, start) -> {
                        if (start == null) {
                            start = currentIndex;
                        }
                        int startingPoint = Math.min(start, existingStateMap.getOrDefault(node, currentIndex));
                        if (startingPoint == Integer.MAX_VALUE) {
                            throw new IllegalStateException("");
                        }
                        return startingPoint;
                    });
                });
            }
            // Now, only keep states if they can match a range shorter than the current longest match
            final int currentLastStart = lastStart;
            stateMap.clear();
            newStateMap.forEach((key, value) -> {
                if (value <= currentLastStart) {
                    next.add(key);
                    stateMap.put(key, value);
                }
            });
            current = next;
        }
        boolean accepting = current.stream().anyMatch(NFA::isAccepting);
        if (accepting) {
            int thisStart = i;
            for (Map.Entry<NFA, Integer> e : stateMap.entrySet()) {
                if (e.getKey().accepting) {
                    thisStart = Math.min(thisStart, e.getValue());
                }
            }
            if (lastStart == Integer.MAX_VALUE) {
                lastStart = thisStart;
            }
            if (lastStart > thisStart) {
                return new MatchResult(true, lastStart, lastEnd);
            }
        }
        else if (lastStart != Integer.MAX_VALUE) {
            return new MatchResult(true, lastStart, lastEnd);
        }

        boolean matched = current.stream().anyMatch(NFA::isAccepting);
        return new MatchResult(matched, lastStart, i);
    }

    protected static boolean hasAcceptingState(Collection<NFA> nfas) {
        for (NFA nfa : nfas) {
            if (nfa.isAccepting()) {
                return true;
            }
        }
        return false;
    }

    protected int computeLastStart(Map<NFA, Integer> stateMap, int i) {
        int thisStart = i;
        for (Map.Entry<NFA, Integer> e : stateMap.entrySet()) {
            if (e.getKey().accepting) {
                thisStart = Math.min(thisStart, e.getValue());
            }
        }
        return thisStart;
    }

    protected Set<NFA> epsilonClosure() {
        return this.epsilonClosure;
    }

    protected static Set<NFA> epsilonClosure(Collection<NFA> nfaStates) {
        Set<NFA> closure = new HashSet<>();
        for (NFA nfa : nfaStates) {
            closure.addAll(nfa.epsilonClosure());
        }
        return closure;
    }

    protected static BitSet epsilonClosureIndices(Collection<NFA> nfaStates) {
        BitSet bs = new BitSet();
        for (NFA nfa : nfaStates) {
            bs.or(nfa.epsilonClosureIndices);
        }
        return bs;
    }

    protected Set<NFA> terminalStates() {
        Set<NFA> terminals = new HashSet<>();
        if (states != null) {
            for (NFA nfa : states) {
                if (nfa.isTerminal()) {
                    terminals.add(nfa);
                }
            }
            return terminals;
        }
        Set<NFA> seen = new HashSet<>();
        Queue<NFA> pending = new LinkedList<>();
        pending.add(this);
        // TODO: kinda hacky
        if (this.getTransitions().isEmpty()) {
            terminals.add(this);
        }
        while (!pending.isEmpty()) {
            NFA nfa = pending.poll();
            if (!seen.contains(nfa)) {
                nfa.getTransitions().stream().
                        flatMap(p -> p.getRight().stream()).
                        filter(p -> !seen.contains(p)).
                        forEach(transitionNFA -> {
                            if (transitionNFA.isTerminal()) {
                                terminals.add(transitionNFA);
                            }
                            pending.add(transitionNFA);
                        });
                seen.add(nfa);
            }
        }
        return terminals;
    }

    public Set<NFA> allStates() {
        Set<NFA> seen = new HashSet<>();
        Queue<NFA> pending = new LinkedList<>();
        pending.add(this);
        while (!pending.isEmpty()) {
            NFA current = pending.poll();
            seen.add(current);
            for (Pair<CharRange, List<NFA>> transition : current.getTransitions()) {
                List<NFA> states = transition.getRight();
                for (NFA next : states) {
                    if (!seen.contains(next)) {
                        pending.add(next);
                    }
                }
            }
        }
        return seen;
    }

    public boolean isTerminal() {
        for (Pair<CharRange, List<NFA>> pair : getTransitions()) {
            for (NFA target : pair.getRight()) {
                if (target != this) {
                    return false;
                }
            }
        }
        return true;
    }

    public int hashCode() {
        return state;
    }

    public boolean equals(Object object) {
        if (object instanceof NFA) {
            NFA other = (NFA) object;
            return other.state == state && other.root == root;
        }
        return false;
    }

    protected void computeEpsilonClosure() {
        if (this.epsilonClosure == null) {
            BitSet seen = new BitSet(root.states.size());
            BitSet pending = new BitSet(root.states.size());

            Set<NFA> closure = new HashSet<>();
            pending.set(this.state);
            seen.set(this.state);

            closure.add(this);
            while (!pending.isEmpty()) {
                int index = pending.nextSetBit(0);
                pending.clear(index);
                epsilonClosureIndices.set(index);

                NFA next = root.states.get(index);
                closure.add(next);
                for (Pair<CharRange, List<NFA>> transition : next.getTransitions()) {
                    if (transition.getLeft().isEmpty()) {
                        for (NFA reachable : transition.getRight()) {
                            if (!seen.get(reachable.state)) {
                                seen.set(reachable.state);
                                pending.set(reachable.state);
                            }
                        }
                    }
                }
            }
            if (closure.isEmpty()) {
                this.epsilonClosure = Collections.emptySet();
            }
            else {
                this.epsilonClosure = closure;
            }
        }
    }

    /**
     * Check a number of invariants. Only applicable to the root node.
     *
     * @throws IllegalStateException if the invariants are broken
     */
    protected void checkRep() {
        for (NFA nfa : states) {
            assert states.get(nfa.state) == nfa;
            assert nfa.epsilonClosure != null : "NFA node " + nfa.state + " has null epsilon closure";
            for (NFA reachable : nfa.epsilonClosure) {
                assert states.contains(reachable) : "Epsilon transition to NFA node " + reachable.state + " not contained in states";
            }
            assert nfa.root != null : "NFA node " + state + " has null root";
        }
        // TODO: assert states.size() == new HashSet<>(states).size() : "nfaStates contains duplicates";
    }
}
