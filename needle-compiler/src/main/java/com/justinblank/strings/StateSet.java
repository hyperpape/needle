package com.justinblank.strings;

import java.util.*;

class StateSet extends HashSet<Integer> {
    /**
     * Maps NFA states to the distance we could have traversed to reach that state. This is necessary for handling
     * search modes (see Russ Cox's article, and incorporate a better explanation). 
     */
    Map<Integer, Integer> stateStarts = new HashMap<>();

    // TODO: fix up how this is set
    boolean seenAccepting;

    @Override
    public boolean add(Integer integer) {
        // TODO: decide if this is lazy...could just not extend hashset
        throw new UnsupportedOperationException("");
    }

    public boolean add(Integer integer, Integer distance) {
        var currentState = stateStarts.get(integer);
        if (currentState != null) {
            if (currentState < distance) {
                stateStarts.put(integer, distance);
            }
        }
        else {
            stateStarts.put(integer, distance);
        }
        return super.add(integer);
    }

    public Integer getDistance(Integer state) {
        return stateStarts.get(state);
    }

    public boolean prune(Integer acceptingState, Integer boundary) {
        boolean removed = false;
        Iterator<Integer> it = this.iterator();
        while (it.hasNext()) {
            var state = it.next();
            if (state.equals(acceptingState)) {
                continue;
            }
            var distance = stateStarts.get(state);
            if (distance < boundary) {
                it.remove();
                removed = true;
                stateStarts.remove(state);
            }
        }
        return removed;
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        for (var state : this) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(state);
        }
        sb.append('}');
        return sb.toString();
    }
}