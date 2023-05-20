package com.justinblank.strings;

class CompilationPolicy {

    boolean usedByteClasses = false;
    boolean useByteClassesForAllStates = false;
    boolean stateArraysUseShorts = false;

    String getStateArrayType() {
        return stateArraysUseShorts ? "[S" : "[B";
    }
}
