package com.justinblank.strings;

import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DFA {

    private boolean accepting;
    private List<Pair<CharRange, DFA>> transitions = new ArrayList<>();

    protected DFA(boolean accepting) {
        this.accepting = accepting;
    }

    protected void addTransition(CharRange charRange, DFA dfa) {
        assert !charRange.isEmpty() : "cannot add an epsilon transition to a DFA";
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

    public int statesCount() {
        return allStates().size();
    }

    public Set<DFA> allStates() {
        Set<DFA> seen = new HashSet<>();
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
}
