package com.justinblank.strings;

import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NFA {
    private boolean accepting;
    private List<Pair<CharRange, List<NFA>>> transitions = new ArrayList<>();

    protected NFA(boolean accepting) {
        this.accepting = accepting;
    }

    protected void addTransitions(CharRange charRange, List<NFA> nfas) {
        transitions.add(Pair.of(charRange, nfas));
        // we trust that our character ranges don't overlap
        transitions.sort(Comparator.comparingInt(p -> p.getLeft().getStart()));
    }

    public List<Pair<CharRange, List<NFA>>> getTransitions() {
        return transitions;
    }

    protected boolean isAccepting() {
        return accepting;
    }

    protected Collection<NFA> transition(char c) {
        for (Pair<CharRange, List<NFA>> transition : transitions) {
            if (transition.getLeft().inRange(c)) {
                return epsilonClosure(transition.getRight());
            }
        }
        return Collections.emptyList();
    }

    public boolean matches(String s) {
        int length = s.length();
        Stream<NFA> current = epsilonClosure().stream();
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            current = current.flatMap(node -> node.transition(c).stream()).filter(Objects::nonNull);
        }
        return current.anyMatch(NFA::isAccepting);
    }

    public Set<NFA> epsilonClosure() {
        Set<NFA> closure = getTransitions().
                stream().
                filter(pair -> pair.getLeft().isEmpty()).
                flatMap(pair -> pair.getRight().stream()).
                collect(Collectors.toSet());
        closure.add(this);
        return closure;
    }

    public static Set<NFA> epsilonClosure(Collection<NFA> nfaStates) {
        return nfaStates.stream().
                flatMap(nfa -> nfa.epsilonClosure().stream()).
                collect(Collectors.toSet());
    }
}
