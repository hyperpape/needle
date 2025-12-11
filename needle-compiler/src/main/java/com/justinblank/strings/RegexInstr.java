package com.justinblank.strings;

import java.util.Arrays;

class RegexInstr {

    enum Opcode {
        CHAR_RANGE,
        JUMP,
        SPLIT,
        MATCH;
    }

    final Opcode opcode;
    final char start;
    final char end;
    final int jumpTarget;
    final int[] splitTargets;
    int priority = 0;

    private static final int[] EMPTY = new int[0];

    RegexInstr(Opcode opcode, char start, char end, int jumpTarget, int[] splitTargets) {
        this.opcode = opcode;
        this.start = start;
        this.end = end;
        this.jumpTarget = jumpTarget;
        this.splitTargets = splitTargets;
    }

    static RegexInstr jump(int target, int priority) {
        var jump =  new RegexInstr(Opcode.JUMP, 'a', 'a', target, EMPTY);
        jump.updatePriority(priority);
        return jump;
    }

    static RegexInstr split(int[] splitTargets, int priority) {
        for (var i = 0; i < splitTargets.length; i++) {
            if (splitTargets[i] < 0) {
                throw new IllegalArgumentException("Target" + i + " cannot be < 0, target=" + splitTargets[i]);
            }
        }
        var split = new RegexInstr(Opcode.SPLIT, 'a', 'a', -1, splitTargets);
        split.updatePriority(priority);
        return split;
    }

    static RegexInstr charRange(char c1, char c2, int priority) {
        var range = new RegexInstr(Opcode.CHAR_RANGE, c1, c2, -1, EMPTY);
        range.updatePriority(priority);
        return range;
    }

    static RegexInstr match(int priority) {
        var instr = new RegexInstr(Opcode.MATCH, 'a', 'a', -1, EMPTY);
        instr.updatePriority(priority);
        return instr;
    }

    private void updatePriority(int priority) {
        if (priority > this.priority) {
            this.priority = priority;
        }
    }

    public String toString() {
        if (opcode.equals(Opcode.MATCH)) {
            return "Match, priority=" + priority;
        }
        else if (opcode.equals(Opcode.JUMP)) {
            return "Jump: " + jumpTarget + ", priority=" + priority;
        }
        else if (opcode.equals(Opcode.SPLIT)) {
            return "Split: " + Arrays.toString(splitTargets) + ", priority=" + priority;
        }
        else {
            return "Char: " + start + ", "+ end + ", priority=" + priority;
        }
    }
}

