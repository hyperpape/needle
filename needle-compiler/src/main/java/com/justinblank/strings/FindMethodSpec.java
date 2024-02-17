package com.justinblank.strings;

import java.util.Objects;

public class FindMethodSpec {

    final DFA dfa;
    final String name;
    final boolean forwards;

    /**
     * The simplest method--determine if the entire string matches.
     */
    public static final String MATCHES = "Matches";
    /**
     * Determine if the needle is contained anywhere within the string
     */
    public static final String CONTAINEDIN = "ContainedIn";
    /**
     * The backwards matcher. We use this after we've found the end of a match, but need to find the beginning.
     */
    public static final String BACKWARDS = "Backwards";
    /**
     * Allows searching forwards in a string to find the endpoint of a match.
     */
    public static final String FORWARDS = "Forwards";

    public FindMethodSpec(DFA dfa, String name, boolean forwards) {
        Objects.requireNonNull(dfa, "dfa cannot be null");
        Objects.requireNonNull(name, "name cannot be non-null)");

        this.dfa = dfa;
        this.name = name;
        this.forwards = forwards;
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

    public String indexMethod() {
        return "index" + name;
    }

    @Override
    public String toString() {
        return "FindMethodSpec{" +
                "name='" + name + '\'' +
                ", forwards=" + forwards +
                '}';
    }
}
