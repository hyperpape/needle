package com.justinblank.strings;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Processes data for the state transition arrays that map byteClasses to target states.
 */
class DFAStateTransitions {

    protected ByteClasses byteClasses;

    /**
     * Map from strings representing the search methods (e.g) "STATES_FORWARD" to sets of strings encoding
     * state transitions. For the encoding, see {@link ByteClassUtil}.
     */
    protected Map<String, Set<String>> byteClassStringMaps = new HashMap<>();

    protected void addStateTransitionString(FindMethodSpec spec, DFA dfaState) {
        byteClassStringMaps.computeIfAbsent(spec.statesConstant(), (s) -> new HashSet<>());
        StringBuilder sb = buildByteClassString(dfaState);
        if (sb.length() != 0) {
            byteClassStringMaps.get(spec.statesConstant()).add(sb.toString());
        }
    }

    boolean willUseByteClasses() {
        return byteClasses != null;
    }

    StringBuilder buildByteClassString(DFA dfaState) {
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        sb.append(ByteClassUtil.encode(dfaState.getStateNumber())).append(ByteClassUtil.STATE_TO_TRANSITIONS_DELINEATOR);
        Set<Byte> bytesWitnessed = new HashSet<>();
        for (var transition : dfaState.getTransitions()) {
            var largestSeenByteClass = 0;
            for (var i = transition.getLeft().getStart(); i <= transition.getLeft().getEnd(); i++) {
                byte byteClass;
                if (i >= 128) {
                    byteClass = byteClasses.catchAll;
                }
                else {
                    byteClass = byteClasses.ranges[i];
                }
                if (byteClass == 0 || byteClass > largestSeenByteClass) {
                    var state = transition.getRight().getStateNumber();
                    if (!bytesWitnessed.contains(byteClass)) {
                        bytesWitnessed.add(byteClass);
                        if (!first) {
                            sb.append(ByteClassUtil.BYTE_CLASS_DELINEATOR);
                        }
                        first = false;
                        sb.append(ByteClassUtil.encode(byteClass)).append(ByteClassUtil.STATE_TRANSITION_DELINEATOR).append(ByteClassUtil.encode(state));
                    }
                    if (!(i > 128)) {
                        largestSeenByteClass = byteClass;
                    }
                }
                if (i > 128) {
                    break;
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
