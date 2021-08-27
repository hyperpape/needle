package com.justinblank.strings;

class MatchingVars implements Vars {
    static final String STATE = DFAClassBuilder.STATE_FIELD;
    static final String INDEX = DFAClassBuilder.INDEX_FIELD;
    static final String STRING = DFAClassBuilder.STRING_FIELD;
    static final String CHAR = DFAClassBuilder.CHAR_FIELD;
    static final String LENGTH = DFAClassBuilder.LENGTH_FIELD;
    static final String LAST_MATCH = "lastMatch";
    static final String WAS_ACCEPTED = "wasAccepted";

    boolean forwards;
    int lengthVar = -1;
    int stringVar = -1;
    int charVar = -1;
    int counterVar = -1;
    int stateVar = -1;
    int lastMatchVar = -1;
    int wasAcceptedVar = -1;

    MatchingVars(int charVar, int counterVar, int stateVar, int lengthVar, int stringVar) {
        this(true, charVar, counterVar, stateVar, lengthVar, stringVar);
    }

    private MatchingVars(boolean forwards, int charVar, int counterVar, int stateVar, int lengthVar, int stringVar) {
        this.forwards = forwards;
        this.charVar = charVar;
        this.counterVar = counterVar;
        this.stateVar = stateVar;
        this.lengthVar = lengthVar;
        this.stringVar = stringVar;
    }

    public MatchingVars setLengthVar(int lengthVar) {
        this.lengthVar = lengthVar;
        return this;
    }

    public MatchingVars setStringVar(int stringVar) {
        this.stringVar = stringVar;
        return this;
    }

    public MatchingVars setCharVar(int charVar) {
        this.charVar = charVar;
        return this;
    }

    public MatchingVars setCounterVar(int counterVar) {
        this.counterVar = counterVar;
        return this;
    }

    public MatchingVars setStateVar(int stateVar) {
        this.stateVar = stateVar;
        return this;
    }

    public MatchingVars setLastMatchVar(int lastMatchVar) {
        this.lastMatchVar = lastMatchVar;
        return this;
    }

    public MatchingVars setForwards(boolean forwards) {
        this.forwards = forwards;
        return this;
    }

    public MatchingVars setWasAcceptedVar(int wasAcceptedVar) {
        this.wasAcceptedVar = wasAcceptedVar;
        return this;
    }

    public int indexByName(String name) {
        switch (name) {
            case STATE:
                return this.stateVar;
            case INDEX:
                return this.counterVar;
            case STRING:
                return this.stringVar;
            case CHAR:
                return this.charVar;
            case LENGTH:
                return this.lengthVar;
            case LAST_MATCH:
                return this.lastMatchVar;
            case WAS_ACCEPTED:
                return this.wasAcceptedVar;
            default:
                throw new IllegalArgumentException("Illegal argument for variable lookup: " + name);
        }
    }
}
