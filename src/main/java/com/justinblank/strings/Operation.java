package com.justinblank.strings;

import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.stream.Collectors;

class Operation {

    Inst inst;
    int count;
    List<Pair<CharRange, Integer>> transitions;
    Block target;
    List<Block> blockTargets;
    RefSpec spec;
    List<Integer> ints;

    public static Operation mkReadChar() {
        return new Operation(Inst.READ_CHAR, -1, null, null, null, null);
    }

    @Override
    public String toString() {
        return "Operation{" +
                "tag=" + inst +
                ", count=" + count +
                ", transitions=" + transitions +
                ", target='" + target + '\'' +
                '}';
    }

    Operation(Operation.Inst inst, int count, List<Pair<CharRange, Integer>> transitions, Block blockTarget, RefSpec spec, List<Integer> ints) {
        this.inst = inst;
        this.count = count;
        this.transitions = transitions;
        this.target = blockTarget;
        this.spec = spec;
        this.ints = ints;
        this.blockTargets = null;
    }

    // We can derive the max from the min, but this is super gross
    Operation(Operation.Inst inst, List<Block> targets, Block failTarget, int min) {
        this.inst = inst;
        this.count = min;
        this.transitions = null;
        this.target = failTarget;
        this.spec = null;
        this.ints = null;
        this.blockTargets = targets;
    }

    static Operation checkBounds(Block returnBlock) {
        return new Operation(Inst.CHECK_BOUNDS, -1, null, returnBlock, null, null);
    }

    static Operation mkJump(Block target, int insn) {
        return new Operation(Inst.JUMP, insn, null, target, null, null);
    }

    public static Operation mkCallState(Block targetBlock) {
        return new Operation(Inst.CALL_STATE, 0, null, targetBlock, null, null);
    }

    static Operation checkChars(DFA dfa, Block failBlock) {
        var transitions = dfa.getTransitions().
                stream().
                map(t -> Pair.of(t.getLeft(), t.getRight().getStateNumber())).
                collect(Collectors.toList());
        return new Operation(Inst.CHECK_CHARS, -1, transitions, failBlock,  null, null);
    }

    public static Operation call(String methodName, String className, String descriptor) {
        return call(methodName, className, descriptor, false);
    }

    public static Operation call(String methodName, String className, String descriptor, boolean invokeSpecial) {
        var spec = new RefSpec(methodName, className, descriptor);
        var inst = invokeSpecial ? Inst.INVOKESPECIAL : Inst.CALL;
        return new Operation(inst, -1, null, null, spec, null);
    }

    public static Operation callStatic(String methodName, String className, String descriptor) {
        var spec = new RefSpec(methodName, className, descriptor);
        var inst = Inst.INVOKESTATIC;
        return new Operation(inst, -1, null, null, spec, null);
    }

    public static Operation callInterface(String methodName, String className, String descriptor) {
        var spec = new RefSpec(methodName, className, descriptor);
        var inst = Inst.INVOKEINTERFACE;
        return new Operation(inst, -1, null, null, spec, null);
    }

    static Operation pushValue(int val) {
        return new Operation(Inst.VALUE, val, null, null, null, null);
    }

    public static Operation mkReadThis() {
        var spec = new RefSpec(null, null, "", true);
        return new Operation(Inst.READ_VAR, 0, null, null, spec, null);
    }

    public static Operation mkReadVar(MatchingVars vars, String name, String descriptor) {
        int index = vars.indexByName(name);
        var spec = new RefSpec(name, null, descriptor);
        return new Operation(Inst.READ_VAR, index,  null, null,  spec, null);
    }

    public static Operation mkReadField(String field, boolean isSelf, String descriptor) {
        var spec = new RefSpec(field, null, descriptor, true);
        return new Operation(Inst.READ_FIELD, 0, null, null, spec, null);
    }

    public static Operation mkReadStatic(String field, boolean isSelf, String descriptor) {
        var spec = new RefSpec(field, null, descriptor, isSelf);
        return new Operation(Inst.READ_STATIC, -1, null, null, spec, null);
    }

    public static Operation mkPutStatic(String field, boolean isSelf, String descriptor) {
        var spec = new RefSpec(field, null, descriptor, isSelf);
        return new Operation(Inst.PUT_STATIC, -1, null, null, spec, null);
    }

    public static Operation mkReadField(String field, String className, String descriptor) {
        var spec = new RefSpec(field, className, descriptor);
        return new Operation(Inst.READ_FIELD, -1, null, null, spec, null);
    }

    public static Operation mkSetField(String field, String className, String descriptor) {
        var spec = new RefSpec(field, className, descriptor);
        return new Operation(Inst.SET_FIELD, -1, null, null, spec, null);
    }

    public static Operation mkReadVar(int index, String descriptor) {
        var spec = new RefSpec(null, null, descriptor);
        return new Operation(Inst.READ_VAR, index, null , null, spec, null);
    }

    public static Operation mkSetVar(int stateVar, String descriptor) {
        var spec = new RefSpec(null, null, descriptor);
        return new Operation(Inst.SET_VAR, stateVar, null, null, spec, null);
    }

    public static Operation mkSetVar(MatchingVars vars, String varName, String descriptor) {
        var index = vars.indexByName(varName);
        var spec = new RefSpec(varName, null, descriptor);
        return new Operation(Inst.SET_VAR, index, null, null, spec, null);
    }

    static Operation mkReturn(int i) {
        return new Operation(Inst.RETURN, i, null, null, null, null);
    }

    static Operation mkOperation(Inst inst) {
        return new Operation(inst, -1, null,  null, null, null);
    }

    static Operation mkOperation(int i) {
        return new Operation(Inst.PASSTHROUGH, i, null, null, null, null);
    }

    static Operation mkConstructor(String type) {
        // TODO...type is a non-descriptor here?
        var spec = new RefSpec("dummy", "dummy", type);
        return new Operation(Inst.NEW, -1, null, null, spec, null);
    }

    static Operation mkTableSwitch(List<Block> blocks, Block failTarget, int min, int max) {
        return new Operation(Inst.TABLESWITCH, blocks, failTarget, min);
    }

    static Operation mkLookupSwitch(List<Block> blocks, Block failTarget) {
        return new Operation(Inst.LOOKUPSWITCH, blocks, failTarget, 0);
    }

    public enum Inst {
        VALUE,
        READ_CHAR,
        INCREMENT_INDEX,
        DECREMENT_INDEX,
        CHECK_BOUNDS,
        CHECK_CHARS,
        STORE_MATCH,
        SET_VAR,
        SET_FIELD,
        READ_STATIC,
        PUT_STATIC,
        READ_FIELD,
        READ_VAR,
        PASSTHROUGH,
        JUMP,
        LOOKUPSWITCH,
        TABLESWITCH,
        CALL,
        NEW,
        INVOKEINTERFACE,
        INVOKESTATIC,
        INVOKESPECIAL,
        CALL_STATE,
        RETURN,
    }
}
