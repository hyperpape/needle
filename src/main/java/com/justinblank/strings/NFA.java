package com.justinblank.strings;

import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class NFA {

    private NFA root;
    private final int state;
    // This will only be non-null on the root
    private List<NFA> states;
    private boolean accepting;
    private List<Pair<CharRange, List<NFA>>> transitions = new ArrayList<>();

    protected NFA(boolean accepting, int index) {
        this.accepting = accepting;
        this.state = index;
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
            boolean accepting = current.stream().anyMatch(NFA::isAccepting);
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
                Collection<NFA> nodeTransitions = node.transition(c);
                nodeTransitions.forEach(transition -> {
                    newStateMap.compute(transition, (nfa, start) -> {
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

    public int hashCode() {
        return state;
    }
}
