package com.justinblank.strings;

import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NFA {

    private NFA root;
    // This will only be non-null on the root
    private List<NFA> states;
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
        Set<NFA> current = epsilonClosure();
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            current = current.stream().
                    flatMap(node -> node.transition(c).stream()).
                    filter(Objects::nonNull).
                    collect(Collectors.toSet());
        }
        return current.stream().anyMatch(NFA::isAccepting);
    }

    protected Set<NFA> epsilonClosure() {
        Set<NFA> closure = new HashSet<>();
        Queue<NFA> pending = new LinkedList<>();
        pending.add(this);
        while (!pending.isEmpty()) {
            NFA next = pending.poll();
            closure.add(next);
            next.getTransitions().
                    stream().
                    filter(pair -> pair.getLeft().isEmpty()).
                    flatMap(pair -> pair.getRight().stream()).
                    filter(nfa -> !closure.contains(nfa)).
                    forEach(pending::add);
        }
        return closure;
    }

    protected static Set<NFA> epsilonClosure(Collection<NFA> nfaStates) {
        return nfaStates.stream().
                flatMap(nfa -> nfa.epsilonClosure().stream()).
                collect(Collectors.toSet());
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

    protected boolean isTerminal() {
        return getTransitions().stream().
                flatMap(pair -> pair.getRight().stream()).
                allMatch(this::equals);
    }
}
