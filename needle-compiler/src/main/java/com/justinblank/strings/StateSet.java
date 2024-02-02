package com.justinblank.strings;

import java.util.*;

class StateSet extends HashSet<Integer> {
    /**
     * Maps NFA states to the distance we could have traversed to reach that state. This is necessary for handling
     * search modes (see Russ Cox's article, and incorporate a better explanation). 
     */
    Map<Integer, StateData> stateStarts = new HashMap<>();

    // TODO: fix up how this is set
    boolean seenAccepting;

    @Override
    public boolean add(Integer integer) {
        // TODO: decide if this is lazy...could just not extend hashset
        throw new UnsupportedOperationException("");
    }

    public boolean add(Integer integer, Integer distance, int priority) {
        var currentState = stateStarts.get(integer);
        if (currentState != null) {
            if (currentState.distance < distance) {
                stateStarts.put(integer, new StateData(distance, priority));
            }
        }
        else {
            stateStarts.put(integer, new StateData(distance, priority));
        }
        return super.add(integer);
    }

    public Integer getDistance(Integer state) {
        return stateStarts.get(state).distance;
    }

    public int getPriority(Integer state) {
        return stateStarts.get(state).priority;
    }

    public boolean prune(Integer acceptingState, Integer boundary, int priority) {
        boolean removed = false;
        Iterator<Integer> it = this.iterator();
        while (it.hasNext()) {
            var state = it.next();
            if (state.equals(acceptingState)) {
                continue;
            }
            var stateData = stateStarts.get(state);
            if (stateData.distance < boundary) {
                it.remove();
                removed = true;
                stateStarts.remove(state);
            }
            else if (stateData.priority > priority) {
                it.remove();
                removed = true;
                stateStarts.remove(priority);
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

    static class StateData {
        final int distance;
        final int priority;

        StateData(int distance, int priority) {
            this.distance = distance;
            this.priority = priority;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StateData stateData = (StateData) o;
            return distance == stateData.distance && priority == stateData.priority;
        }

        @Override
        public int hashCode() {
            return Objects.hash(distance, priority);
        }

        @Override
        public String toString() {
            return "StateData{" +
                    "distance=" + distance +
                    ", priority=" + priority +
                    '}';
        }
    }
}