package com.justinblank.strings;

public class RegexInstr {

    enum Opcode {
        Char,
        CharRange,
        Jump,
        Split,
        Match;
    }

    final Opcode opcode;
    final char start;
    final char end;
    final int target1;
    final int target2;

    public RegexInstr(Opcode opcode, char start, char end, int target1, int target2) {
        this.opcode = opcode;
        this.start = start;
        this.end = end;
        this.target1 = target1;
        this.target2 = target2;
    }

    static RegexInstr jump(int target) {
        return new RegexInstr(Opcode.Jump, 'a', 'a', target, -1);
    }

    static RegexInstr split(int target1, int target2) {
        return new RegexInstr(Opcode.Split, 'a', 'a', target1, target2);
    }

    static RegexInstr charRange(char c1, char c2) {
        return new RegexInstr(Opcode.CharRange, c1, c2, -1, -1);
    }

    static RegexInstr match() {
        return new RegexInstr(Opcode.Match, 'a', 'a', -1, -1);
    }

    public String toString() {
        if (opcode.equals(Opcode.Match)) {
            return "Match";
        }
        else if (opcode.equals(Opcode.Jump)) {
            return "Jump: " + target1;
        }
        else if (opcode.equals(Opcode.Split)) {
            return "Split: " + target1 + "," + target2;
        }
        else {
            return "Char: " + start + ", "+ end;
        }
    }
//    static class Char extends RegexInstr {
//        private final char c;
//
//        public Char(char c) {
//            this.c = c;
//        }
//    }
//
//    static class Match extends RegexInstr {
//    }
//
//    static class Jump extends RegexInstr {
//        private final int target;
//
//        public Jump(int i) {
//            target = i;
//        }
//    }
//
//    static class Split extends RegexInstr {
//        private final int target1;
//        private final int target2;
//
//        public Split(int target1, int target2) {
//            this.target1 = target1;
//            this.target2 = target2;
//        }
//    }

}

