package com.justinblank.strings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StateSetTest {

    @Test
    void addNewState() {
        var set = new StateSet();
        assertTrue(set.add(1, 3, 2));
        assertEquals(3, set.getDistance(1));
        assertEquals(2, set.getPriority(1));
    }

    @Test
    void largerDistanceWinsRegardlessOfPriority() {
        var set = new StateSet();
        set.add(1, 3, 5);
        set.add(1, 1, 1);
        assertEquals(3, set.getDistance(1));
        assertEquals(5, set.getPriority(1));
    }

    @Test
    void equalDistanceKeepsBestPriorityRegardlessOfAddOrder() {
        var first = new StateSet();
        first.add(1, 3, 2);
        first.add(1, 3, 1);
        var second = new StateSet();
        second.add(1, 3, 1);
        second.add(1, 3, 2);
        assertEquals(first.getPriority(1), second.getPriority(1));
        assertEquals(1, first.getPriority(1));
    }

    @Test
    void worsePriorityDoesNotReplaceAtEqualDistance() {
        var set = new StateSet();
        set.add(1, 3, 1);
        set.add(1, 3, 4);
        assertEquals(1, set.getPriority(1));
    }

    @Test
    void mergedDataIsOrderIndependent() {
        var data = new int[][]{{1, 0, 3}, {2, 1, 1}, {1, 2, 2}, {2, 1, 4}, {3, 2, 1}};
        var forward = new StateSet();
        for (var entry : data) {
            forward.add(entry[0], entry[1], entry[2]);
        }
        var backward = new StateSet();
        for (int i = data.length - 1; i >= 0; i--) {
            var entry = data[i];
            backward.add(entry[0], entry[1], entry[2]);
        }
        assertEquals(forward.states, backward.states);
        for (var state : forward.states) {
            assertEquals(forward.getDistance(state), backward.getDistance(state));
            assertEquals(forward.getPriority(state), backward.getPriority(state));
        }
        assertEquals(2, forward.getDistance(1));
        assertEquals(2, forward.getPriority(1));
        assertEquals(1, forward.getDistance(2));
        assertEquals(1, forward.getPriority(2));
    }

    @Test
    void pruneRemovesLaterRestartsAndLowerPrecedenceThreads() {
        var set = new StateSet();
        set.add(1, 3, 1);
        set.add(2, 1, 1);
        set.add(3, 3, 2);
        set.add(4, 4, 1);
        assertTrue(set.prune(1, 3, 1));
        // state 2 restarted later (distance 1 < 3) and state 3 has lower precedence (priority 2 > 1); state 4
        // restarted earlier with equal precedence, so it survives
        assertFalse(set.states.contains(2));
        assertFalse(set.states.contains(3));
        assertTrue(set.states.contains(4));
        assertFalse(set.prune(1, 3, 1));
    }
}