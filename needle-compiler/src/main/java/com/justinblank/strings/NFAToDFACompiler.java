package com.justinblank.strings;

import java.util.*;

import static com.justinblank.strings.RegexInstr.Opcode.*;

class NFAToDFACompiler {

    private Map<Set<Integer>, DFA> stateSets = new HashMap<>();
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
        Set<Integer> states = nfa.epsilonClosure(0);
        if (mode == ConversionMode.CONTAINED_IN || mode == ConversionMode.DFA_SEARCH) {
            states.add(0);
        }
        root = DFA.root(nfa.hasAcceptingState(states));
        stateSets.put(states, root);
        addNFAStatesToDFA(states, mode);
        return root;
    }

    private void addNFAStatesToDFA(Set<Integer> states, ConversionMode mode) {
        Stack<Set<Integer>> pending = new Stack<>();
        pending.add(states);
        while (!pending.isEmpty()) {
            states = pending.pop();
            DFA dfa = stateSets.get(states);
            Set<Integer> epsilonClosure = nfa.epsilonClosure(states);
            boolean accepting = nfa.hasAcceptingState(states);
            // No point in ever going past an accepting state for the contained in search
            // Searches will be correct without this line, but produced DFA will be larger than necessary
            if (accepting && mode == ConversionMode.CONTAINED_IN) {
                continue;
            }
            if ((mode == ConversionMode.CONTAINED_IN || (!accepting && mode == ConversionMode.DFA_SEARCH))) {
                epsilonClosure.add(0);
            }
            List<CharRange> ranges = CharRange.minimalCovering(findCharRanges(epsilonClosure));
            for (CharRange range : ranges) {
                // any element of the range is equally good here, getStart()/getEnd() doesn't matter
                Set<Integer> postTransitionStates = nfa.epsilonClosure(transition(epsilonClosure, range.getStart()));
                // We want to add the initial state to the state set if we're doing a search method (not match) to
                // enable restarting when we reach an empty state
                // but we want to avoid doing that when we've reached an accepting state
                if (!nfa.hasAcceptingState(postTransitionStates) && (mode == ConversionMode.CONTAINED_IN || mode == ConversionMode.DFA_SEARCH)) {
                    postTransitionStates.add(0);
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

    protected Set<Integer> transition(Collection<Integer> nfaStates, char c) {
        Set<Integer> transitionStates = new HashSet<>();
        for (Integer state : nfaStates) {
            RegexInstr instr = nfa.regexInstrs[state];
            if (instr.opcode == CHAR_RANGE && instr.start <= c && instr.end >= c) {
                transitionStates.add(state + 1);
            }
        }
        return transitionStates;
    }
}
