package com.justinblank.strings;

import com.justinblank.strings.RegexAST.*;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class ThompsonRegexInstrBuilder {

    public static List<RegexInstr> createNFA(Node ast) {
        return new ThompsonRegexInstrBuilder().build(ast);
    }

    protected List<RegexInstr> build(Node ast) {
        List<RegexInstr> regex = createPartial(ast, new ArrayList<>());
        regex.add(RegexInstr.match());
        return regex;
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
            instrs.set(splitIndex, RegexInstr.split(splitIndex + 1, postIndex));
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
                instrs.set(switchLocation, RegexInstr.split(switchLocation + 1, finalLocation));
            }
        }
        else if (ast instanceof Alternation) {
            Alternation a = (Alternation) ast;

            int splitIndex = instrs.size();
            instrs.add(null);
            int firstSplitTarget = instrs.size();
            createPartial(a.left, instrs);
            int firstJumpIndex = instrs.size();
            instrs.add(null);
            int secondSplitTarget = instrs.size();
            createPartial(a.right, instrs);
            int secondJumpIndex = instrs.size();
            instrs.add(null);

            int finalJumpTarget = instrs.size();
            RegexInstr jump = RegexInstr.jump(finalJumpTarget);
            instrs.set(firstJumpIndex, jump);
            instrs.set(secondJumpIndex, jump);
            instrs.set(splitIndex, RegexInstr.split(firstSplitTarget, secondSplitTarget));
        }
        else if (ast instanceof CharRangeNode) {
            CharRange range = ((CharRangeNode) ast).range();
            instrs.add(RegexInstr.charRange(range.getStart(), range.getEnd()));
        }
        else {
            throw new IllegalStateException("Unhandled ast node type=" + ast.getClass().getSimpleName());
        }
        return instrs;
    }
}
