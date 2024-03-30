package com.justinblank.strings;

import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

import static com.justinblank.strings.RegexInstr.Opcode.*;

class NFAToDFACompiler {

    private Map<StateSet, List<Pair<StateSet, DFA>>> stateSets = new HashMap<>();
    private int nextState = 1; // root will always be zero
    private DFA root;
    private final NFA nfa;

    NFAToDFACompiler(NFA nfa) {
        this.nfa = nfa;
    }

    public static DFA compile(NFA nfa, ConversionMode mode) {
        return compile(nfa, mode, false);
    }

    protected static DFA compile(NFA nfa, ConversionMode mode, boolean debug) {
        DFA dfa = new NFAToDFACompiler(nfa)._compile(nfa, mode);
        dfa.pruneDeadStates();
        if (debug) {
            System.out.println("Pre-minimization dfa");
            System.out.println(GraphViz.toGraphviz(dfa));
        }
        return MinimizeDFA.minimizeDFA(dfa);
    }

    DFA _compile(NFA nfa, ConversionMode mode) {
        StateSet states = new StateSet();
        states.add(0, 0, RegexInstrBuilder.STARTING_PRIORITY);
        states = getEpsilonClosure(states);
        root = DFA.root(nfa.hasAcceptingState(states));
        states.seenAccepting = root.isAccepting();
        storeDFA(states, root);
        addNFAStatesToDFA(states, mode);
        return root;
    }

    private DFA storeDFA(StateSet states, DFA dfa) {
        var stateDFAPairs = stateSets.get(states);
        if (null == stateDFAPairs) {
            stateDFAPairs = new ArrayList<>();
            stateSets.put(states, stateDFAPairs);
        }
        stateDFAPairs.add(Pair.of(states, dfa));
        stateSets.put(states, stateDFAPairs);
        return dfa;
    }

    private void addNFAStatesToDFA(StateSet states, ConversionMode mode) {
        Stack<StateSet> pending = new Stack<>();
        pending.add(states);
        while (!pending.isEmpty()) {
            states = pending.pop();
            DFA dfa = getDFA(states);
            StateSet epsilonClosure = getEpsilonClosure(states);
            boolean accepting = epsilonClosure.seenAccepting;
            // No point in ever going past an accepting state for the contained in search
            // Searches will be correct without this block, because the compiled DFA algorithm also checks for accepting
            // states, but produced DFA will be larger than necessary
            if (accepting && mode == ConversionMode.CONTAINED_IN) {
                continue;
            }
            if ((mode == ConversionMode.CONTAINED_IN || (!accepting && mode == ConversionMode.DFA_SEARCH))) {
                epsilonClosure.add(0, 0, RegexInstrBuilder.STARTING_PRIORITY);
                // TODO: for the sake of normalizing things, should this be:
                // var initialStates = new StateSet();
                // initialStates.add(0, 0);
                // initialStates = getEpsilonClosure(initialStates);
                // for (var state : initialStates) {
                //     epsilonClosure.add(state, initialStates.getDistance(state);
                // }
            }
            List<CharRange> ranges = CharRange.coverAllChars(CharRange.minimalCovering(findCharRanges(epsilonClosure)));
            for (CharRange range : ranges) {
                // any element of the range is equally good here, getStart()/getEnd() doesn't matter
                StateSet postTransitionStates = getEpsilonClosure(transition(epsilonClosure, range.getStart()));
                // We want to add the initial state to the state set if we're doing a search method (not match) to
                // enable restarting when we reach an empty state
                // but we want to avoid doing that when we've reached an accepting state
                postTransitionStates.seenAccepting = nfa.hasAcceptingState(postTransitionStates) || epsilonClosure.seenAccepting;
                // TODO: Pruning should be a no-op for ConversionMode.BASIC, but in fact, removing the mode check will
                //  cause test failures
                if (postTransitionStates.seenAccepting && (mode == ConversionMode.CONTAINED_IN || mode == ConversionMode.DFA_SEARCH)) {
                    boolean removed;
                    do {
                        removed = false;
                        for (var state : postTransitionStates) {
                            if (nfa.isAcceptingState(state)) {
                                var distance = postTransitionStates.getDistance(state);
                                var priority = postTransitionStates.getPriority(state);
                                if (postTransitionStates.prune(state, distance, priority)) {
                                    removed = true;
                                    break; // avoid ConcurrentModificationException
                                }
                            }
                        }
                    } while (removed);
                }
                if (!postTransitionStates.seenAccepting && (mode == ConversionMode.CONTAINED_IN || mode == ConversionMode.DFA_SEARCH)) {
                    postTransitionStates.add(0, 0, RegexInstrBuilder.STARTING_PRIORITY);
                    // This doesn't change behavior, but it does make it easier to read the stateSets
                    postTransitionStates = getEpsilonClosure(postTransitionStates);
                }
                DFA targetDfa = getDFA(postTransitionStates);
                if (targetDfa == null) {
                    pending.add(postTransitionStates);
                    targetDfa = new DFA(root, nfa.hasAcceptingState(postTransitionStates), nextState++);
                    storeDFA(postTransitionStates, targetDfa);
                }
                dfa.addTransition(range, targetDfa);
            }
        }
    }

    private DFA getDFA(StateSet states) {
        var stateDFAPairs = stateSets.get(states);
        if (stateDFAPairs == null) {
            return null;
        }
        for (var pair : stateDFAPairs) {
            if (states.size() == 1 || (states.seenAccepting == pair.getLeft().seenAccepting)) {
                return pair.getRight();
            }
        }
        return null;
    }

    private StateSet getEpsilonClosure(StateSet states) {
        StateSet closure = new StateSet();
        for (Integer state : states) {
            var priority = nfa.regexInstrs[state].priority;
            for (Integer epsilonTransitionState : nfa.epsilonClosure(state)) {
                if (nfa.isAcceptingState(epsilonTransitionState)) {
                    closure.seenAccepting = true;
                }
                if (states.contains(epsilonTransitionState)) {
                    closure.add(epsilonTransitionState, states.getDistance(epsilonTransitionState), priority);
                }
                else {
                    closure.add(epsilonTransitionState, states.getDistance(state), priority);
                }
            }
        }
        closure.seenAccepting |= states.seenAccepting;
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
            var priority = instr.priority;
            if (instr.opcode == CHAR_RANGE && instr.start <= c && instr.end >= c) {
                transitionStates.add(state + 1, nfaStates.getDistance(state) + 1, priority);
            }
        }
        return transitionStates;
    }


}
