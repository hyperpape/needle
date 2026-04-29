package com.justinblank.strings.RegexAST;

import java.util.Objects;

public class LengthComponent implements Comparable<LengthComponent> {
    final int min;
    final int max;
    final int size;

    public LengthComponent(int min, int max, int size) {
        this.min = min;
        this.max = max;
        this.size = size;
    }

    public LengthComponent times(int repeat) {
        return new LengthComponent(min * repeat, max * repeat, size);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        LengthComponent that = (LengthComponent) o;
        return min == that.min && max == that.max && size == that.size;
    }

    @Override
    public int hashCode() {
        return Objects.hash(min, max, size);
    }

    @Override
    public int compareTo(LengthComponent o) {
        if (size != o.size) {
            return Integer.compare(size, o.size);
        }
        if (min != o.min) {
            return Integer.compare(min, o.min);
        }
        if (max != o.max) {
            return Integer.compare(max, o.max);
        }
        return 0;
    }
}
