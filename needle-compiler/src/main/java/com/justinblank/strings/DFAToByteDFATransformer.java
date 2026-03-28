package com.justinblank.strings;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class DFAToByteDFATransformer {

    private final DFA root;
    private final AtomicInteger stateCount = new AtomicInteger(1);
    private final Map<Integer, DFA> transformationMap = new HashMap<>();
    private final DFA transformed;

    DFAToByteDFATransformer(DFA root) {
        this.root = root;
        transformed = DFA.root(root.isAccepting());
    }


    static DFA toByteDFA(DFA root) {
        return new DFAToByteDFATransformer(root).convert();
    }

    DFA convert() {
        DFA allDead = new DFA(transformed, false, 1);

        Queue<DFA> pending = new ArrayDeque<>();
        Set<DFA> seen = new HashSet<>();
        pending.add(root);
        transformationMap.put(0, transformed);
        while (!pending.isEmpty()) {
            DFA next = pending.poll();
            if (seen.contains(next)) {
                continue;
            }
            var nextState = transformationMap.get(next.getStateNumber());
            for (var transition : next.getTransitions()) {
                DFA transitionTarget = transformationMap.computeIfAbsent(transition.getRight().getStateNumber(), n -> new DFA(transformed, transition.getRight().isAccepting(), stateCount.incrementAndGet()));
                pending.add(transition.getRight());
                char start = transition.getLeft().getStart();
                char end = transition.getLeft().getEnd();

                char firstHighChar = (char) (start >> 8);
                char finalHighChar = (char) (end >> 8);
                // Only problem is we have no transitions from odds to dead state
                if (firstHighChar == finalHighChar) {
                    DFA afterFirstByte = determineFirstByteTransition(nextState, firstHighChar, finalHighChar);
                    afterFirstByte.addTransition(new CharRange((char) (start & 0xFF), (char) (end & 0xFF)), transitionTarget);
                }
                else {
                    {
                        DFA afterFirstByte = new DFA(transformed, false, stateCount.incrementAndGet());
                        nextState.addTransition(new CharRange(firstHighChar, firstHighChar), afterFirstByte);
                        afterFirstByte.addTransition(new CharRange((char) (start & 0xFF), (char) 255), transitionTarget);
                    }
                    if (firstHighChar + 1 < finalHighChar) {
                        DFA afterFirstByte = new DFA(transformed, false, stateCount.incrementAndGet());
                        nextState.addTransition(new CharRange((char) (firstHighChar + 1), (char) (finalHighChar - 1)), afterFirstByte);
                        afterFirstByte.addTransition(new CharRange((char) 0, (char) 255), transitionTarget);
                    }
                    {
                        DFA afterFirstByte = new DFA(transformed, false, stateCount.incrementAndGet());
                        nextState.addTransition(new CharRange(finalHighChar, finalHighChar), afterFirstByte);
                        afterFirstByte.addTransition(new CharRange((char) 0, (char) (end & 0xFF)), transitionTarget);
                    }
                }
            }
            seen.add(next);
        }
        return transformed;
    }

    private DFA determineFirstByteTransition(DFA nextState, char firstHighChar, char finalHighChar) {
        DFA afterFirstByte = nextState.transition(firstHighChar);
        if (afterFirstByte == null) {
            afterFirstByte = new DFA(transformed, false, stateCount.incrementAndGet());
            nextState.addTransition(new CharRange(firstHighChar, finalHighChar), afterFirstByte);
        }
        return afterFirstByte;
    }
}
