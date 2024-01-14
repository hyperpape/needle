package com.justinblank.strings;

import com.justinblank.strings.RegexAST.*;

import java.util.*;

import static com.justinblank.strings.RegexInstr.Opcode.JUMP;
import static com.justinblank.strings.RegexInstr.Opcode.SPLIT;

class RegexInstrBuilder {

    public static RegexInstr[] createNFA(Node ast) {
        return new RegexInstrBuilder().build(ast);
    }

    protected RegexInstr[] build(Node ast) {
        List<RegexInstr> regex = createPartial(ast, new ArrayList<>());
        regex.add(RegexInstr.match());
        resolveJumps(regex);
        assert checkRep(regex);
        return regex.toArray(new RegexInstr[0]);
    }
    
    private void resolveJumps(List<RegexInstr> regex) {
        for (int i = 0; i < regex.size(); i++) {
            RegexInstr instr = regex.get(i);
            if (instr.opcode == JUMP) {
                int resolvedTarget = getResolvedJump(regex, instr.jumpTarget);
                if (resolvedTarget != -1) {
                    regex.set(i, RegexInstr.jump(resolvedTarget));
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
                regex.set(i, RegexInstr.split(resolved));
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

    protected List<RegexInstr> createPartial(Node ast, List<RegexInstr> instrs) {
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
            instrs.add(RegexInstr.jump(splitIndex));
            int postIndex = instrs.size();
            instrs.set(splitIndex, RegexInstr.split(new int[] {splitIndex + 1, postIndex}));
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
                instrs.set(switchLocation, RegexInstr.split(new int[] { switchLocation + 1, finalLocation}));
            }
        }
        else if (ast instanceof Union) {
            Union a = (Union) ast;

            int splitIndex = instrs.size();
            if (!(a.left instanceof Union)) {
                instrs.add(null);
            }
            int firstSplitTarget = instrs.size();
            createPartial(a.left, instrs);
            int firstJumpIndex = instrs.size();
            instrs.add(null);
            int secondSplitTarget = instrs.size();
            createPartial(a.right, instrs);

            int finalJumpTarget = instrs.size();
            RegexInstr jump = RegexInstr.jump(finalJumpTarget);
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
            instrs.set(splitIndex, RegexInstr.split(splitTargetsArray));
        }
        else if (ast instanceof CharRangeNode) {
            CharRange range = ((CharRangeNode) ast).range();
            instrs.add(RegexInstr.charRange(range.getStart(), range.getEnd()));
        }
        else if (ast instanceof LiteralNode) {
            String s = ((LiteralNode) ast).getLiteral();
            for (int i = 0; i < s.length(); i++) {
                instrs.add(RegexInstr.charRange(s.charAt(i), s.charAt(i)));
            }
        }
        else {
            throw new IllegalStateException("Unhandled ast node type=" + ast.getClass().getSimpleName());
        }
        return instrs;
    }
}
