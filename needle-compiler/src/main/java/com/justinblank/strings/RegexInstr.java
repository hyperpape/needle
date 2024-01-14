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

    private static final int[] EMPTY = new int[0];

    private static final RegexInstr MATCH = new RegexInstr(Opcode.MATCH, 'a', 'a', -1, EMPTY);

    RegexInstr(Opcode opcode, char start, char end, int jumpTarget, int[] splitTargets) {
        this.opcode = opcode;
        this.start = start;
        this.end = end;
        this.jumpTarget = jumpTarget;
        this.splitTargets = splitTargets;
    }

    static RegexInstr jump(int target) {
        return new RegexInstr(Opcode.JUMP, 'a', 'a', target, EMPTY);
    }

    static RegexInstr split(int[] splitTargets) {
        for (var i = 0; i < splitTargets.length; i++) {
            if (splitTargets[i] < 0) {
                throw new IllegalArgumentException("Target" + i + " cannot be < 0, target=" + splitTargets[i]);
            }
        }
        return new RegexInstr(Opcode.SPLIT, 'a', 'a', -1, splitTargets);
    }

    static RegexInstr charRange(char c1, char c2) {
        return new RegexInstr(Opcode.CHAR_RANGE, c1, c2, -1, EMPTY);
    }

    static RegexInstr match() {
        return MATCH;
    }

    public String toString() {
        if (opcode.equals(Opcode.MATCH)) {
            return "Match";
        }
        else if (opcode.equals(Opcode.JUMP)) {
            return "Jump: " + jumpTarget;
        }
        else if (opcode.equals(Opcode.SPLIT)) {
            return "Split: " + Arrays.toString(splitTargets);
        }
        else {
            return "Char: " + start + ", "+ end;
        }
    }
}

