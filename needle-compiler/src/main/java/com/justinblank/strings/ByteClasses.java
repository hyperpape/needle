package com.justinblank.strings;

class ByteClasses {

    static byte CATCHALL_INVALID = -1;

    final byte[] ranges;
    final byte catchAll;

    public ByteClasses(byte[] ranges, byte catchAll) {
        this.ranges = ranges;
        this.catchAll = catchAll;
    }

}
