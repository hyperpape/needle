package com.justinblank.strings;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.util.Collection;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Opcodes.SIPUSH;

class CompilerUtil {

    public static final String OBJECT_DESCRIPTOR = "Ljava/lang/Object;";
    public static final String STRING_DESCRIPTOR = "Ljava/lang/String;";

    /**
     * Utility method, push an int onto the stack, assuming it is no bigger than a short
     *
     * @param mv       method visitor
     * @param constant the constant
     */
    static void pushInt(MethodVisitor mv, int constant) {
        if (constant < 6 && constant >= -1) {
            switch (constant) {
                case -1: {
                    mv.visitInsn(ICONST_M1);
                    break;
                }
                case 0: {
                    mv.visitInsn(ICONST_0);
                    break;
                }
                case 1: {
                    mv.visitInsn(ICONST_1);
                    break;
                }
                case 2: {
                    mv.visitInsn(ICONST_2);
                    break;
                }
                case 3: {
                    mv.visitInsn(ICONST_3);
                    break;
                }
                case 4: {
                    mv.visitInsn(ICONST_4);
                    break;
                }
                case 5: {
                    mv.visitInsn(ICONST_5);
                }
            }
        } else if (constant <= 127 && constant >= 0) {
            mv.visitIntInsn(BIPUSH, constant);
        } else if (constant >= Short.MIN_VALUE && constant <= Short.MAX_VALUE) {
            mv.visitIntInsn(SIPUSH, constant);
        } else {
            mv.visitLdcInsn(constant);
        }
    }

}
