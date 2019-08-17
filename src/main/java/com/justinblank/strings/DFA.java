package com.justinblank.strings;

import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class DFA {

    private boolean accepting;
    private final int stateNumber;
    private DFA root;
    // Only populated on the root
    private List<DFA> states;
    private List<Pair<CharRange, DFA>> transitions = new ArrayList<>();

    protected DFA(boolean accepting, int stateNumber) {
        this.accepting = accepting;
        this.stateNumber = stateNumber;
    }

    static DFA root(boolean accepting) {
        return new DFA(true, accepting, 0);
    }

    private DFA(boolean root, boolean accepting, int stateNumber) {
        if (root) {
            this.root = this;
            this.states = new ArrayList<>();
            this.states.add(this);
        }
        this.accepting = accepting;
        this.stateNumber = stateNumber;
    }

    protected DFA(DFA root, boolean accepting, int stateNumber) {
        if (root == null) {
            throw new IllegalArgumentException("Cannot create DFA with null root");
        }
        this.root = root;
        this.accepting = accepting;
        this.stateNumber = stateNumber;
        this.root.states.add(this);
    }

    public static DFA createDFA(String regex) {
        NFA nfa = ThompsonNFABuilder.createNFA(RegexParser.parse(regex));
        return NFAToDFACompiler.compile(nfa);
    }

    protected void addTransition(CharRange charRange, DFA dfa) {
        assert !charRange.isEmpty() : "cannot add an epsilon transition to a DFA";
        if (transitions.stream().anyMatch(t -> t.getLeft().equals(charRange))) {
            return;
        }
        transitions.add(Pair.of(charRange, dfa));
        // we trust that our character ranges don't overlap
        transitions.sort(Comparator.comparingInt(p -> p.getLeft().getStart()));
    }

    protected List<Pair<CharRange, DFA>> getTransitions() {
        return transitions;
    }

    protected boolean isAccepting() {
        return accepting;
    }

    protected DFA transition(char c) {
        for (Pair<CharRange, DFA> transition : transitions) {
            if (transition.getLeft().inRange(c)) {
                return transition.getRight();
            }
        }
        return null;
    }

    public boolean matches(String s) {
        int length = s.length();
        DFA current = this;
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            current = current.transition(c);
            if (current == null) {
                return false;
            }
        }
        return current.accepting;
    }

    public MatchResult search(String s) {
        // In order to match, we need to know the earliest index we could start from to reach a given state
        int[] stateStarts = initSearchStateArray();
        int[] newStateStarts = null;
        // the starting index of a successful match. May be set multiple times, but can only ever decrease.
        int matchStart = Integer.MAX_VALUE;
        // the final index of a succcesful. May be set multiple times, but should only ever increase.
        int matchEnd = Integer.MIN_VALUE;
        if (accepting) {
            matchStart = 0;
            matchEnd = 0;
        }
        for (int i = 0; i < s.length(); i++) {
            if (i == 0) {
                newStateStarts = initSearchStateArray();
            }
            else {
                clearSearchStateArray(newStateStarts);
            }
            char c = s.charAt(i);
            int earliestCurrentStart = Integer.MAX_VALUE;
            for (int j = 0; j < stateStarts.length; j++) {
                int stateStart = stateStarts[j];
                // We consider states that are live, but ignore those which started later than the best match we've seen
                boolean shouldConsider = stateStart > -1 && stateStart <= matchStart;
                // We also always consider the initial state when we haven't yet seen a match
                shouldConsider |= j == 0 && matchStart == Integer.MAX_VALUE;
                if (shouldConsider) {
                    DFA dfa = states.get(j);
                    DFA found = dfa.transition(c);
                    if (found != null) {
                        int foundStateNumber = found.stateNumber;
                        if (newStateStarts[foundStateNumber] == -1 ||
                                newStateStarts[foundStateNumber] > stateStarts[foundStateNumber]) {
                            if (i == 0) {
                                stateStart = 0;
                            }
                            else if (dfa.stateNumber == 0) {
                                stateStart = i;
                            }
                            newStateStarts[foundStateNumber] = stateStart;
                            earliestCurrentStart = Math.min(earliestCurrentStart, stateStart);
                            if (found.accepting) {
                                int newMatchStart = newStateStarts[foundStateNumber];
                                if (newMatchStart <= matchStart) {
                                    matchStart = newMatchStart;
                                    matchEnd = i + 1;
                                }
                            }
                        }
                    }
                }
            }
            if (earliestCurrentStart > matchStart) {
                return new MatchResult(true, matchStart, matchEnd);
            }
            // Swap arrays, to avoid repeatedly allocating
            int[] tmp = stateStarts;
            stateStarts = newStateStarts;
            newStateStarts = tmp;
        }
        if (matchStart == Integer.MAX_VALUE) {
            return MatchResult.FAILURE;
        }
        else {
            return new MatchResult(true, matchStart, matchEnd);
        }
    }

    private int[] initSearchStateArray() {
        int[] stateStarts = new int[states.size()];
        clearSearchStateArray(stateStarts);
        return stateStarts;
    }

    private void clearSearchStateArray(int[] stateStarts) {
        for (int i = 0; i < stateStarts.length; i++) {
            stateStarts[i] = -1;
        }
    }

    public int statesCount() {
        return allStates().size();
    }

    public Set<DFA> allStates() {
        Set<DFA> seen = new LinkedHashSet<>();
        Queue<DFA> pending = new LinkedList<>();
        pending.add(this);
        while (!pending.isEmpty()) {
            DFA current = pending.poll();
            seen.add(current);
            for (Pair<CharRange, DFA> transition : current.getTransitions()) {
                DFA next = transition.getRight();
                if (!seen.contains(next)) {
                    pending.add(next);
                    seen.add(next);
                }
            }
        }
        return seen;
    }

    public Set<DFA> acceptingStates() {
        return allStates().stream().filter(DFA::isAccepting).collect(Collectors.toSet());
    }

    public int getStateNumber() {
        return stateNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DFA dfa = (DFA) o;
        return root == dfa.root && stateNumber == dfa.stateNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(stateNumber);
    }

    /**
     * Check invariants:
     * <ul>
     *     <li>The states reachable from the root should be contained in the states member variable</li>
     *     <li>All states should have distinct stateNumbers</li>
     *     <li>All states should refer to the same root</li>
     * </ul>
     *
     * Only applicable to the root node.
     *
     * @throws IllegalStateException if the invariants are broken
     */
    protected void checkRep() {
        assert states.stream().
                flatMap(dfa -> dfa.getTransitions().stream()).
                map(Pair::getRight).
                allMatch(reachable -> states.contains(reachable)) : "Some state was unreachable";
        assert states.stream().allMatch(dfa -> dfa.root == this);
        assert states.stream().map(DFA::getStateNumber).count() == states.size();
        assert states.contains(this) : "root not included in states";
        assert transitions.stream().map(Pair::getRight).allMatch(dfa -> states.contains(dfa));
    }

    protected boolean isRoot() {
        return this == root;
    }
}
