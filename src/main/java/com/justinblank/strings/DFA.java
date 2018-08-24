package com.justinblank.strings;

import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class DFA {

    private boolean accepting;
    private List<Pair<CharRange, DFA>> transitions = new ArrayList<>();

    protected DFA(boolean accepting) {
        this.accepting = accepting;
    }

    protected void addTransition(CharRange charRange, DFA dfa) {
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
        Set<DFA> states = new HashSet<>();
        Queue<DFA> queue = new LinkedList<>();
        queue.add(this);
        while (!queue.isEmpty()) {
            DFA next = queue.poll();
            states.add(next);
            for (Pair<CharRange, DFA> pair : next.getTransitions()) {
                DFA dfa = pair.getRight();
                boolean added = states.add(dfa);
                if (added) {
                    queue.add(dfa);
                }
            }
        }
        return states.size();
    }
}
