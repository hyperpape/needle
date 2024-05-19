package com.justinblank.strings;

import com.justinblank.strings.RegexAST.Node;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

class DFA {

    public static final int MAX_CHAR_FOR_BYTECLASSES = 127;
    private boolean accepting;
    private int stateNumber;
    private DFA root;
    // Only populated on the root
    private List<DFA> states;
    private List<Pair<CharRange, DFA>> transitions = new ArrayList<>();

    static DFA root(boolean accepting) {
        return new DFA(true, accepting, 0);
    }

    private DFA(boolean root, boolean accepting, int stateNumber) {
        if (root) {
            this.root = this;
            this.states = new ArrayList<>();
            this.states.add(this);
        }
        this.accepting = accepting;
        this.stateNumber = stateNumber;
    }

    protected DFA(DFA root, boolean accepting, int stateNumber) {
        if (root == null) {
            throw new IllegalArgumentException("Cannot create DFA with null root");
        }
        this.root = root;
        this.accepting = accepting;
        this.stateNumber = stateNumber;
        this.root.states.add(this);
    }

    public static DFA createDFA(String regex) {
        return createDFA(regex, ConversionMode.BASIC); // TODO: this needs revisited
    }

    static DFA createDFA(String regex, ConversionMode mode) {
        Node node = RegexParser.parse(regex);
        try {
            NFA nfa = new NFA(RegexInstrBuilder.createNFA(node));
            return NFAToDFACompiler.compile(nfa, mode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create dfa from string '" + regex + "'", e);
        }
    }

    protected void addTransition(CharRange charRange, DFA dfa) {
        assert !charRange.isEmpty() : "cannot add an epsilon transition to a DFA";
        for (var existingTransition : transitions) {
            if (existingTransition.getLeft().equals(charRange)) {
                return;
            }
        }
        transitions.add(Pair.of(charRange, dfa));
        // we trust that our character ranges don't overlap
        transitions.sort(Comparator.comparingInt(p -> p.getLeft().getStart()));
    }

    protected List<Pair<CharRange, DFA>> getTransitions() {
        return transitions;
    }

    protected boolean isAccepting() {
        return accepting;
    }

    protected boolean isTerminal() {
        return transitions.isEmpty();
    }

    protected DFA transition(char c) {
        for (Pair<CharRange, DFA> transition : transitions) {
            if (transition.getLeft().inRange(c)) {
                return transition.getRight();
            }
        }
        return null;
    }

    public boolean matches(String s) {
        int length = s.length();
        DFA current = this;
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            current = current.transition(c);
            if (current == null) {
                return false;
            }
        }
        return current.accepting;
    }

    public MatchResult search(String s) {
        // In order to match, we need to know the earliest index we could start from to reach a given state
        int[] stateStarts = initSearchStateArray();
        int[] newStateStarts = null;
        // the starting index of a successful match. May be set multiple times, but can only ever decrease.
        int matchStart = Integer.MAX_VALUE;
        // the final index of a successful match. May be set multiple times, but should only ever increase.
        int matchEnd = Integer.MIN_VALUE;
        if (accepting) {
            matchStart = 0;
            matchEnd = 0;
        }
        for (int i = 0; i < s.length(); i++) {
            if (i == 0) {
                newStateStarts = initSearchStateArray();
            } else {
                clearSearchStateArray(newStateStarts);
            }
            char c = s.charAt(i);
            int earliestCurrentStart = Integer.MAX_VALUE;
            for (int j = 0; j < stateStarts.length; j++) {
                int stateStart = stateStarts[j];
                // We consider states that are live, but ignore those which started later than the best match we've seen
                boolean shouldConsider = stateStart > -1 && stateStart <= matchStart;
                // We also always consider the initial state when we haven't yet seen a match
                shouldConsider |= j == 0 && matchStart == Integer.MAX_VALUE;
                if (shouldConsider) {
                    DFA dfa = states.get(j);
                    DFA found = dfa.transition(c);
                    if (found != null) {
                        int foundStateNumber = found.stateNumber;
                        if (newStateStarts[foundStateNumber] == -1 ||
                                newStateStarts[foundStateNumber] > stateStarts[foundStateNumber]) {
                            if (i == 0) {
                                stateStart = 0;
                            } else if (dfa.stateNumber == 0) {
                                stateStart = i;
                            }
                            newStateStarts[foundStateNumber] = stateStart;
                            earliestCurrentStart = Math.min(earliestCurrentStart, stateStart);
                            if (found.accepting) {
                                int newMatchStart = newStateStarts[foundStateNumber];
                                if (newMatchStart <= matchStart) {
                                    matchStart = newMatchStart;
                                    matchEnd = i + 1;
                                }
                            }
                        }
                    }
                }
            }
            if (earliestCurrentStart > matchStart) {
                return new MatchResult(true, matchStart, matchEnd);
            }
            // Swap arrays, to avoid repeatedly allocating
            int[] tmp = stateStarts;
            stateStarts = newStateStarts;
            newStateStarts = tmp;
        }
        if (matchStart == Integer.MAX_VALUE) {
            return MatchResult.FAILURE;
        } else {
            return new MatchResult(true, matchStart, matchEnd);
        }
    }

    private int[] initSearchStateArray() {
        int[] stateStarts = new int[states.size()];
        clearSearchStateArray(stateStarts);
        return stateStarts;
    }

    private void clearSearchStateArray(int[] stateStarts) {
        Arrays.fill(stateStarts, -1);
    }

    public int statesCount() {
        return states.size();
    }

    public Set<DFA> allStates() {
        return new HashSet<>(states);
    }

    protected Set<DFA> acceptingStates() {
        var accepting = new HashSet<DFA>();
        for (var state : states) {
            if (state.isAccepting()) {
                accepting.add(state);
            }
        }
        return accepting;
    }

    protected boolean hasSelfTransition() {
        for (Pair<CharRange, DFA> transition : transitions) {
            if (transition.getRight() == this) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get a chain of states that must be passed through, starting with a given state
     *
     * @return a non-null, possibly empty list of states that must be passed through in order--does not contain the
     * starting state
     */
    List<DFA> chain() {
        var chain = new LinkedHashSet<DFA>();
        var next = this;
        while (true) {

            if (next.isAccepting()) {
                break;
            }
            else if (next.transitions.size() == 1) {
                 var newNext = next.transitions.get(0).getRight();
                 if (chain.contains(newNext)) {
                     break;
                 }
                 else {
                     if (next != this) {
                         chain.add(newNext);
                     }
                     next = newNext;
                 }
            }
            else {
                Set<DFA> followers = new HashSet<>();
                for (var transition : transitions) {
                    followers.add(transition.getRight());
                }
                if (followers.size() != 1) {
                    break;
                }
                var newNext = followers.iterator().next();
                if (next != this) {
                    chain.add(newNext);
                }
                next = newNext;
            }
        }
        return new ArrayList<>(chain);
    }

    Map<Integer, Offset> calculateOffsets(Factorization factorization) {
        var map = new HashMap<Integer, Offset>();
        var seen = new HashSet<Integer>();
        for (var state : states) {
            if (shouldCalculateOffset(factorization, seen, state)) {
                var offset = state.calculateOffset();
                offset.ifPresent(o -> {
                    seen.addAll(o.passedStates);
                    map.put(state.stateNumber, o);
                });
            }

        }
        return map;
    }

    /**
     * Determine if we'll want to calculate offsets for a dfa state
     *
     * This code is very preliminary--need to find a better way to represent the entire issue of considering offsets.
     *
     * Normally, in a dfa like "abcd", we want to calculate offsets for the root state, but not the state after "a", as
     * it's "covered" by the previous offset (there is no point in checking multiple offsets that "look ahead at the
     * same place").
     *
     * However, in a dfa like "the [Cc]rown" the initial state has an offset of 9 characters, pointing to the 'n'.
     * In spite of that, we want to calculate an offset after the ' ' character, as we'll consume the initial prefix
     * "the ", then consider looking ahead 5 characters for the 'n' character.
     *
     * @param factorization the DFA's factorization
     * @param passed        the set of states that are passed by some other offset
     * @param state         the state in question
     * @return whether to calculate offsets for the state
     */
    private boolean shouldCalculateOffset(Factorization factorization, Set<Integer> passed, DFA state) {
        if (!passed.contains(state.stateNumber)) {
            return true;
        }
        if (factorization != null) {
            return factorization.getSharedPrefix().flatMap(this::after).map((postPrefixState) -> postPrefixState == state).orElse(false);
        }
        return false;
    }

    Optional<Offset> calculateOffset() {
        Set<Integer> passedStates = new HashSet<>();
        var count = -1;
        CharRange charRange = null;
        var next = this;
        // TODO: handle case where offset is resolved to . or similarly permissive character range: checking that is
        //  not useful
        while (true) {
            if (passedStates.contains(next) || next.hasSelfTransition() || next.transitions.size() == 0 ||
                    next.isAccepting()) {
                break;
            }
            if (next.transitions.size() != 1) {
                // TODO: this line is more restrictive than it has to be, e.g. "(ab)|(cd)efg won't pass
                if (!next.allTransitionsLeadToSameState()) {
                    break;
                }
            }
            passedStates.add(next.stateNumber);

            var transition = next.transitions.get(0);
            next = transition.getRight();
            count++;
            charRange = transition.getLeft();
        }

        if (count > 0) {
            return Optional.of(new Offset(count, passedStates, charRange));
        } else {
            return Optional.empty();
        }
    }

    boolean allTransitionsLeadToSameState() {
        for (int i = 0; i < transitions.size() - 1; i++) {
            if (transitions.get(i).getRight() != transitions.get(i + 1).getRight()) {
                return false;
            }
        }
        return true;
    }

    boolean allForwardTransitionsLeadToSameState() {
        var forwardTransitions = getTransitionsLeavingZeroState();
        for (var i = 0; i < forwardTransitions.size() - 1; i++) {
            if (forwardTransitions.get(i).getRight() != forwardTransitions.get(i + 1).getRight()) {
                return false;
            }
        }
        return true;
    }

    protected boolean isAllAscii() {
        for (var dfa : states) {
            for (var transition : dfa.transitions) {
                if ((int) transition.getLeft().getEnd() > 127) {
                    return false;
                }
            }
        }
        return true;
    }

    char maxChar() {
        char maxChar = 0;
        for (var dfa : states) {
            for (var transition : dfa.transitions) {
                var charRange = transition.getLeft();
                if (charRange.getStart() > maxChar) {
                    maxChar = charRange.getStart();
                }
                if (charRange.getEnd() > maxChar) {
                    maxChar = charRange.getEnd();
                }
            }
        }
        return maxChar;
    }

    /**
     * Return the largest character that the DFA treats uniquely.
     *
     * Treating a character uniquely means that when we have a regex like "a." the regex can match characters larger
     * than "a", but it doesn't need byteclasses that distinguish those characters from each other.
     *
     * Recognizing that we can handle all characters larger than a certain size with the same byteclass should allow us
     * to use byteclasses on some regexes that couldn't previously use them. 
     * @return
     */
    char maxDistinguishedChar() {
        char maxChar = 0;
        for (var dfa : states) {
            for (var transition : dfa.transitions) {
                var charRange = transition.getLeft();
                if (charRange.getStart() > maxChar) {
                    maxChar = charRange.getStart();
                }
                if (charRange.treatsAllNonAsciiIdentically()) {
                    continue;
                }
                if (charRange.getEnd() > maxChar) {
                    maxChar = charRange.getEnd();
                }
            }
        }
        return maxChar;
    }

    /**
     * Calculate a byte[129] of byteClasses that are sufficient to distinguish characters in all transitions of this
     * DFA. Assumes that this DFA consists only of ascii characters.
     *
     * 0 is used to represent that the byte is not in any byteClass.
     * @return byteClasses
     */
    ByteClasses byteClasses() {
        List<RangeGroup> rangeGroups = generateRangeGroups();

        byte byteClass = 1;
        byte catchAll = ByteClasses.CATCHALL_INVALID;
        var ranges = new byte[129];
        for (var rangeGroup : rangeGroups) {
            // This only should work because we're only calling this method after checking
            // DFA.maxDistinguishedChar <= 127
            if (rangeGroup.ranges.get(rangeGroup.ranges.size() - 1).getEnd() == '\uFFFF') {
                catchAll = byteClass;
            }
            for (var range : rangeGroup.ranges) {
                for (var c = range.getStart(); c <= Math.min(range.getEnd(), MAX_CHAR_FOR_BYTECLASSES); c++) {
                    ranges[c] = byteClass;
                }
            }
            byteClass++;
        }
        if (catchAll == ByteClasses.CATCHALL_INVALID) {
            catchAll = byteClass;
        }
        return new ByteClasses(ranges, catchAll, byteClass + 1);
    }

    List<RangeGroup> generateRangeGroups() {
        // Sometimes we'll have two disjoint character ranges that can only lead to transitions to the same state, for
        // instance in the regex [A-Za-z]+ing, [A-Z][a-f][h][j-m][o-z] all lead from the state 0-1. By tracking these
        // transitions, we can significantly reduce the number of byteClasses that we produce
        var charRangesToStateTransitions = charRanges();
        var uniqueSets = new HashMap<Set<Pair<Integer, Integer>>, RangeGroup>();
        for (var e : charRangesToStateTransitions.entrySet()) {
            var rangeSet = uniqueSets.computeIfAbsent(e.getValue(), k -> new RangeGroup());
            rangeSet.ranges.add(e.getKey());
        }

        List<RangeGroup> rangeGroups = new ArrayList<>(uniqueSets.values());
        for (var rangeGroup : rangeGroups) {
            Collections.sort(rangeGroup.ranges);
        }
        Collections.sort(rangeGroups);
        return rangeGroups;
    }

    Map<CharRange, Set<Pair<Integer, Integer>>> charRanges() {
        List<CharRange> distinctRanges = getDistinctCharRanges(getSortedTransitions());

        Map<CharRange, Set<Pair<Integer, Integer>>> hashMap = new HashMap<>();
        for (var range : distinctRanges) {
            var set = new HashSet<Pair<Integer, Integer>>();
            hashMap.put(range, set);
            for (var state : states) {
                for (var transition : state.transitions) {
                    if (transition.getLeft().overlaps(range)) {
                        set.add(Pair.of(state.stateNumber, transition.getRight().getStateNumber()));
                    }
                }
            }
        }
        return hashMap;
    }

    /**
     * Create a minimal covering of all character ranges included in DFA with the property that if any two ranges in
     * the DFA disagree on some character, then the covering places those two characters in separate ranges.
     * E.g. [a-z][b-c][g-h] will produce five ranges: [a][b-c][d-f][g-h][i-z]
     * @return a list of non-overlapping character ranges that cover every character the DFA recognizes
     */
    List<CharRange> getDistinctCharRanges(List<CharRange> allTransitions) {

        List<CharRange> derivedRanges = new ArrayList<>();
        char nextStart = '\u0000';
        for (var i = 0; i < allTransitions.size(); i++) {
            var leftRange = allTransitions.get(i);
            nextStart = (char) Math.max(nextStart, leftRange.getStart());
            while (nextStart <= leftRange.getEnd()) {
                var nextEnd = getNextEnd(allTransitions, i, nextStart, leftRange);
                derivedRanges.add(new CharRange(nextStart, nextEnd));
                if (nextEnd == Character.MAX_VALUE) {
                    return derivedRanges;
                }
                else {
                    nextStart = (char) (nextEnd + 1);
                }
            }
        }
        return derivedRanges;
    }

    private char getNextEnd(List<CharRange> allTransitions, int i, char nextStart, CharRange leftRange) {
        var nextEnd = leftRange.getEnd();
        for (var j = i + 1; j < allTransitions.size(); j++) {
            var rightRange = allTransitions.get(j);
            if (rightRange.getEnd() < nextStart) {
                continue;
            }
            if (rightRange.getStart() > nextEnd) {
                return nextEnd;
            }
            if (nextStart >= rightRange.getStart()) {
                nextEnd = (char) Math.min(nextEnd, rightRange.getEnd());
            }
            else {
                nextEnd = (char) (rightRange.getStart() - 1);
            }
        }
        return nextEnd;
    }

    private static char determineEnd(List<CharRange> allTransitions, int i, CharRange startRange) {
        var end = startRange.getEnd();
        for (; i < allTransitions.size(); i++) {
            var otherRange = allTransitions.get(i);
            if (startRange.overlaps(otherRange)) {
                end = (char) Math.min(end, otherRange.getEnd());
            }
            else {
                return end;
            }
        }
        return end;
    }

    List<CharRange> getSortedTransitions() {
        var uniqueRanges = states.stream().flatMap(s -> s.getTransitions().stream()).map(Pair::getLeft).collect(Collectors.toSet());
        var allTransitions = new ArrayList<>(uniqueRanges);
        Collections.sort(allTransitions);
        return allTransitions;
    }

    private char addRange(List<CharRange> derivedRanges, List<CharRange> allTransitions, char highWaterMark, int i) {
        var range = allTransitions.get(i);
        var start = (char) Math.max(highWaterMark, range.getStart());
        var end = range.getEnd();
        for (var j = i; j < allTransitions.size(); j++) {
            if (end < start) {
                return highWaterMark;
            }
            var secondRange = allTransitions.get(j);
            if (secondRange.getStart() > end) {
                break;
            }
            else if (secondRange.getStart() == start) {
                end = (char) Math.min(end, secondRange.getEnd());
            }
        }
        derivedRanges.add(new CharRange(start, end));
        return end;
    }

    protected int charCount() {
        int chars = 0;
        for (Pair<CharRange, DFA> transition : transitions) {
            chars += 1 + (int) transition.getLeft().getEnd() - (int) transition.getLeft().getStart();
        }
        return chars;
    }

    protected int charTotal() {
        int total = 0;
        for (var transition : transitions) {
            total += transition.getLeft().getStart();
        }
        return total;
    }

    public int getStateNumber() {
        return stateNumber;
    }

    @Override
    public String toString() {
        return "DFA{" +
                "accepting=" + accepting +
                ", stateNumber=" + stateNumber +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DFA dfa = (DFA) o;
        return root == dfa.root && stateNumber == dfa.stateNumber;
    }

    @Override
    public int hashCode() {
        return stateNumber;
    }

    /**
     * Check invariants:
     * <ul>
     *     <li>The states reachable from the root should be contained in the states member variable</li>
     *     <li>All states should have distinct stateNumbers</li>
     *     <li>All states should refer to the same root</li>
     * </ul>
     * <p>
     * Only applicable to the root node.
     *
     * @throws IllegalStateException if the invariants are broken
     */
    protected boolean checkRep() {
        assert allStatesReachable() : "Some state was unreachable";
        assert allStates().containsAll(states);
        assert new HashSet<>(states).containsAll(allStates());
        assert states.stream().allMatch(dfa -> dfa.root == this);
        assert states.stream().map(DFA::getStateNumber).count() == states.size();
        assert states.contains(this) : "root not included in states";
        assert states.stream().anyMatch(DFA::isAccepting) : "no accepting state found";
        assert transitions.stream().map(Pair::getRight).allMatch(dfa -> states.contains(dfa));
        assert states.stream().filter(dfa -> dfa.getTransitions().isEmpty()).count() < 2;
        return true;
    }

    private boolean allStatesReachable() {
        for (DFA dfa : states) {
            for (Pair<CharRange, DFA> transition : dfa.transitions) {
                DFA target = transition.getRight();
                if (target.stateNumber > states.size() || states.get(target.stateNumber) != target) {
                    return false;
                }
            }
        }
        return true;
    }

    protected boolean isRoot() {
        return this == root;
    }

    public Optional<DFA> after(String prefix) {
        var dfa = this;
        for (int i = 0; i < prefix.length(); i++) {
            dfa = dfa.transition(prefix.charAt(i));
            if (dfa == null) {
                return Optional.empty();
            }
        }
        return Optional.of(dfa);
    }

    /**
     * Determine if this DFA's forward transitions are of a "well behaved" set, where we can easily check whether the
     * transition's conditions are met. Examples are:
     * - a single contiguous range of characters
     * - a single case insensitive character, e.g. [Ss].
     *
     * @return whether the forward transition can be handled by one of a set of simple predicates.
     */
    public boolean forwardTransitionIsPredicate(CompilationPolicy compilationPolicy) {
        var effectiveTransitions = getTransitionsLeavingZeroState();
        if (effectiveTransitions.size() == 1) {
            var range = effectiveTransitions.get(0).getLeft();
            var count = 1 + (range.getEnd() - range.getStart());
            return count <= compilationPolicy.predicateRangeSizeCutoff;
        } else if (effectiveTransitions.size() == 2) {
            var firstRange = effectiveTransitions.get(0).getLeft();
            var secondRange = effectiveTransitions.get(1).getLeft();
            if (firstRange.isSingleCharRange() && secondRange.isSingleCharRange()) {
                return Math.abs(firstRange.getStart() - secondRange.getStart()) == 32;
            }
        }
        return false;
    }

    List<Pair<CharRange, DFA>> getTransitionsLeavingZeroState() {
        var effectiveTransitions = new ArrayList<Pair<CharRange, DFA>>();
        for (var transition : transitions) {
            if (transition.getRight().stateNumber != 0) {
                effectiveTransitions.add(transition);
            }
        }
        return effectiveTransitions;
    }

    public DFA forwardFollowingState() {
        if (!allForwardTransitionsLeadToSameState()) {
            throw new IllegalArgumentException("Not allowed to call this method on a dfa that can lead to multiple states");
        }
        return getTransitionsLeavingZeroState().get(0).getRight();
    }

    void pruneDeadStates() {
        var liveStates = findLiveStates();
        for (var state : states) {
            var liveTransitions = new ArrayList<Pair<CharRange, DFA>>();
            for (var transition : state.transitions) {
                if (liveStates.contains(transition.getRight().stateNumber)) {
                    liveTransitions.add(transition);
                }
            }
            state.transitions = liveTransitions;
        }
        var newStates = new ArrayList<DFA>();
        for (var state : states) {
            if (liveStates.contains(state.stateNumber)) {
                newStates.add(state);
            }
        }
        states = newStates;
        for (var i = 0; i < this.states.size(); i++) {
            this.states.get(i).stateNumber = i;
        }
    }

    Set<Integer> findLiveStates() {
        Set<Integer> liveStates = new HashSet<>();
        liveStates.add(root.stateNumber);
        for (var state : states) {
            if (state.isAccepting()) {
                liveStates.add(state.stateNumber);
            }
        }
        boolean changed = true;
        // TODO: O(n^2)
        while (changed) {
            changed = false;
            for (var state : states) {
                if (!liveStates.contains(state.stateNumber)) {
                    for (var transition : state.transitions) {
                        if (liveStates.contains(transition.getRight().stateNumber)) {
                            changed = true;
                            liveStates.add(state.stateNumber);
                        }
                    }
                }
            }
        }
        return liveStates;
    }
}
