package com.justinblank.strings;

import com.justinblank.strings.RegexAST.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ThompsonNFABuilder {

    private NFA root;
    private int index = 0;
    private List<NFA> nfaStates = new ArrayList<>();

    public static NFA createNFA(Node ast) {
        ThompsonNFABuilder builder = new ThompsonNFABuilder();
        return builder.build(ast);
    }

    protected NFA build(Node ast) {
        NFA root = createPartial(ast);
        // TODO: kinda hacky
        if (!nfaStates.contains(root)) {
            nfaStates.add(root);
        }
        Collections.reverse(nfaStates);
        NFA finalState = new NFA(true, index++);
        nfaStates.add(finalState);
        for (NFA state : nfaStates) {
            state.setRoot(root);
            if (state != finalState && state.isTerminal()) {
                state.addEpsilonTransition(finalState);
            }
        }
        root.setStates(nfaStates);
        for (NFA nfa : nfaStates) {
            nfa.computeEpsilonClosure();
        }
        root.computeEpsilonClosure();
        root.checkRep();
        return root;
    }

    protected NFA createPartial(Node ast) {
        NFA nfa;
        if (ast instanceof Concatenation) {
            Concatenation c = (Concatenation) ast;
            NFA tail = createPartial(c.tail);
            nfaStates.add(tail);
            NFA head = createPartial(c.head);
            nfaStates.add(head);
            for (NFA terminal : head.terminalStates()) {
                terminal.addTransitions(CharRange.emptyRange(), Collections.singletonList((tail)));
            }
            nfa = head;
        }
        else if (ast instanceof Repetition) {
            Repetition r = (Repetition) ast;
            NFA child = createPartial(r.node);
            nfa = child;
            NFA end = new NFA(false, index++);
            for (NFA terminal : nfa.terminalStates()) {
                terminal.addEpsilonTransition(nfa);
                terminal.addEpsilonTransition(end);
            }
            nfaStates.add(end);
            nfa.addEpsilonTransition(end);
        }
        // TODO: revisit this solution. It will perform badly, and the bytecode
        //  compiler would do better with higher level info about repetitions
        else if (ast instanceof CountedRepetition) {
            CountedRepetition countedRepetition = (CountedRepetition) ast;
            NFA child = new NFA(false, index++);
            // NFA child = createPartial(countedRepetition.node);
            nfa = child;
            NFA end = new NFA(false, index++);
            nfaStates.add(end);
            int repetition = 0;
            for (; repetition < countedRepetition.min; repetition++) {
                NFA child2 = createPartial(countedRepetition.node);
                for (NFA terminal : child.terminalStates()) {
                    terminal.addEpsilonTransition(child2);
                }
                child = child2;
                nfaStates.add(child);
            }
            for (; repetition < countedRepetition.max; repetition++) {
                NFA child2 = createPartial(countedRepetition.node);
                for (NFA terminal : child.terminalStates()) {
                    terminal.addEpsilonTransition(child2);
                    terminal.addEpsilonTransition(end);
                }
                child = child2;
                nfaStates.add(child);
            }
            nfaStates.add(nfa);
        }
        else if (ast instanceof Alternation) {
            Alternation a = (Alternation) ast;
            nfa = new NFA(false, index++);
            NFA left = createPartial(a.left);
            nfaStates.add(left);
            NFA right = createPartial(a.right);
            nfaStates.add(right);
            nfa.addTransitions(CharRange.emptyRange(), List.of(left, right));
            NFA end = new NFA(false, index++);
            nfaStates.add(end);
            left.terminalStates().forEach(n -> n.addEpsilonTransition(end));
            right.terminalStates().forEach(n -> n.addEpsilonTransition(end));
            // nfaStates.add(nfa);
        }
        else if (ast instanceof CharRangeNode) {
            CharRangeNode range = (CharRangeNode) ast;
            nfa = new NFA(false, index++);
            NFA end = new NFA(false, index++);
            nfa.addTransitions(range.range(), Collections.singletonList(end));
            nfaStates.add(end);
        }
        else {
            throw new IllegalStateException("");
        }
        return nfa;
    }

}
