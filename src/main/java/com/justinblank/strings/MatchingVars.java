package com.justinblank.strings;

class MatchingVars {
    final boolean forwards;
    final int lengthVar;
    final int stringVar;
    final int charVar;
    final int counterVar;
    final int stateVar;

    MatchingVars(int charVar, int counterVar, int stateVar, int lengthVar, int stringVar) {
        this(true, charVar, counterVar, stateVar, lengthVar, stringVar);
    }

    private MatchingVars(boolean forwards, int charVar, int counterVar, int stateVar,int lengthVar, int stringVar) {
        this.forwards = forwards;
        this.charVar = charVar;
        this.counterVar = counterVar;
        this.stateVar = stateVar;
        this.lengthVar = lengthVar;
        this.stringVar = stringVar;
    }

    static MatchingVars backwards(int charVar, int counterVar, int stateVar, int lengthVar, int stringVar) {
        return new MatchingVars(false, charVar, counterVar, stateVar, lengthVar, stringVar);
    }

}
