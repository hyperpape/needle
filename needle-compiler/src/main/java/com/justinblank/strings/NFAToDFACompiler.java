package com.justinblank.strings;

import java.util.*;

import static com.justinblank.strings.RegexInstr.Opcode.*;

class NFAToDFACompiler {

    private Map<StateSet, DFA> stateSets = new HashMap<>();
    private int nextState = 1; // root will always be zero
    private DFA root;
    private final NFA nfa;

    NFAToDFACompiler(NFA nfa) {
        this.nfa = nfa;
    }

    public static DFA compile(NFA nfa, ConversionMode mode) {
        DFA dfa = new NFAToDFACompiler(nfa)._compile(nfa, mode);
        return MinimizeDFA.minimizeDFA(dfa);
    }

    protected static DFA compile(NFA nfa, ConversionMode mode, boolean debug) {
        DFA dfa = new NFAToDFACompiler(nfa)._compile(nfa, mode);
        if (debug) {
            System.out.println("Pre-minimization dfa");
            System.out.println(GraphViz.toGraphviz(dfa));
        }
        return MinimizeDFA.minimizeDFA(dfa);
    }

    DFA _compile(NFA nfa, ConversionMode mode) {
        StateSet states = new StateSet();
        states.add(0, 0);
        states = getEpsilonClosure(states);
        if (mode == ConversionMode.CONTAINED_IN || mode == ConversionMode.DFA_SEARCH) {
            states.add(0, 0);
        }
        root = DFA.root(nfa.hasAcceptingState(states));
        stateSets.put(states, root);
        addNFAStatesToDFA(states, mode);
        return root;
    }

    private void addNFAStatesToDFA(StateSet states, ConversionMode mode) {
        Stack<StateSet> pending = new Stack<>();
        pending.add(states);
        while (!pending.isEmpty()) {
            states = pending.pop();
            DFA dfa = stateSets.get(states);
            StateSet epsilonClosure = getEpsilonClosure(states);
            boolean accepting = nfa.hasAcceptingState(states);
            // No point in ever going past an accepting state for the contained in search
            // Searches will be correct without this line, but produced DFA will be larger than necessary
            if (accepting && mode == ConversionMode.CONTAINED_IN) {
                continue;
            }
            if ((mode == ConversionMode.CONTAINED_IN || (!accepting && mode == ConversionMode.DFA_SEARCH))) {
                epsilonClosure.add(0, 0);
            }
            List<CharRange> ranges = CharRange.minimalCovering(findCharRanges(epsilonClosure));
            for (CharRange range : ranges) {
                // any element of the range is equally good here, getStart()/getEnd() doesn't matter
                StateSet postTransitionStates = getEpsilonClosure(transition(epsilonClosure, range.getStart()));
                // We want to add the initial state to the state set if we're doing a search method (not match) to
                // enable restarting when we reach an empty state
                // but we want to avoid doing that when we've reached an accepting state
                if (!nfa.hasAcceptingState(postTransitionStates) && (mode == ConversionMode.CONTAINED_IN || mode == ConversionMode.DFA_SEARCH)) {
                    postTransitionStates.add(0, 0);
                }
                DFA targetDfa = stateSets.get(postTransitionStates);
                if (targetDfa == null) {
                    pending.add(postTransitionStates);
                    targetDfa = new DFA(root, nfa.hasAcceptingState(postTransitionStates), nextState++);
                    stateSets.put(postTransitionStates, targetDfa);
                }
                dfa.addTransition(range, targetDfa);
            }
        }
    }

    private StateSet getEpsilonClosure(StateSet states) {
        StateSet closure = new StateSet();
        for (Integer state : states) {
            for (Integer epsilonTransitionState : nfa.epsilonClosure(state)) {
                if (states.contains(epsilonTransitionState)) {
                    closure.add(epsilonTransitionState, states.getDistance(epsilonTransitionState));
                }
                else {
                    closure.add(epsilonTransitionState, states.getDistance(state));
                }
            }
        }
        return closure;
    }

    protected List<CharRange> findCharRanges(Collection<Integer> nfas) {
        List<CharRange> ranges = new ArrayList<>();
        for (Integer state : nfas) {
            RegexInstr instr = nfa.regexInstrs[state];
            if (instr.opcode == CHAR_RANGE) {
                ranges.add(new CharRange(instr.start, instr.end));
            }
        }
        return ranges;
    }

    protected StateSet transition(StateSet nfaStates, char c) {
        StateSet transitionStates = new StateSet();
        for (Integer state : nfaStates) {
            RegexInstr instr = nfa.regexInstrs[state];
            if (instr.opcode == CHAR_RANGE && instr.start <= c && instr.end >= c) {
                transitionStates.add(state + 1, nfaStates.getDistance(state) + 1);
            }
        }
        return transitionStates;
    }

    static class StateSet extends HashSet<Integer> {
        Map<Integer, Integer> stateStarts = new HashMap<>();

        @Override
        public boolean add(Integer integer) {
            // TODO: decide if this is lazy...could just not extend hashset
            throw new UnsupportedOperationException("");
        }

        public boolean add(Integer integer, Integer distance) {
            var currentState = stateStarts.get(integer);
            if (currentState != null) {
                if (currentState < distance) {
                    stateStarts.put(integer, distance);
                }
            }
            else {
                stateStarts.put(integer, distance);
            }
            return super.add(integer);
        }

        public Integer getDistance(Integer state) {
            return stateStarts.get(state);
        }
    }
}
