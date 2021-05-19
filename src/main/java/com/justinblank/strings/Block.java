package com.justinblank.strings;

import org.objectweb.asm.Label;

import java.util.List;

class Block {

    int number;
    List<Operation> operations;
    Label label;

    Block(int number, List<Operation> operations) {
        this.number = number;
        this.operations = operations;
    }

    Block operate(int i) {
        addOperation(Operation.mkOperation(i));
        return this;
    }

    Block readVar(int index, String descriptor) {
        addOperation(Operation.mkReadVar(index, descriptor));
        return this;
    }

    Block readVar(MatchingVars vars, String varName, String descriptor) {
        addOperation(Operation.mkReadVar(vars, varName, descriptor));
        return this;
    }

    Block readStatic(String field, boolean isSelf, String descriptor) {
        addOperation(Operation.mkReadStatic(field, isSelf, descriptor));
        return this;
    }

    Block putStatic(String field, boolean isSelf, String descriptor) {
        addOperation(Operation.mkPutStatic(field, isSelf, descriptor));
        return this;
    }

    Block setVar(int index, String descriptor) {
        addOperation(Operation.mkSetVar(index, descriptor));
        return this;
    }

    Block setVar(MatchingVars vars, String varName, String descriptor) {
        addOperation(Operation.mkSetVar(vars, varName, descriptor));
        return this;
    }

    Block readField(String field, boolean isSelf, String descriptor) {
        addOperation(Operation.mkReadField(field, isSelf, descriptor));
        return this;
    }

    Block push(int i) {
        addOperation(Operation.pushValue(i));
        return this;
    }

    Block cmp(Block target, int i) {
        addOperation(Operation.mkCmp(target, i));
        return this;
    }

    Block jump(Block target, int i) {
        addOperation(Operation.mkJump(target, i));
        return this;
    }

    Block addReturn(int i) {
        addOperation(Operation.mkReturn(i));
        return this;
    }

    Block readThis() {
        addOperation(Operation.mkReadThis());
        return this;
    }

    Block call(String methodName, String className, String descriptor) {
        addOperation(Operation.call(methodName, className, descriptor, false));
        return this;
    }

    Block call(String methodName, String className, String descriptor, boolean invokeSpecial) {
        addOperation(Operation.call(methodName, className, descriptor, invokeSpecial));
        return this;
    }

    Block callStatic(String methodName, String className, String descriptor) {
        addOperation(Operation.callStatic(methodName, className, descriptor));
        return this;
    }

    Block callInterface(String methodName, String className, String descriptor) {
        addOperation(Operation.callInterface(methodName, className, descriptor));
        return this;
    }

    Block construct(String type) {
        addOperation(Operation.mkConstructor(type));
        return this;
    }

    @Override
    public String toString() {
        return "Block" + number;
    }

    void addOperation(Operation op) {
        this.operations.add(op);
    }

    public Label getLabel() {
        if (label == null) {
            label = new Label();
        }
        return label;
    }
}
