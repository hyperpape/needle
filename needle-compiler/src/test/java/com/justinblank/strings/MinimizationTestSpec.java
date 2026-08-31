package com.justinblank.strings;

public class MinimizationTestSpec {
    
    public final String pattern;
    public final int stateCount;
    public final int withMinimization;
    
    public MinimizationTestSpec(String pattern, int stateCount, int withMinimization) {
        this.pattern = pattern;
        this.stateCount = stateCount;
        this.withMinimization = withMinimization;
    }
}
