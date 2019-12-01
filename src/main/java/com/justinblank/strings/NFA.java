package com.justinblank.strings;

import com.justinblank.strings.RegexAST.Node;
import com.justinblank.strings.Search.SearchMethod;
import com.justinblank.strings.Search.SearchMethodMatcher;
import com.justinblank.strings.Search.SearchMethods;
import com.justinblank.util.SparseSet;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

import static com.justinblank.strings.RegexInstr.Opcode.*;

public class NFA implements SearchMethod {

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

    public static SearchMethod createNFA(String regex) {
        Node parse = RegexParser.parse(regex);
        if (Node.isAhoCorasickPattern(parse)) {
            List<String> strings = new ArrayList<>();
            Node.extractLiterals(parse, strings);
            return SearchMethods.makeSearchMethod(strings);
        }
        return ThompsonNFABuilder.createNFA(parse);
    }

    protected void addTransitions(CharRange charRange, List<NFA> nfas) {
        for (NFA nfa : nfas) {
            nfa.root = this.root;
        }
        if (isTerminal) {
            for (NFA nfa : nfas) {
                if (nfa != this) {
                    isTerminal = false;
                    break;
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
                    // intentional fallthrough so that we can continue evaluating the target of the jump
                }
                if (opcode == SPLIT) {
                    currentStates.add(regexInstr.target1);
                    currentStates.add(regexInstr.target2);
                    continue;
                }
                if (opcode != MATCH && c >= regexInstr.start && c <= regexInstr.end) {
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

    @Override
    public Matcher matcher(String s) {
        return new SearchMethodMatcher(this, s);
    }

    @Override
    public boolean containedIn(String s) {
        return find(s).matched;
    }

    private MatchResult matchResult(SparseSet states, int[] stateOrigins, int currentIndex) {
        int lastStart = Integer.MAX_VALUE;
        for (int i = 0; i < states.size(); i++) {
            int stateIndex = states.getByIndex(i);
            RegexInstr instr = regexInstrs.get(stateIndex);
            if (instr.opcode == JUMP) {
                instr = regexInstrs.get(instr.target1);
            }
            if (instr.opcode == MATCH) {
                int successOrigin = stateOrigins[stateIndex];
                if (successOrigin <= lastStart) {
                    lastStart = successOrigin;
                }
            }
            else if (instr.opcode == SPLIT) {
                int origin = stateOrigins[stateIndex];
                int target1 = instr.target1;
                if (regexInstrs.get(target1).opcode == MATCH) {
                    if (origin < lastStart) {
                        lastStart = origin;
                    }
                }
                else {
                    states.add(target1);
                    stateOrigins[target1] = Math.min(origin, stateOrigins[target1]);
                }
                int target2 = instr.target2;
                if (regexInstrs.get(target2).opcode == MATCH) {
                    if (origin < lastStart) {
                        lastStart = origin;
                    }
                }
                else {
                    states.add(target2);
                    stateOrigins[target2] = Math.min(origin, stateOrigins[target2]);
                }
            }
        }
        if (lastStart < Integer.MAX_VALUE) {
            return MatchResult.success(lastStart, currentIndex);
        }
        return MatchResult.failure();
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

    @Override
    public int findIndex(String s) {
        MatchResult result = find(s);
        return result.start;
    }

    @Override
    public MatchResult find(String s) {
        return find(s, 0, s.length(), false);
    }

    @Override
    public MatchResult find(String s, int start, int end) {
        return find(s, start, end, false);
    }

    public MatchResult find(String s, int start, int end, boolean anchored) {
        int length = s.length();
        if (start > length) {
            throw new IndexOutOfBoundsException("starting index " + start + " is out of bounds");
        }
        if (end > length) {
            throw new IndexOutOfBoundsException("ending index " + end + " is out of bounds");
        }
        int i = start;
        int lastStart = Integer.MAX_VALUE;
        int lastEnd = -1;
        int size = this.regexInstrs.size();
        SparseSet activeStates = new SparseSet(size);
        SparseSet newStates = new SparseSet(size);
        int[] stateOrigins = new int[size];
        Arrays.fill(stateOrigins, Integer.MAX_VALUE);
        int[] newStateOrigins = new int[size];
        Arrays.fill(newStateOrigins, Integer.MAX_VALUE);
        for (; i < end; i++) {
            char c = s.charAt(i);
            // If we have returned to the initial state, during the course of a match, i.e. with a*b matching "aaab", we
            // should not override the match in progress. Otherwise, start over, to search for
            if (stateOrigins[0] == Integer.MAX_VALUE && !anchored || i == start) {
                activeStates.add(0);
                stateOrigins[0] = i;
            }
            for (int j = 0; j < activeStates.size(); j++) {
                int currentState = activeStates.getByIndex(j);
                int origin = stateOrigins[currentState];
                if (anchored && origin > start) {
                    continue;
                }
                if (origin > lastStart) {
                    continue;
                }
                RegexInstr instr = this.regexInstrs.get(currentState);
                int target1 = instr.target1;
                if (instr.opcode == JUMP) {
                    instr = this.regexInstrs.get(instr.target1);

                    activeStates.add(target1);
                    if (origin < stateOrigins[target1]) {
                        stateOrigins[target1] = origin;
                        j = Math.min(j, activeStates.indexOf(target1));
                    }
                    currentState = target1;
                }
                if (instr.opcode == SPLIT) {
                    activeStates.add(target1);
                    RegexInstr target1Instr = this.regexInstrs.get(target1);
                    if (target1Instr.opcode != MATCH) {
                        if (origin < stateOrigins[target1]) {
                            stateOrigins[target1] = origin;
                            j = Math.min(j, activeStates.indexOf(target1));
                        }
                    }
                    int target2 = instr.target2;
                    RegexInstr target2Instr = this.regexInstrs.get(target2);
                    if (target2Instr.opcode != MATCH) {
                        activeStates.add(target2);
                        if (origin < stateOrigins[target2]) {
                            stateOrigins[target2] = origin;
                            j = Math.min(j, activeStates.indexOf(target2));
                        }
                    }
                    if (target1Instr.opcode == MATCH) {
                        instr = target1Instr;
                    }
                    else if (target2Instr.opcode == MATCH) {
                        instr = target2Instr;
                    }
                    else {
                        continue;
                    }
                }
                if (instr.opcode == CHAR_RANGE) {
                    // 25 mg every 6 hours
                    if (instr.start <= c && instr.end >= c) {
                        int next = currentState + 1;
                        instr = this.regexInstrs.get(next);
                        if (instr.opcode != MATCH) {
                            if (instr.opcode == JUMP) {
                                next = instr.target1;
                            }
                            newStates.add(next);
                            newStateOrigins[next] = Math.min(newStateOrigins[next], origin);
                        }
                    }
                }
                if (instr.opcode == MATCH) {
                    if (origin <= lastStart) {
                        lastStart = origin;
                        lastEnd = i;
                    }
                }
            }

            SparseSet tempStates = activeStates;
            activeStates = newStates;
            newStates = tempStates;
            newStates.clear();
            int[] tempOrigins = stateOrigins;
            stateOrigins = newStateOrigins;
            newStateOrigins = tempOrigins;
            Arrays.fill(newStateOrigins, Integer.MAX_VALUE);
        }
        // TODO: rewrite for clarity
        MatchResult result = null;
        if (lastEnd > -1) {
            result = MatchResult.success(lastStart, lastEnd + 1);
        }
        MatchResult result2 = matchResult(activeStates, stateOrigins, i);
        if (result == null) {
            return result2;
        }
        if (result.compareTo(result2) > 0) {
            return result;
        }
        return result2;
    }

    public MatchResult search(String s) {
        return find(s);
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
