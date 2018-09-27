package com.justinblank.strings;

import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NFAToDFACompiler {

    private Map<Set<NFA>, DFA> stateSets = new HashMap<>();

    private NFAToDFACompiler() {
    }

    public static DFA compile(NFA nfa) {
        return new NFAToDFACompiler()._compile(nfa);
    }

    private DFA _compile(NFA nfa) {
        Set<NFA> nfas = nfa.epsilonClosure();
        DFA dfa = new DFA(nfas.stream().anyMatch(NFA::isAccepting));
        addNFAStatesToDFA(nfas, dfa);
        return dfa;
    }

    private void addNFAStatesToDFA(Set<NFA> nfas, DFA dfa) {
        Set<NFA> epsilonClosure = NFA.epsilonClosure(nfas);
        Stream<NFA> nfaStream = epsilonClosure.stream();
        List<CharRange> ranges = CharRange.minimalCovering(findCharRanges(nfaStream.collect(Collectors.toList())));
        for (CharRange range : ranges) {
            // choice of start/end is arbitrary
            Set<NFA> moves = NFA.epsilonClosure(transition(epsilonClosure, range.getStart()));
            DFA targetDfa = stateSets.get(moves);
            if (targetDfa == null) {
                boolean accepting = moves.stream().anyMatch(NFA::isAccepting);
                targetDfa = new DFA(accepting);
                stateSets.put(moves, targetDfa);
                addNFAStatesToDFA(moves, targetDfa);
            }
            dfa.addTransition(range, targetDfa);
        }
    }

    protected static List<CharRange> findCharRanges(List<NFA> nfas) {
        List<CharRange> ranges = new ArrayList<>();
        for (NFA nfa : nfas) {
            List<Pair<CharRange, List<NFA>>> transitionList = nfa.getTransitions();
            for (Pair<CharRange, List<NFA>> pair : transitionList) {
                if (!pair.getLeft().isEmpty()) {
                    ranges.add(pair.getLeft());
                }
            }
        }
        return ranges;
    }

    protected static Set<NFA> transition(Collection<NFA> nfaStates, char c) {
        return nfaStates.stream().
                flatMap(state -> state.transition(c).stream()).
                collect(Collectors.toSet());
    }


}
