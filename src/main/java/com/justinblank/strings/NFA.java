package com.justinblank.strings;

import com.justinblank.util.SparseSet;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

import static com.justinblank.strings.RegexInstr.Opcode.*;

public class NFA {

    private NFA root;
    private final int state;
    // This will only be non-null on the root
    private List<NFA> states;
    private boolean accepting;
    private List<Pair<CharRange, List<NFA>>> transitions = new ArrayList<>();
    private Set<NFA> epsilonClosure;
    private BitSet epsilonClosureIndices = new BitSet();
    private boolean isTerminal = true;
    List<RegexInstr> regexInstrs;

    protected NFA(boolean accepting, int index) {
        this.accepting = accepting;
        this.state = index;
    }

    public static NFA createNFA(String regex) {
        return ThompsonNFABuilder.createNFA(RegexParser.parse(regex));
    }

    protected void addTransitions(CharRange charRange, List<NFA> nfas) {
        for (NFA nfa : nfas) {
            nfa.root = this.root;
        }
        if (isTerminal) {
            for (NFA nfa : nfas) {
                if (nfa != this) {
                    isTerminal = false;
                }
            }
        }
        transitions.add(Pair.of(charRange, nfas));
        // we trust that our character ranges don't overlap
        transitions.sort(Comparator.comparingInt(p -> p.getLeft().getStart()));
    }

    protected void addEpsilonTransition(NFA end) {
        addTransitions(CharRange.emptyRange(), Collections.singletonList(end));
    }

    protected List<Pair<CharRange, List<NFA>>> getTransitions() {
        return transitions;
    }

    protected NFA getRoot() {
        return root;
    }

    protected void setRoot(NFA root) {
        this.root = root;
    }

    protected boolean isAccepting() {
        return accepting;
    }

    public List<NFA> getStates() {
        return states;
    }

    public void setStates(List<NFA> states) {
        this.states = states;
    }

    protected int getState() {
        return state;
    }

    protected NFA getState(int state) {
        return states.get(state);
    }

    protected BitSet transition(char c) {
        for (Pair<CharRange, List<NFA>> transition : transitions) {
            if (transition.getLeft().inRange(c)) {
                BitSet bitSet = new BitSet();
                for (NFA nfa : transition.getRight()) {
                    bitSet.or(nfa.epsilonClosureIndices);
                }
                return bitSet;
            }
        }
        return new BitSet();
    }

    public boolean matches(String s) {
        int length = s.length();
        SparseSet currentStates = new SparseSet(states.size() - 1);
        currentStates.add(0);
        SparseSet newStates = new SparseSet(states.size() - 1);

        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            for (int stateIndex = 0; stateIndex < currentStates.size(); stateIndex++) {
                int examinedState = currentStates.getByIndex(stateIndex);
                RegexInstr regexInstr = regexInstrs.get(examinedState);
                RegexInstr.Opcode opcode = regexInstr.opcode;
                // Current matches came from the previous iteration and should be ignored
                if (opcode == MATCH) {
                    continue;
                }
                if (opcode == JUMP) {
                    examinedState = regexInstr.target1;
                    regexInstr = regexInstrs.get(examinedState);
                    opcode = regexInstr.opcode;
                }
                if (opcode == SPLIT) {
                    currentStates.add(regexInstr.target1);
                    currentStates.add(regexInstr.target2);
                    continue;
                }
                if (c >= regexInstr.start && c <= regexInstr.end) {
                    newStates.add(examinedState + 1);
                }
            }
            SparseSet tmp = currentStates;
            currentStates = newStates;
            newStates = tmp;
            newStates.clear();
        }
        return statesMatched(currentStates);
    }

    private boolean statesMatched(SparseSet states) {
        for (int i = 0; i < states.size(); i++) {
            int stateIndex = states.getByIndex(i);
            RegexInstr instr = regexInstrs.get(stateIndex);
            if (instr.opcode == JUMP) {
                instr = regexInstrs.get(instr.target1);
            }

            if (instr.opcode == MATCH) {
                return true;
            }
            else if (instr.opcode == SPLIT) {
                if (regexInstrs.get(instr.target1).opcode == MATCH) {
                    return true;
                }
                else {
                    states.add(instr.target1);
                }
                if (regexInstrs.get(instr.target2).opcode == MATCH) {
                    return true;
                }
                else {
                    states.add(instr.target2);
                }
            }
        }
        return false;
    }

    public MatchResult search(String s) {
        int length = s.length();
        int lastStart = Integer.MAX_VALUE;
        int lastEnd = -1;
        int i = 0;
        Set<NFA> initial = epsilonClosure();
        Set<NFA> current = initial;
        Map<NFA, Integer> stateMap = new HashMap<>();
        for (; i < length; i++) {
            // If any of our states can accept, we've found a match.
            // Keep searching for a longer match, but store the current start and end indices
            // we will no longer consider anything that starts after this starting location
            boolean accepting = hasAcceptingState(current);
            if (accepting) {
                lastStart = computeLastStart(stateMap, i);
                lastEnd = i;
            }

            current.addAll(initial);
            final int currentIndex = i;
            char c = s.charAt(i);
            Set<NFA> next = new HashSet<>();
            Map<NFA, Integer> newStateMap = new HashMap<>();
            Map<NFA, Integer> existingStateMap = stateMap;
            for (NFA node : current) {
                BitSet bitSet = node.transition(c);
                bitSet.stream().forEach(nfaIndex -> {
                    newStateMap.compute(states.get(nfaIndex), (nfa, start) -> {
                        if (start == null) {
                            start = currentIndex;
                        }
                        int startingPoint = Math.min(start, existingStateMap.getOrDefault(node, currentIndex));
                        if (startingPoint == Integer.MAX_VALUE) {
                            throw new IllegalStateException("");
                        }
                        return startingPoint;
                    });
                });
            }
            // Now, only keep states if they can match a range shorter than the current longest match
            final int currentLastStart = lastStart;
            stateMap.clear();
            newStateMap.forEach((key, value) -> {
                if (value <= currentLastStart) {
                    next.add(key);
                    stateMap.put(key, value);
                }
            });
            current = next;
        }
        boolean accepting = current.stream().anyMatch(NFA::isAccepting);
        if (accepting) {
            int thisStart = i;
            for (Map.Entry<NFA, Integer> e : stateMap.entrySet()) {
                if (e.getKey().accepting) {
                    thisStart = Math.min(thisStart, e.getValue());
                }
            }
            if (lastStart == Integer.MAX_VALUE) {
                lastStart = thisStart;
            }
            if (lastStart > thisStart) {
                return new MatchResult(true, lastStart, lastEnd);
            }
        }
        else if (lastStart != Integer.MAX_VALUE) {
            return new MatchResult(true, lastStart, lastEnd);
        }

        boolean matched = current.stream().anyMatch(NFA::isAccepting);
        return new MatchResult(matched, lastStart, i);
    }

    protected static boolean hasAcceptingState(Collection<NFA> nfas) {
        for (NFA nfa : nfas) {
            if (nfa.isAccepting()) {
                return true;
            }
        }
        return false;
    }

    protected int computeLastStart(Map<NFA, Integer> stateMap, int i) {
        int thisStart = i;
        for (Map.Entry<NFA, Integer> e : stateMap.entrySet()) {
            if (e.getKey().accepting) {
                thisStart = Math.min(thisStart, e.getValue());
            }
        }
        return thisStart;
    }

    protected Set<NFA> epsilonClosure() {
        return this.epsilonClosure;
    }

    protected static Set<NFA> epsilonClosure(Collection<NFA> nfaStates) {
        Set<NFA> closure = new HashSet<>();
        for (NFA nfa : nfaStates) {
            closure.addAll(nfa.epsilonClosure());
        }
        return closure;
    }

    protected static BitSet epsilonClosureIndices(Collection<NFA> nfaStates) {
        BitSet bs = new BitSet();
        for (NFA nfa : nfaStates) {
            bs.or(nfa.epsilonClosureIndices);
        }
        return bs;
    }

    public boolean isTerminal() {
        return isTerminal;
    }

    public int hashCode() {
        return state;
    }

    public boolean equals(Object object) {
        if (object instanceof NFA) {
            NFA other = (NFA) object;
            return other.state == state && other.root == root;
        }
        return false;
    }

    protected void computeEpsilonClosure() {
        if (this.epsilonClosure == null) {
            BitSet seen = new BitSet(root.states.size());
            BitSet pending = new BitSet(root.states.size());

            Set<NFA> closure = new HashSet<>();
            pending.set(this.state);
            seen.set(this.state);

            closure.add(this);
            while (!pending.isEmpty()) {
                int index = pending.nextSetBit(0);
                pending.clear(index);
                epsilonClosureIndices.set(index);

                NFA next = root.states.get(index);
                closure.add(next);
                for (Pair<CharRange, List<NFA>> transition : next.getTransitions()) {
                    if (transition.getLeft().isEmpty()) {
                        for (NFA reachable : transition.getRight()) {
                            if (!seen.get(reachable.state)) {
                                seen.set(reachable.state);
                                pending.set(reachable.state);
                            }
                        }
                    }
                }
            }
            if (closure.isEmpty()) {
                this.epsilonClosure = Collections.emptySet();
            }
            else {
                this.epsilonClosure = closure;
            }
        }
    }

    /**
     * Check a number of invariants. Only applicable to the root node.
     *
     * @throws IllegalStateException if the invariants are broken
     */
    protected void checkRep() {
        for (NFA nfa : states) {
            assert states.get(nfa.state) == nfa;
            assert nfa.epsilonClosure != null : "NFA node " + nfa.state + " has null epsilon closure";
            for (NFA reachable : nfa.epsilonClosure) {
                assert states.contains(reachable) : "Epsilon transition to NFA node " + reachable.state + " not contained in states";
            }
            assert nfa.root != null : "NFA node " + state + " has null root";
        }
        // TODO: assert states.size() == new HashSet<>(states).size() : "nfaStates contains duplicates";
    }
}
