package com.justinblank.strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NFATestUtil {
    
    public static NFA aSTAR_aORb_() {
        NFA nfa = new NFA(false);
        List<NFA> postTransition = new ArrayList<>();
        postTransition.add(nfa);
        NFA penultimate = new NFA(false);
        postTransition.add(penultimate);
        nfa.addTransitions(new CharRange('a', 'a'), postTransition);
        nfa.addTransitions(CharRange.emptyRange(), Collections.singletonList(penultimate));

        penultimate.addTransitions(new CharRange('a', 'b'), Collections.singletonList(new NFA(true)));

        return nfa;
    }

    public static NFA _0to9AtoZatoz_STAR() {
        NFA nfa = new NFA(true);
        nfa.addTransitions(new CharRange('0', '9'), Collections.singletonList(nfa));
        nfa.addTransitions(new CharRange('A', 'Z'), Collections.singletonList(nfa));
        nfa.addTransitions(new CharRange('a', 'z'), Collections.singletonList(nfa));
        return nfa;
    }
}
