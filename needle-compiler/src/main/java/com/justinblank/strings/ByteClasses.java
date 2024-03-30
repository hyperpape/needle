package com.justinblank.strings;

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

}
