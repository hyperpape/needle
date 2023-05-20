package com.justinblank.strings;

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
    final int target1;
    final int target2;

    private static final RegexInstr MATCH = new RegexInstr(Opcode.MATCH, 'a', 'a', -1, -1);

    public RegexInstr(Opcode opcode, char start, char end, int target1, int target2) {
        this.opcode = opcode;
        this.start = start;
        this.end = end;
        this.target1 = target1;
        this.target2 = target2;
    }

    static RegexInstr jump(int target) {
        return new RegexInstr(Opcode.JUMP, 'a', 'a', target, -1);
    }

    static RegexInstr split(int target1, int target2) {
        return new RegexInstr(Opcode.SPLIT, 'a', 'a', target1, target2);
    }

    static RegexInstr charRange(char c1, char c2) {
        return new RegexInstr(Opcode.CHAR_RANGE, c1, c2, -1, -1);
    }

    static RegexInstr match() {
        return MATCH;
    }

    public String toString() {
        if (opcode.equals(Opcode.MATCH)) {
            return "Match";
        }
        else if (opcode.equals(Opcode.JUMP)) {
            return "Jump: " + target1;
        }
        else if (opcode.equals(Opcode.SPLIT)) {
            return "Split: " + target1 + "," + target2;
        }
        else {
            return "Char: " + start + ", "+ end;
        }
    }
}

