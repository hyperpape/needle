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

    protected NFA(NFA nfa) {
        this.regexInstrs = nfa.regexInstrs;
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
                instr = regexInstrs[instr.jumpTarget];
            }
            if (instr.opcode == MATCH) {
                int successOrigin = stateOrigins[stateIndex];
                if (successOrigin <= lastStart) {
                    lastStart = successOrigin;
                }
            }
            else if (instr.opcode == SPLIT) {
                int origin = stateOrigins[stateIndex];
                for (var target : instr.splitTargets) {
                    if (regexInstrs[target].opcode == MATCH) {
                        if (origin < lastStart) {
                            lastStart = origin;
                        }
                    }
                    else {
                        states.add(target);
                        stateOrigins[target] = Math.min(origin, stateOrigins[target]);
                    }
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

    public MatchResult find(String s, int start, int end, boolean anchored) {
        SearchMethodUtil.checkIndices(s, start, end);
        int i = start;
        int lastStart = Integer.MAX_VALUE;
        int lastEnd = Integer.MIN_VALUE;
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
                // The only way we could have a jump here is either
                // 1) the previous iteration left it as the result of a split--but the builder ensures a jump never
                // follows a split
                // 2) it followed a charrange instruction, but that block handles moving to the target of the jump
                assert instr.opcode != JUMP;
                if (instr.opcode == SPLIT) {
                    RegexInstr matchInstr = null;
                    for (var target : instr.splitTargets) {
                        activeStates.add(target);
                        RegexInstr targetInstr = this.regexInstrs[target];
                        if (targetInstr.opcode != MATCH) {
                            if (origin < stateOrigins[target]) {
                                stateOrigins[target] = origin;
                                if (activeStates.indexOf(target) < j) {
                                    j = activeStates.indexOf(target) - 1;
                                }
                            }
                        }
                        else if (matchInstr == null) {
                            matchInstr = targetInstr;
                        }
                    }
                    if (matchInstr != null) {
                        instr = matchInstr;
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
                                next = instr.jumpTarget;
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
                    if (instr.opcode == MATCH) {
                        lastStart = origin;
                        lastEnd = i;
                    }
                }
                else if (instr.opcode == MATCH) {
                    if (origin <= lastStart) {
                        lastStart = origin;
                        if (lastEnd < i) {
                            lastEnd = i - 1;
                        }
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
        if (lastEnd > Integer.MIN_VALUE) {
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

    protected boolean isAcceptingState(Integer state) {
        return this.regexInstrs[state].opcode == MATCH;
    }

    // TODO: Optimized version that works on a collection
    protected Set<Integer> epsilonClosure(Integer initial) {
        Set<Integer> seen = new HashSet<>();
        Set<Integer> closure = new HashSet<>();
        Queue<Integer> pending = new LinkedList<>();
        pending.add(initial);
        while (!pending.isEmpty()) {
            Integer next = pending.poll();
            seen.add(next);
            RegexInstr instr = this.regexInstrs[next];
            if (instr.opcode == SPLIT) {
                for (var target : instr.splitTargets) {
                    if (!seen.contains(target)) {
                        pending.add(target);
                    }
                }
            }
            else if (instr.opcode == JUMP) {
                Integer newNext = instr.jumpTarget;
                if (!seen.contains(newNext)) {
                    pending.add(newNext);
                }
            }
            else {
                closure.add(next);
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

    public NFA selfTransitioning() {
        var list = new ArrayList<RegexInstr>();
        for (var instr : regexInstrs) {
            list.add(instr);
        }

        return new NFA(list.toArray(new RegexInstr[0]));
    }
}
