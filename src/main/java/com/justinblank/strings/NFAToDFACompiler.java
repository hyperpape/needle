package com.justinblank.strings;

import java.util.*;

import static com.justinblank.strings.RegexInstr.Opcode.*;

public class NFAToDFACompiler {

    private Map<Set<Integer>, DFA> stateSets = new HashMap<>();
    private int state = 1; // root will always be zero
    private DFA root;
    private final NFA nfa;

    private NFAToDFACompiler(NFA nfa) {
        this.nfa = nfa;
    }

    public static DFA compile(NFA nfa) {
        DFA dfa = new NFAToDFACompiler(nfa)._compile(nfa);
        return MinimizeDFA.minimizeDFA(dfa);
    }

    private DFA _compile(NFA nfa) {
        Set<Integer> states = nfa.epsilonClosure(0);
        root = DFA.root(nfa.hasAcceptingState(states));
        addNFAStatesToDFA(states, root);
        return root;
    }

    private void addNFAStatesToDFA(Set<Integer> states, DFA dfa) {
        Stack<Set<Integer>> pending = new Stack<>();
        pending.add(states);
        while (!pending.isEmpty()) {
            states = pending.pop();
            DFA foundDFA = stateSets.get(states);
            if (foundDFA != null) {
                dfa = foundDFA;
            }
            Set<Integer> epsilonClosure = nfa.epsilonClosure(states);
            List<CharRange> ranges = CharRange.minimalCovering(findCharRanges(epsilonClosure));
            for (CharRange range : ranges) {
                // any element of the range is equally good here, getStart()/getEnd() doesn't matter
                Set<Integer> moves = nfa.epsilonClosure(transition(epsilonClosure, range.getStart()));
                DFA targetDfa = stateSets.get(moves);
                if (targetDfa == null) {
                    pending.add(moves);
                    boolean accepting = nfa.hasAcceptingState(moves);
                    targetDfa = new DFA(root, accepting, state++);
                    stateSets.put(moves, targetDfa);
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
