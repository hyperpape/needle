package com.justinblank.strings;

import java.util.*;

class StateSet {
    /**
     * Maps NFA states to the distance we could have traversed to reach that state. This is necessary for handling
     * search modes (see Russ Cox's article, and incorporate a better explanation). 
     */
    Map<Integer, StateData> stateStarts = new HashMap<>();
    Set<Integer> states = new HashSet<>();

    // TODO: fix up how this is set
    boolean seenAccepting;

    /**
     * Adds the state, or merges in the reaching thread's data. When multiple threads reach the same state, the one
     * that would produce the better match owns it: the larger distance (an earlier restart, so an earlier match
     * start) wins, and at equal distance the smaller priority value (higher parse precedence) wins. The result is
     * independent of the order in which threads are added.
     */
    public boolean add(Integer integer, Integer distance, int priority) {
        var currentState = stateStarts.get(integer);
        if (currentState == null || currentState.distance < distance
                || (currentState.distance == distance && currentState.priority > priority)) {
            stateStarts.put(integer, new StateData(distance, priority));
        }
        return states.add(integer);
    }

    public Integer getDistance(Integer state) {
        return stateStarts.get(state).distance;
    }

    public int getPriority(Integer state) {
        return stateStarts.get(state).priority;
    }

    public boolean prune(Integer acceptingState, Integer boundary, int priority) {
        boolean removed = false;
        Iterator<Integer> it = states.iterator();
        while (it.hasNext()) {
            var state = it.next();
            if (state.equals(acceptingState)) {
                continue;
            }
            var stateData = stateStarts.get(state);
            // A thread that started later than the accepting thread loses to the accepting match on leftmost
            // grounds; a thread that started equally early loses on precedence. A thread that started earlier must
            // survive regardless of precedence: its matches beat the accepting match under leftmost-first.
            if (stateData.distance < boundary
                    || (stateData.distance == boundary && priority < stateData.priority)) {
                it.remove();
                removed = true;
                stateStarts.remove(state);
            }
        }
        return removed;
    }

    @Override
    public String toString() {
        if (states.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        for (var state : states) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(state);
        }
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        StateSet stateSet = (StateSet) o;
        return Objects.equals(states, stateSet.states);
    }

    @Override
    public int hashCode() {
        return states.hashCode();
    }

    public Collection<Integer> getStates() {
        return states;
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