package com.justinblank.strings;

import java.io.PrintWriter;

class ClassPrinter {

    int indentation;
    PrintWriter pw;
    boolean indented = false;

    ClassPrinter(PrintWriter pw) {
        this.pw = pw;
    }

    void print(Object obj) {
        if (!indented) {
            indent();
            indented = true;
        }
        pw.print(obj);
    }

    private void indent() {
        for (int i = 0; i < indentation; i++) {
            pw.print("    ");
        }
    }

    void println(Object obj) {
        if (!indented) {
            indent();
        }
        pw.println(obj);
        indented = false;
    }

    void printOperation(Operation op) {
        if (op.inst == Operation.Inst.CHECK_CHARS) {
            for (var p : op.transitions) {
                print(op.inst);
                print("(");
                print(p.getLeft().getStart());
                print("-");
                print(p.getLeft().getEnd());
                print(") ->");
                print(p.getRight());
                print(" ELSE JUMP TO " + op.target.toString().toUpperCase());
                println("");
            }
            return;
        }
        print(op.inst);
        print(' ');
        switch (op.inst) {
            case VALUE:
                println(op.count);
                return;
            case CMP:
                // TODO: pretty-print these
                print(op.count);
                print(' ');
            case CHECK_BOUNDS:
            case JUMP:
                println(op.target);
                return;
            case CALL:
                print(op.spec.isSelf ? "Self" : op.spec.className);
                print("#");
                println(op.spec.name);
                return;
            case READ_FIELD:
                if (op.spec != null) {
                    if (op.spec.isSelf) {
                        println("this");
                    } else {
                        println(op.spec.name);
                    }
                }
                return;
            case READ_VAR:
            case SET_VAR:
                if (op.spec != null) {
                    if (op.spec.isSelf) {
                        print("this");
                    } else {
                        print(op.spec.name);
                    }
                    print(' ');
                    println(op.count);
                } else {
                    println(op.count);
                }
                return;
            default:
                println("");
        }
    }

    void printBlock(Block block) {
        println("BLOCK" + block.number + ":");
        indentation++;
        for (Operation op : block.operations) {
            printOperation(op);
        }
        indentation--;
    }

    void printMethod(Method method) {
        println("METHOD " + method.methodName.toUpperCase() + ":");
        for (Block block : method.blocks) {
            indentation++;
            printBlock(block);
            indentation--;
        }
    }

    void printClass(ClassBuilder builder) {
        for (Method method : builder.allMethods()) {
            indentation++;
            printMethod(method);
            indentation--;
        }
    }
}
