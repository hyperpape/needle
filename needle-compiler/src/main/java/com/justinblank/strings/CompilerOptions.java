package com.justinblank.strings;

public class CompilerOptions {

    protected final int flags;
    protected final CharacterDistribution distribution;
    protected final DebugOptions debugOptions;

    public CompilerOptions(int flags, CharacterDistribution distribution, DebugOptions debugOptions) {
        this.flags = flags;
        this.distribution = distribution;
        this.debugOptions = debugOptions;
    }

    public static CompilerOptions fromFlags(int flags) {
        return new CompilerOptions(flags, CharacterDistribution.DEFAULT, DebugOptions.none());
    }

    public static CompilerOptions defaultOptions() {
        return new CompilerOptions(0, CharacterDistribution.DEFAULT, DebugOptions.none());
    }
}
