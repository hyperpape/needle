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
}
