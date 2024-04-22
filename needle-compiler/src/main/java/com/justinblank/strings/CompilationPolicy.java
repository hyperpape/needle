package com.justinblank.strings;

class CompilationPolicy {

    boolean usedByteClasses = false;
    boolean useByteClassesForAllStates = false;

    // 6 is a wild guess
    final int predicateRangeSizeCutoff = 6;
}
