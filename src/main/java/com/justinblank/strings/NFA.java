package com.justinblank.strings;

import com.justinblank.strings.RegexAST.Node;
import com.justinblank.strings.Search.SearchMethod;
import com.justinblank.strings.Search.SearchMethodMatcher;
import com.justinblank.strings.Search.SearchMethodUtil;
import com.justinblank.strings.Search.SearchMethods;
import com.justinblank.util.SparseSet;

import java.util.*;

import static com.justinblank.strings.RegexInstr.Opcode.*;

class NFA implements SearchMethod {

    RegexInstr[] regexInstrs;

    protected NFA(RegexInstr[] regexInstrs) {
        this.regexInstrs = regexInstrs;
    }

    public static SearchMethod createNFA(String regex) {
        Node parse = RegexParser.parse(regex);
        var factors = parse.bestFactors();
        if (factors.isComplete()) {
            return SearchMethods.makeSearchMethod(factors.getAll());
        }
        return new NFA(RegexInstrBuilder.createNFA(parse));
    }

    /**
     * This method exists just for the sake of ensuring that we get adequate test coverage of our NFA.
     *
     * @param regex the regex
     * @return an NFA
     */
    static NFA createNFANoAhoCorasick(String regex) {
        Node parse = RegexParser.parse(regex);
        return new NFA(RegexInstrBuilder.createNFA(parse));
    }

    static NFA createReversedNFANoAhoCorasick(String regex) {
        Node reversed = RegexParser.parse(regex).reversed();
        return new NFA(RegexInstrBuilder.createNFA(reversed));
    }

    public boolean matches(String s) {
        MatchResult result = find(s, 0, s.length(), true);
        return result.matched && result.end == s.length();
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
            RegexInstr instr = regexInstrs[stateIndex];
            if (instr.opcode == JUMP) {
                instr = regexInstrs[instr.target1];
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
                if (regexInstrs[target1].opcode == MATCH) {
                    if (origin < lastStart) {
                        lastStart = origin;
                    }
                }
                else {
                    states.add(target1);
                    stateOrigins[target1] = Math.min(origin, stateOrigins[target1]);
                }
                int target2 = instr.target2;
                if (regexInstrs[target2].opcode == MATCH) {
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
        SearchMethodUtil.checkIndices(s, start, end);
        int i = start;
        int lastStart = Integer.MAX_VALUE;
        int lastEnd = -1;
        int size = this.regexInstrs.length;
        SparseSet activeStates = new SparseSet(size);
        activeStates.add(0);
        SparseSet newStates = new SparseSet(size);
        int[] stateOrigins = new int[size];
        Arrays.fill(stateOrigins, Integer.MAX_VALUE);
        stateOrigins[0] = 0;
        int[] newStateOrigins = new int[size];
        Arrays.fill(newStateOrigins, Integer.MAX_VALUE);
        for (; i < end; i++) {
            char c = s.charAt(i);
            // If we have returned to the initial state, during the course of a match, i.e. with a*b matching "aaab", we
            // should not override the match in progress. Otherwise, start over, to search for a match.
            if (i == start || (stateOrigins[0] == Integer.MAX_VALUE && !anchored && lastStart == Integer.MAX_VALUE)) {
                activeStates.add(0);
                stateOrigins[0] = i;
            }
            else if (lastStart != Integer.MAX_VALUE && activeStates.size() == 0) {
                break;
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
                RegexInstr instr = this.regexInstrs[currentState];
                int target1 = instr.target1;
                // The only way we could have a jump here is either
                // 1) the previous iteration left it as the result of a split--but the builder ensures a jump never
                // follows a split
                // 2) it followed a charrange instruction, but that block handles moving to the target of the jump
                assert instr.opcode != JUMP;
                if (instr.opcode == SPLIT) {
                    activeStates.add(target1);
                    RegexInstr target1Instr = this.regexInstrs[target1];
                    if (target1Instr.opcode != MATCH) {
                        if (origin < stateOrigins[target1]) {
                            stateOrigins[target1] = origin;
                            if (activeStates.indexOf(target1) < j) {
                                j = activeStates.indexOf(target1) - 1;
                            }
                        }
                    }
                    int target2 = instr.target2;
                    RegexInstr target2Instr = this.regexInstrs[target2];
                    if (target2Instr.opcode != MATCH) {
                        activeStates.add(target2);
                        if (origin < stateOrigins[target2]) {
                            stateOrigins[target2] = origin;
                            if (activeStates.indexOf(target2) < j) {
                                j = activeStates.indexOf(target2) - 1;
                            }
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
                    if (instr.start <= c && instr.end >= c) {
                        int next = currentState + 1;
                        instr = this.regexInstrs[next];
                        if (instr.opcode != MATCH) {
                            if (instr.opcode == JUMP) {
                                next = instr.target1;
                            }
                            if (this.regexInstrs[next].opcode != MATCH) {
                                newStates.add(next);
                                newStateOrigins[next] = Math.min(newStateOrigins[next], origin);
                            }
                            else {
                                instr = this.regexInstrs[next];
                            }
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

    protected boolean hasAcceptingState(Collection<Integer> indices) {
        for (Integer index : indices) {
            if (this.regexInstrs[index].opcode == MATCH) {
                return true;
            }
        }
        return false;
    }

    // TODO: Optimized version that works on a collection
    protected Set<Integer> epsilonClosure(Integer initial) {
        Set<Integer> closure = new HashSet<>();
        closure.add(initial);
        Queue<Integer> pending = new LinkedList<>();
        pending.add(initial);
        while (!pending.isEmpty()) {
            Integer next = pending.poll();
            RegexInstr instr = this.regexInstrs[next];
            if (instr.opcode == SPLIT) {
                Integer newNext1 = instr.target1;
                if (!closure.contains(newNext1)) {
                    pending.add(newNext1);
                    closure.add(newNext1);
                }
                Integer newNext2 = instr.target2;
                if (!closure.contains(newNext2)) {
                    pending.add(newNext2);
                    closure.add(newNext2);
                }
            }
            else if (instr.opcode == JUMP) {
                Integer newNext = instr.target1;
                if (!closure.contains(newNext)) {
                    pending.add(newNext);
                    closure.add(newNext);
                }
            }
        }
        return closure;
    }

    protected Set<Integer> epsilonClosure(Collection<Integer> nfaStates) {
        Set<Integer> closure = new HashSet<>();
        for (Integer state : nfaStates) {
            closure.addAll(epsilonClosure(state));
        }
        return closure;
    }
}
