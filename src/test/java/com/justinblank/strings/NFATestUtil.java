package com.justinblank.strings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NFATestUtil {

    /**
     * Create an NFA corresponding to (a*)(a|b)
     * @return
     */
    public static NFA aSTAR_aORb_() {
        NFA nfa = new NFA(false, 0);
        List<NFA> postTransition = new ArrayList<>();
        postTransition.add(nfa);
        NFA penultimate = new NFA(false, 1);
        postTransition.add(penultimate);
        nfa.addTransitions(new CharRange('a', 'a'), postTransition);
        nfa.addTransitions(CharRange.emptyRange(), Collections.singletonList(penultimate));

        NFA other = new NFA(true, 2);
        penultimate.addTransitions(new CharRange('a', 'b'), Collections.singletonList(other));

        // crappy book-keeping
        nfa.setRoot(nfa);
        nfa.setStates(Arrays.asList(nfa, penultimate, other));
        other.computeEpsilonClosure();
        nfa.computeEpsilonClosure();
        penultimate.computeEpsilonClosure();
        postTransition.stream().forEach(NFA::computeEpsilonClosure);
        return nfa;
    }

    /**
     * Create an NFA corresponding to [A-Za-z0-9]*
     * @return an NFA
     */
    public static NFA _0to9AtoZatoz_STAR() {
        NFA nfa = new NFA(true, 0);
        nfa.addTransitions(new CharRange('0', '9'), Collections.singletonList(nfa));
        nfa.addTransitions(new CharRange('A', 'Z'), Collections.singletonList(nfa));
        nfa.addTransitions(new CharRange('a', 'z'), Collections.singletonList(nfa));
        nfa.setRoot(nfa);
        nfa.setStates(Arrays.asList(nfa));
        nfa.computeEpsilonClosure();
        return nfa;
    }
}
