package com.justinblank.strings;

import com.justinblank.strings.RegexAST.*;

import java.util.Arrays;
import java.util.Collections;

// TODO: This construction is like the Thompson construction. Verify and name appropriately
class ASTToNFA {

    public static NFA createNFA(Node ast) {
        NFA nfa = createPartial(ast);
        NFA finalState = new NFA(true);
        for (NFA terminal : nfa.terminalStates()) {
            terminal.addEpsilonTransition(finalState);
        }
        return nfa;
    }

    protected static NFA createPartial(Node ast) {
        NFA nfa;
        if (ast instanceof Concatenation) {
            Concatenation c = (Concatenation) ast;
            NFA head = createPartial(c.head);
            NFA tail = createPartial(c.tail);
            for (NFA terminal : head.terminalStates()) {
                terminal.addTransitions(CharRange.emptyRange(), Collections.singletonList((tail)));
            }
            nfa = head;
        }
        else if (ast instanceof Repetition) {
            Repetition r = (Repetition) ast;
            NFA child = createPartial(r.node);
            nfa = child;
            NFA end = new NFA(false);
            for (NFA terminal : nfa.terminalStates()) {
                terminal.addEpsilonTransition(nfa);
                terminal.addEpsilonTransition(end);
            }
            nfa.addEpsilonTransition(end);
        }
        else if (ast instanceof Alternation) {
            Alternation a = (Alternation) ast;
            nfa = new NFA(false);
            NFA left = createPartial(a.left);
            NFA right = createPartial(a.right);
            nfa.addTransitions(CharRange.emptyRange(), Arrays.asList(left, right));
            NFA end = new NFA(false);
            left.addEpsilonTransition(end);
            right.addEpsilonTransition(end);
        }
        else if (ast instanceof CharRangeNode) {
            CharRangeNode range = (CharRangeNode) ast;
            nfa = new NFA(false);
            NFA end = new NFA(false);
            nfa.addTransitions(range.range(), Collections.singletonList(end));
        }
        else {
            throw new IllegalStateException("");
        }
        return nfa;
    }

}
