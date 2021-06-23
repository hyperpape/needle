package com.justinblank.strings;

import java.util.Objects;
import java.util.Set;

class Offset {
    int length;
    final Set<Integer> passedStates;
    CharRange charRange;

    @Override
    public String toString() {
        return "Offset{" +
                "length=" + length +
                ", passedStates=" + passedStates +
                ", charRange=" + charRange +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Offset offset = (Offset) o;
        return length == offset.length && Objects.equals(passedStates, offset.passedStates) && Objects.equals(charRange, offset.charRange);
    }

    @Override
    public int hashCode() {
        return Objects.hash(length, passedStates, charRange);
    }

    Offset(int length, Set<Integer> passedStates, CharRange charRange) {
        this.length = length;
        this.passedStates = passedStates;
        this.charRange = charRange;
    }
}
