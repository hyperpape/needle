package com.justinblank.util;

import org.junit.jupiter.api.Test;
import org.quicktheories.core.Gen;
import org.quicktheories.generators.IntegersDSL;
import org.quicktheories.generators.ListsDSL;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.quicktheories.QuickTheory.qt;

class SparseSetTest {

    @Test
    void sparseSetAddAndClear() {
        SparseSet sparseSet = new SparseSet(10);
        assertEquals(10, sparseSet.capacity());

        assertEquals(-1, sparseSet.indexOf(0));
        sparseSet.add(0);
        assertEquals(0, sparseSet.indexOf(0));
        assertTrue(sparseSet.contains(0));
        sparseSet.add(10);
        assertEquals(1, sparseSet.indexOf(10));
        sparseSet.add(0);
        assertEquals(0, sparseSet.indexOf(0));

        sparseSet.clear();
        assertEquals(-1, sparseSet.indexOf(0));
        assertFalse(sparseSet.contains(0));
        sparseSet.add(10);
        assertEquals(0, sparseSet.indexOf(10));
        assertTrue(sparseSet.contains(10));
        sparseSet.remove(10);
        assertFalse(sparseSet.contains(10));
    }

    @Test
    void indexOfHandlesGreaterThanMaxArguments() {
        SparseSet sparseSet = new SparseSet(10);
        assertEquals(-1, sparseSet.indexOf(11));
        assertEquals(-1, sparseSet.indexOf(100));
    }

    @Test
    void operationSequence() {
        SparseSet set = new SparseSet(10);
        set.add(1);
        set.add(2);
        set.add(3);
        set.remove(2);
        assertEquals(2, set.size());
        assertTrue(set.contains(1));
        assertTrue(set.contains(3));
        assertFalse(set.contains(2));
    }

    @Test
    void add() {
        SparseSet sparseSet = new SparseSet(100);
        qt().forAll(new IntegersDSL().between(0, 100)).check((i) -> {
            sparseSet.add(i);
            return sparseSet.contains(i);
        });
    }

    @Test
    void addAndClear() {
        SparseSet sparseSet = new SparseSet(100);
        qt().forAll(new IntegersDSL().between(0, 100)).check((i) -> {
            sparseSet.add(i);
            if (!sparseSet.contains(i)) {
                return false;
            }
            sparseSet.clear();
            return !sparseSet.contains(i);
        });
    }

    @Test
    void addAndRemove() {
        SparseSet sparseSet = new SparseSet(100);
        qt().forAll(new IntegersDSL().between(0, 100)).check((i) -> {
            sparseSet.add(i);
            if (!sparseSet.contains(i)) {
                return false;
            }
            boolean removed = sparseSet.remove(i);
            return removed && !sparseSet.contains(i);
        });
    }

    @Test
    void generatedSequences() {
        Gen<List<Integer>> gen = new ListsDSL().of(new IntegersDSL().between(0, 301)).ofSizeBetween(0, 100);
        qt().forAll(gen).check((integers) -> {
            Set<Integer> referenceSet = new HashSet<>();
            SparseSet sparseSet = new SparseSet(100);
            for (Integer i : integers) {
                if (i < 100) {
                    if (referenceSet.add(i) != sparseSet.add(i)) {
                        return false;
                    }
                }
                else if (i < 200) {
                   if (referenceSet.remove(i - 100) != sparseSet.remove(i - 100)) {
                       return false;
                   }
                }
                else if (i < 300) {
                    boolean refContained = referenceSet.contains(i - 200);
                    boolean sparseContained = sparseSet.contains(i  - 200);
                    if (refContained != sparseContained) {
                        return false;
                    }
                }
                else if (i == 301) {
                    referenceSet.clear();
                    sparseSet.clear();
                }
                if (referenceSet.size() != sparseSet.size()) {
                    return false;
                }
                for (Integer contained : referenceSet) {
                    if (!sparseSet.contains(contained)) {
                        return false;
                    }
                }
                for (int j = 0; j < sparseSet.size(); j++) {
                    int n = sparseSet.getByIndex(j);
                    if (!referenceSet.contains(n)) {
                        return false;
                    }
                    if (sparseSet.indexOf(n) != j) {
                        return false;
                    }
                }
            }
            return true;
        });
    }

    @Test
    void canAddMax() {
        SparseSet set = new SparseSet(10);
        set.add(10);
    }

    @Test
    void checkOutOfBoundsAdd() {
        SparseSet set = new SparseSet(10);
        assertThrows(IndexOutOfBoundsException.class, () ->
            set.add(11));
    }

    @Test
    void checkOutOfBoundsGetByIndex() {
        SparseSet sparseSet = new SparseSet(10);
        assertThrows(IndexOutOfBoundsException.class, () ->
            sparseSet.getByIndex(1));
    }

    @Test
    void testToString() {
        SparseSet set = new SparseSet(10);
        assertEquals("[]", set.toString());
        set.add(10);
        set.add(8);
        assertEquals("[10, 8]", set.toString());
        set.add(7);
        assertEquals("[10, 8, 7]", set.toString());
        set.clear();
        assertEquals("[]", set.toString());
    }

    @Test
    void toStringSequences() {
        Gen<List<Integer>> gen = new ListsDSL().of(new IntegersDSL().between(0, 301)).ofSizeBetween(0, 100);
        qt().forAll(gen).check((integers) -> {
            Set<Integer> referenceSet = new HashSet<>();
            SparseSet sparseSet = new SparseSet(100);
            for (Integer i : integers) {
                if (i < 100) {
                    boolean refSetAdd = referenceSet.add(i);
                    boolean sparseSetAdd = sparseSet.add(i);
                    if (refSetAdd != sparseSetAdd) {
                        return false;
                    }
                } else if (i < 200) {
                    referenceSet.remove(i - 100);
                    sparseSet.remove(i - 100);
                } else if (i < 300) {
                    boolean refContained = referenceSet.contains(i - 200);
                    boolean sparseContained = sparseSet.contains(i - 200);
                    if (refContained != sparseContained) {
                        return false;
                    }
                } else if (i == 301) {
                    referenceSet.clear();
                    sparseSet.clear();
                }
                for (Integer contained : referenceSet) {
                    if (!sparseSet.toString().contains(contained.toString())) {
                        return false;
                    }
                }
            }
            return true;
        });
    }

    @Test
    void negativeCapacityNotAllowed() {
        assertThrows(IllegalArgumentException.class, () -> {
            SparseSet sparseSet = new SparseSet(-1);
        });
    }

    @Test
    void zeroArgumentsAreAllowed() {
        SparseSet sparseSet = new SparseSet(0);
        sparseSet.add(0);
    }
}
