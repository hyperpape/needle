package com.justinblank.strings;

import java.util.Objects;

public class FindMethodSpec {

    final DFA dfa;
    final String name;
    final boolean forwards;
    final CompilationPolicy compilationPolicy;

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

    public FindMethodSpec(DFA dfa, String name, boolean forwards, Factorization factorization) {
        Objects.requireNonNull(dfa, "dfa cannot be null");
        Objects.requireNonNull(name, "name cannot be null)");
        Objects.requireNonNull(factorization, "compilationPolicy cannot be null");

        this.dfa = dfa;
        this.name = name;
        this.forwards = forwards;
        this.compilationPolicy = CompilationPolicy.create(factorization);
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

    // TODO: could weaken the "allForwardTransitionsLeadToSameState" condition here, but it's not obvious to me whether
    //  it would be worth it
    // We check spec.dfa.isAccepting() here, because if we have a dfa that matches at zero, looping here doesn't
    // make sense, and getting the loop correct is annoying
    protected boolean canSeekForPredicate() {
        return dfa.forwardTransitionIsPredicate(compilationPolicy) && dfa.allForwardTransitionsLeadToSameState() && dfa.isAccepting();
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
