package com.justinblank.strings;

public class DebugOptions {

    final boolean printInstructions;
    final boolean writeClassToFs;
    final boolean trackStates;
    final boolean printDFAs;

    public DebugOptions(boolean printInstructions, boolean writeClassToFs, boolean trackStates, boolean printDFAs) {
        this.printInstructions = printInstructions;
        this.writeClassToFs = writeClassToFs;
        this.trackStates = trackStates;
        this.printDFAs = printDFAs;
    }

    public static DebugOptions none() {
        return new DebugOptions(false, false, false, false);
    }

    public static DebugOptions all() {
        return new DebugOptions(true, true, true, true);
    }

    public static DebugOptions writeToFsOnly() {
        return new DebugOptions(false, true, false, false);
    }

    public boolean isDebug() {
        return printInstructions || writeClassToFs || trackStates || printDFAs;
    }
}
