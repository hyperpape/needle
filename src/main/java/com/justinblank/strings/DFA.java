package com.justinblank.strings;

import com.justinblank.strings.RegexAST.Node;
import com.justinblank.strings.Search.SearchMethod;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

class DFA {

    private boolean accepting;
    private final int stateNumber;
    private DFA root;
    // Only populated on the root
    private List<DFA> states;
    private List<Pair<CharRange, DFA>> transitions = new ArrayList<>();

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
        Node node = RegexParser.parse(regex);
        try {
            NFA nfa = new NFA(RegexInstrBuilder.createNFA(node));
            return NFAToDFACompiler.compile(nfa);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create dfa from string '" + regex + "'", e);
        }
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
        // the final index of a successful match. May be set multiple times, but should only ever increase.
        int matchEnd = Integer.MIN_VALUE;
        if (accepting) {
            matchStart = 0;
            matchEnd = 0;
        }
        for (int i = 0; i < s.length(); i++) {
            if (i == 0) {
                newStateStarts = initSearchStateArray();
            } else {
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
                            } else if (dfa.stateNumber == 0) {
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
        } else {
            return new MatchResult(true, matchStart, matchEnd);
        }
    }

    private int[] initSearchStateArray() {
        int[] stateStarts = new int[states.size()];
        clearSearchStateArray(stateStarts);
        return stateStarts;
    }

    private void clearSearchStateArray(int[] stateStarts) {
        Arrays.fill(stateStarts, -1);
    }

    public int statesCount() {
        return states.size();
    }

    public Set<DFA> allStates() {
        return new HashSet<>(states);
    }

    protected Set<DFA> acceptingStates() {
        return allStates().stream().filter(DFA::isAccepting).collect(Collectors.toSet());
    }

    protected boolean hasSelfTransition() {
        for (Pair<CharRange, DFA> transition : transitions) {
            if (transition.getRight() == this) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get a chain of states that must be passed through, starting with a given state
     *
     * @return a non-null, possibly empty list of states that must be passed through in order--does not contain the
     * starting state
     */
    List<DFA> chain() {
        var chain = new LinkedHashSet<DFA>();
        var next = this;
        while (true) {

            if (next.isAccepting()) {
                break;
            }
            else if (next.transitions.size() == 1) {
                 var newNext = next.transitions.get(0).getRight();
                 if (chain.contains(newNext)) {
                     break;
                 }
                 else {
                     if (next != this) {
                         chain.add(newNext);
                     }
                     next = newNext;
                 }
            }
            else {
                Set<DFA> followers = new HashSet<>();
                for (var transition : transitions) {
                    followers.add(transition.getRight());
                }
                if (followers.size() != 1) {
                    break;
                }
                var newNext = followers.iterator().next();
                if (next != this) {
                    chain.add(newNext);
                }
                next = newNext;
            }
        }
        return new ArrayList<>(chain);
    }

    Map<Integer, Offset> calculateOffsets() {
        var map = new HashMap<Integer, Offset>();
        var seen = new HashSet<>();
        for (var state : states) {
            // TODO: this is an oversimplification, but we could
            if (!seen.contains(state.stateNumber)) {
                var offset = state.calculateOffset();
                offset.ifPresent(o -> {
                    seen.addAll(o.passedStates);
                    map.put(state.stateNumber, o);
                });
            }

        }
        return map;
    }

    Optional<Offset> calculateOffset() {
        Set<Integer> passedStates = new HashSet<>();
        var count = -1;
        CharRange charRange = null;
        var next = this;
        // TODO: handle case where offset is resolved to . or similarly permissive character range: checking that is
        //  not useful
        while (true) {
            if (passedStates.contains(next) || next.hasSelfTransition() || next.transitions.size() == 0 ||
                    next.isAccepting()) {
                break;
            }
            if (next.transitions.size() != 1) {
                if (!next.allTransitionsLeadToSameState()) {
                    break;
                }
            }
            passedStates.add(next.stateNumber);

            var transition = next.transitions.get(0);
            next = transition.getRight();
            count++;
            charRange = transition.getLeft();
        }

        if (count > 0) {
            return Optional.of(new Offset(count, passedStates, charRange));
        } else {
            return Optional.empty();
        }
    }

    private boolean allTransitionsLeadToSameState() {
        for (int i = 0; i < transitions.size() - 1; i++) {
            if (transitions.get(i).getRight() != transitions.get(i + 1).getRight()) {
                return false;
            }
        }
        return true;
    }

    protected int charCount() {
        int chars = 0;
        for (Pair<CharRange, DFA> transition : transitions) {
            chars += 1 + (int) transition.getLeft().getEnd() - (int) transition.getLeft().getStart();
        }
        return chars;
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
        return stateNumber;
    }

    /**
     * Check invariants:
     * <ul>
     *     <li>The states reachable from the root should be contained in the states member variable</li>
     *     <li>All states should have distinct stateNumbers</li>
     *     <li>All states should refer to the same root</li>
     * </ul>
     * <p>
     * Only applicable to the root node.
     *
     * @throws IllegalStateException if the invariants are broken
     */
    protected boolean checkRep() {
        assert allStatesReachable() : "Some state was unreachable";
        assert allStates().containsAll(states);
        assert states.containsAll(allStates());
        assert states.stream().allMatch(dfa -> dfa.root == this);
        assert states.stream().map(DFA::getStateNumber).count() == states.size();
        assert states.contains(this) : "root not included in states";
        assert states.stream().anyMatch(DFA::isAccepting) : "no accepting state found";
        assert transitions.stream().map(Pair::getRight).allMatch(dfa -> states.contains(dfa));
        return true;
    }

    private boolean allStatesReachable() {
        for (DFA dfa : states) {
            for (Pair<CharRange, DFA> transition : dfa.transitions) {
                DFA target = transition.getRight();
                if (target.stateNumber > states.size() || states.get(target.stateNumber) != target) {
                    return false;
                }
            }
        }
        return true;
    }

    protected boolean isRoot() {
        return this == root;
    }

    public Optional<DFA> after(String prefix) {
        var dfa = this;
        for (int i = 0; i < prefix.length(); i++) {
            dfa = dfa.transition(prefix.charAt(i));
            if (dfa == null) {
                return Optional.empty();
            }
        }
        return Optional.of(dfa);
    }
}
