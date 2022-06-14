package com.justinblank.strings;

import com.justinblank.classcompiler.Block;
import com.justinblank.classcompiler.Operation;
import com.justinblank.classcompiler.RefSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.stream.Collectors;

public class CheckCharsOperation extends Operation {

    private Block successTarget;
    List<Pair<CharRange, DFA>> transitions;

    CheckCharsOperation(Inst inst, int count, List<Pair<CharRange, Integer>> transitions, Block blockTarget, RefSpec spec, List<Integer> ints) {
        super(inst, count, blockTarget, spec, ints);
    }

    void setSuccessTarget(Block successTarget) {
        this.successTarget = successTarget;
    }

    void setTransitions(List<Pair<CharRange, DFA>> transitions) {
        this.transitions = transitions;
    }

    Block getSuccessTarget() {
        return successTarget;
    }

    static Operation checkChars(DFA dfa, Block failBlock, Block successTarget) {
        var transitions = dfa.getTransitions().
                stream().
                map(t -> Pair.of(t.getLeft(), t.getRight().getStateNumber())).
                collect(Collectors.toList());
        var checkChars = new CheckCharsOperation(Inst.CHECK_CHARS, -1, transitions, failBlock,  null, null);
        checkChars.setSuccessTarget(successTarget);
        checkChars.setTransitions(dfa.getTransitions());
        return checkChars;
    }


}
