package com.justinblank.strings;

import java.util.Objects;

class FindMethodSpec {

    final DFA dfa;
    final String name;

    public FindMethodSpec(DFA dfa, String name) {
        Objects.requireNonNull(dfa, "dfa cannot be null");
        Objects.requireNonNull(name, "name cannot be non-null)");

        this.dfa = dfa;
        this.name = name;
    }

    int statesCount() {
        return dfa.statesCount();
    }

    String statesConstant() {
        if (name.isEmpty()) {
            return "STATES";
        }
        return ("STATES_" + name).toUpperCase();
    }

    public String wasAcceptedName() {
        return "wasAccepted" + name;
    }

    public String wasAcceptedSetName() {

        if (name.isEmpty()) {
            return "ACCEPTED_SET";
        }
        else {
            return "ACCEPTED_SET_" + name;
        }
    }

    public String stateArrayName(int stateNumber) {
        if (name.isEmpty()) {
            return "stateTransitions" + stateNumber;
        }
        else {
            return "stateTransitions" + name + stateNumber;
        }

    }
}
