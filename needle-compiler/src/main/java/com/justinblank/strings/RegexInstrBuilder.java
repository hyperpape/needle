package com.justinblank.strings;

import com.justinblank.strings.RegexAST.*;

import java.util.*;

import static com.justinblank.strings.RegexInstr.Opcode.JUMP;
import static com.justinblank.strings.RegexInstr.Opcode.SPLIT;

class RegexInstrBuilder {

    static final int STARTING_PRIORITY = 1;
    private int maxPriority = STARTING_PRIORITY;

    public static RegexInstr[] createNFA(Node ast) {
        return new RegexInstrBuilder().build(ast);
    }

    protected RegexInstr[] build(Node ast) {
        List<RegexInstr> instructions = new ArrayList<>();
        createPartial(ast, instructions);
        instructions.add(RegexInstr.match(priorityForMatch(instructions)));
        resolveJumps(instructions);
        assert checkRep(instructions);
        return instructions.toArray(new RegexInstr[0]);
    }


    private int priorityForMatch(List<RegexInstr> instructions) {
        int matchIndex = instructions.size();
        int priority = Integer.MAX_VALUE;
        for (var instr : instructions) {
            if (instr.opcode == RegexInstr.Opcode.JUMP) {
                if (instr.jumpTarget == matchIndex) {
                    priority = Math.min(priority, instr.priority);
                }
            }
            else if (instr.opcode == RegexInstr.Opcode.SPLIT) {
                for (var splitTarget : instr.splitTargets) {
                    if (splitTarget == matchIndex) {
                        priority = Math.min(priority, instr.priority);
                        break;
                    }
                }
            }
        }
        return priority;
    }

    private void resolveJumps(List<RegexInstr> regex) {
        for (int i = 0; i < regex.size(); i++) {
            RegexInstr instr = regex.get(i);
            if (instr.opcode == JUMP) {
                int resolvedTarget = getResolvedJump(regex, instr.jumpTarget);
                // TODO: document...I have no recollection why this would happen
                if (resolvedTarget != -1) {
                    regex.set(i, RegexInstr.jump(resolvedTarget, instr.priority));
                }
            }
            else if (instr.opcode == SPLIT) {
                int[] resolved = new int[instr.splitTargets.length];
                for (var j = 0; j < instr.splitTargets.length; j++) {
                    int resolvedTarget = getResolvedJump(regex, instr.splitTargets[j]);
                    if (resolvedTarget == -1) {
                        resolvedTarget = instr.splitTargets[j];
                    }
                    resolved[j] = resolvedTarget;
                }
                regex.set(i, RegexInstr.split(resolved, instr.priority));
            }
        }
    }

    private int getResolvedJump(List<RegexInstr> regex, int jump) {
        RegexInstr target = regex.get(jump);
        int resolvedTarget = -1;
        while (target.opcode == JUMP) {
            resolvedTarget = target.jumpTarget;
            target = regex.get(resolvedTarget);
        }
        return resolvedTarget;
    }

    private boolean checkRep(List<RegexInstr> regex) {
        for (var instr : regex) {
            if (instr.opcode == JUMP) {
                assert regex.get(instr.jumpTarget).opcode != JUMP;
            }
            else if (instr.opcode == SPLIT) {
                for (var i = 0; i < instr.splitTargets.length; i++) {
                    assert regex.get(instr.splitTargets[i]).opcode != JUMP;
                }
            }
        }
        return true;
    }

    protected void createPartial(Node ast, List<RegexInstr> instrs) {
        if (ast instanceof Concatenation) {
            Concatenation c = (Concatenation) ast;
            createPartial(c.head, instrs);
            createPartial(c.tail, instrs);
        }
        else if (ast instanceof Repetition) {
            Repetition r = (Repetition) ast;
            int splitIndex = instrs.size();
            instrs.add(null);
            createPartial(r.node, instrs);
            instrs.add(RegexInstr.jump(splitIndex, maxPriority));
            maxPriority++;
            int postIndex = instrs.size();
            instrs.set(splitIndex, RegexInstr.split(new int[] {splitIndex + 1, postIndex}, maxPriority));
        }
        else if (ast instanceof CountedRepetition) {
            CountedRepetition countedRepetition = (CountedRepetition) ast;
            int repetition = 0;
            for (; repetition < countedRepetition.min; repetition++) {
                createPartial(countedRepetition.node, instrs);
            }
            List<Integer> switchLocations = new ArrayList<>();
            for (; repetition < countedRepetition.max; repetition++) {
                switchLocations.add(instrs.size());
                instrs.add(null);
                createPartial(countedRepetition.node, instrs);
            }
            int finalLocation = instrs.size();
            for (Integer switchLocation : switchLocations) {
                RegexInstr newInstr = RegexInstr.split(new int[]{switchLocation + 1, finalLocation}, maxPriority);
                instrs.set(switchLocation, newInstr);
            }
        }
        else if (ast instanceof Union) {
            Union a = (Union) ast;

            int splitIndex = instrs.size();
            if (!(a.left instanceof Union)) {
                instrs.add(null);
            }
            int firstSplitTarget = instrs.size();

            int firstAlternativePriority = maxPriority;
            createPartial(a.left, instrs);
            maxPriority++;
            int firstJumpIndex = instrs.size();
            instrs.add(null);
            int secondSplitTarget = instrs.size();
            createPartial(a.right, instrs);
            maxPriority++;

            int finalJumpTarget = instrs.size();
            // TODO: not sure what this should be
            RegexInstr jump = RegexInstr.jump(finalJumpTarget, firstAlternativePriority);
            instrs.set(firstJumpIndex, jump);

            List<Integer> splitTargets = new ArrayList<>();
            if (instrs.get(firstSplitTarget).opcode == SPLIT) {
                for (var i : instrs.get(firstSplitTarget).splitTargets) {
                    splitTargets.add(i);
                }
            }
            else {
                splitTargets.add(firstSplitTarget);
            }
            if (secondSplitTarget < instrs.size() && instrs.get(secondSplitTarget).opcode == SPLIT) {
                for (var i : instrs.get(secondSplitTarget).splitTargets) {
                    splitTargets.add(i);
                }
            }
            else {
                splitTargets.add(secondSplitTarget);
            }
            int[] splitTargetsArray = new int[splitTargets.size()];
            for (var i = 0; i < splitTargets.size(); i++) {
                splitTargetsArray[i] = splitTargets.get(i);
            }
            RegexInstr split = RegexInstr.split(splitTargetsArray, firstAlternativePriority);
            instrs.set(splitIndex, split);
        }
        else if (ast instanceof CharRangeNode) {
            CharRange range = ((CharRangeNode) ast).range();
            RegexInstr newInstr = RegexInstr.charRange(range.getStart(), range.getEnd(), maxPriority);
            instrs.add(newInstr);
        }
        else if (ast instanceof LiteralNode) {
            String s = ((LiteralNode) ast).getLiteral();
            for (int i = 0; i < s.length(); i++) {
                RegexInstr newInstr = RegexInstr.charRange(s.charAt(i), s.charAt(i), maxPriority);
                instrs.add(newInstr);
            }
        }
        else {
            throw new IllegalStateException("Unhandled ast node type=" + ast.getClass().getSimpleName());
        }
    }

    int incrementPriority(int priority) {
        priority++;
        if (priority > maxPriority) {
            maxPriority = priority;
        }
        return priority;
    }
}
