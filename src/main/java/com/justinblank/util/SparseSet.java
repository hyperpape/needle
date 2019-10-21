package com.justinblank.util;

import java.util.Objects;

/**
 * A sparse set data structure, as described at https://research.swtch.com/sparse. This data structure serves similar
 * purposes to a BitSet, but trades of a substantial increase in space use for an O(1) clear operation and better
 * iteration performance when order of iteration does not matter. Note that the description of using unitialized memory
 * from the initial article does not apply to this implementation (though after a clear operation, stale data will be
 * present, and be ignored, just as in Cox's description.
 */
public class SparseSet {

    private int count;
    private int max;
    private final int[] dense;
    private final int[] other;

    public SparseSet(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Cannot create a sparseset with " + n + " elements");
        }
        max = n;
        count = 0;
        dense = new int[n + 2];
        other = new int[n + 2];
    }

    public boolean add(int n) {
        int effectiveIndex = n + 1;
        int denseLocation = this.other[effectiveIndex];
        if (denseLocation > count || this.dense[denseLocation] != effectiveIndex) {
            this.other[effectiveIndex] = ++count;
            this.dense[count] = effectiveIndex;
            return true;
        }
        return false;
    }

    public boolean remove(int n) {
        int effectiveIndex = n + 1;
        int denseLocation = this.other[effectiveIndex];
        if (denseLocation > count || this.dense[denseLocation] != effectiveIndex) {
            return false;
        }
        this.other[effectiveIndex] = 0;
        int otherLoc = this.dense[count];
        this.other[otherLoc] = denseLocation;
        this.dense[denseLocation] = otherLoc;

        count--;
        return true;
    }

    public boolean contains(int n) {
        int effectiveIndex = n + 1;
        int denseLocation = this.other[effectiveIndex];
        return denseLocation <= count && this.dense[denseLocation] == effectiveIndex;
    }

    public void clear() {
        count = 0;
    }

    public int capacity() {
        return max;
    }

    public int size() {
        return count;
    }

    public int getByIndex(int n) {
        Objects.checkIndex(n, count);
        return this.dense[n + 1] - 1;
    }
}
