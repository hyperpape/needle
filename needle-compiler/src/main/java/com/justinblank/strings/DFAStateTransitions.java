package com.justinblank.strings;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Processes data for the state transition arrays that map byteClasses to target states.
 */
class DFAStateTransitions {

    protected byte[] byteClasses;

    /**
     * Map from strings representing the search methods (e.g) STATES_FORWARD to sets of strings encoding
     * state transitions. For the encoding, see {@link ByteClassUtil}.
     */
    protected Map<String, Set<String>> byteClassStringMaps = new HashMap<>();

    protected void addStateTransitionString(FindMethodSpec spec, DFA dfaState) {
        StringBuilder sb = getByteClassString(dfaState);
        if (sb.length() != 0) {
            byteClassStringMaps.get(spec.statesConstant()).add(sb.toString());
        }
    }

    boolean willUseByteClasses(DFA dfaState, DFAClassBuilder dfaClassBuilder) {
        if (byteClasses == null) {
            return false;
        }
        return true; // dfaState.getTransitions().size() > 3 || !dfa.allTransitionsLeadToSameState();
    }

    StringBuilder getByteClassString(DFA dfaState) {
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        sb.append(ByteClassUtil.encode(dfaState.getStateNumber())).append(':');
        Set<Byte> bytesWitnessed = new HashSet<>();
        for (var transition : dfaState.getTransitions()) {
            var largestSeenByteClass = 0;
            for (var i = transition.getLeft().getStart(); i <= transition.getLeft().getEnd(); i++) {
                var byteClass = byteClasses[i];
                if (byteClass > largestSeenByteClass) {
                    var state = transition.getRight().getStateNumber();
                    if (!bytesWitnessed.contains(byteClass)) {
                        bytesWitnessed.add(byteClass);
                        if (!first) {
                            sb.append(",");
                        }
                        first = false;
                        sb.append(ByteClassUtil.encode(byteClass)).append('-').append(ByteClassUtil.encode(state));
                    }
                    largestSeenByteClass = byteClass;
                }
            }
        }
        // In case we had no transitions
        if (sb.charAt(sb.length() - 1) == ':') {
            sb = new StringBuilder();
        }
        return sb;
    }
}
