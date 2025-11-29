package com.justinblank.strings;

public class CompilerOptions {

    protected final int flags;
    protected final CharacterDistribution distribution;
    protected final DebugOptions debugOptions;

    public CompilerOptions(int flags, CharacterDistribution distribution, DebugOptions debugOptions) {
        if ((flags & ~Pattern.ALL_FLAGS) != 0) {
            throw new IllegalArgumentException("Unrecognized flags=" + flags);
        }
        this.flags = flags;
        this.distribution = distribution;
        this.debugOptions = debugOptions;
    }

    public static CompilerOptions fromFlags(int flags) {
        return new CompilerOptions(flags, CharacterDistribution.DEFAULT, DebugOptions.none());
    }

    public static CompilerOptions defaultOptions() {
        return fromFlags(0);
    }
}
