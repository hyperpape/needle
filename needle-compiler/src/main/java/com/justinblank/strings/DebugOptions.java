package com.justinblank.strings;

public class DebugOptions {

    final boolean printInstructions;
    final boolean writeClassToFs;
    final boolean trackStates;

    public DebugOptions(boolean printInstructions, boolean writeClassToFs, boolean trackStates) {
        this.printInstructions = printInstructions;
        this.writeClassToFs = writeClassToFs;
        this.trackStates = trackStates;
    }

    public static DebugOptions none() {
        return new DebugOptions(false, false, false);
    }

    public boolean isDebug() {
        return printInstructions || writeClassToFs || trackStates;
    }
}
