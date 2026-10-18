package com.justinblank.strings;

import java.util.Arrays;
import java.util.Objects;

class ByteClasses {

    static byte CATCHALL_INVALID = -1;

    final byte[] ranges;
    final byte catchAll;

    final int byteClassCount;

    public ByteClasses(byte[] ranges, byte catchAll, int byteClassCount) {
        this.ranges = ranges;
        this.catchAll = catchAll;
        this.byteClassCount = byteClassCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ByteClasses that = (ByteClasses) o;
        return catchAll == that.catchAll && byteClassCount == that.byteClassCount && Arrays.equals(ranges, that.ranges);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(ranges), catchAll, byteClassCount);
    }
}
