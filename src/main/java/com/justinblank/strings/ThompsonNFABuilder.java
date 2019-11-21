package com.justinblank.strings;

import com.justinblank.strings.RegexAST.*;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

class ThompsonNFABuilder {

    private NFA root;
    private int index = 0;
    private List<NFA> nfaStates = new ArrayList<>();

    public static NFA createNFA(Node ast) {
        ThompsonNFABuilder builder = new ThompsonNFABuilder();
        NFA nfa = builder.build(ast);
        nfa.regexInstrs = ThompsonRegexInstrBuilder.createNFA(ast);
        return nfa;
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
        nfaStates = new ArrayList<>(new HashSet<>(nfaStates)); // TODO: don't be lazy
        nfaStates.sort(Comparator.comparingInt(NFA::getState));
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
            for (NFA terminal : findTerminalStates(head)) {
                terminal.addTransitions(CharRange.emptyRange(), Collections.singletonList((tail)));
            }
            nfa = head;
        }
        else if (ast instanceof Repetition) {
            Repetition r = (Repetition) ast;
            NFA child = createPartial(r.node);
            nfa = child;
            NFA end = new NFA(false, index++);
            for (NFA terminal : findTerminalStates(nfa)) {
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
                for (NFA terminal : findTerminalStates(child)) {
                    terminal.addEpsilonTransition(child2);
                }
                child = child2;
                nfaStates.add(child);
            }
            for (; repetition < countedRepetition.max; repetition++) {
                NFA child2 = createPartial(countedRepetition.node);
                for (NFA terminal : findTerminalStates(child)) {
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
            findTerminalStates(left).forEach(n -> n.addEpsilonTransition(end));
            findTerminalStates(right).forEach(n -> n.addEpsilonTransition(end));
        }
        else if (ast instanceof CharRangeNode) {
            CharRangeNode range = (CharRangeNode) ast;
            nfa = new NFA(false, index++);
            NFA end = new NFA(false, index++);
            nfa.addTransitions(range.range(), Collections.singletonList(end));
            nfaStates.add(end);
        }
        else if (ast instanceof LiteralNode) {
            nfa = new NFA(false, index++);
            NFA current = nfa;
            String s = ((LiteralNode) ast).getLiteral();
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                NFA next = new NFA(false, index++);
                current.addTransitions(new CharRange(c, c), Collections.singletonList(next));
                nfaStates.add(current);
                current = next;
            }
            NFA end = new NFA(false, index++);
            current.addTransitions(CharRange.emptyRange(), Collections.singletonList(end));
            nfaStates.add(current);
            nfaStates.add(end);
        }
        else {
            throw new IllegalStateException("Unhandled ast node type=" + ast.getClass().getSimpleName());
        }
        return nfa;
    }

    protected List<NFA> findTerminalStates(NFA targetNFA) {
        List<NFA> terminals = new ArrayList<>();
        BitSet seen = new BitSet(nfaStates.size());
        Queue<NFA> pending = new LinkedList<>();
        pending.add(targetNFA);
        seen.set(targetNFA.getState());
        if (targetNFA.getTransitions().isEmpty()) {
            terminals.add(targetNFA);
        }
        while (!pending.isEmpty()) {
            NFA nfa = pending.poll();
            seen.set(nfa.getState());
            for (Pair<CharRange, List<NFA>> transition : nfa.getTransitions()) {
                for (NFA reachable : transition.getRight()) {
                    if (!seen.get(reachable.getState())) {
                        if (reachable.isTerminal()) {
                            terminals.add(reachable);
                        }
                        pending.add(reachable);
                        seen.set(reachable.getState());
                    }
                }
            }
        }
        return terminals;
    }

}
