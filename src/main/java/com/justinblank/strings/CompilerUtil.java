package com.justinblank.strings;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.util.Collection;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Opcodes.SIPUSH;

class CompilerUtil {

    public static final String STRING_DESCRIPTOR = "Ljava/lang/String;";

    static Label[] makeLabelsForCollection(Collection<?> collection) {
        int labelSize = collection.size();
        return makeLabels(labelSize);
    }

    static Label[] makeLabels(int labelSize) {
        Label[] labels = new Label[labelSize];
        for (int i = 0; i < labelSize; i++) {
            labels[i] = new Label();
        }
        return labels;
    }

    /**
     * Utility method, push an int onto the stack, assuming it is no bigger than a short
     *
     * @param mv       method visitor
     * @param constant the constant
     */
    static void pushShortInt(MethodVisitor mv, int constant) {
        if (constant < 6) {
            switch (constant) {
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
        } else if (constant <= 127) {
            mv.visitIntInsn(BIPUSH, constant);
        } else {
            mv.visitIntInsn(SIPUSH, constant);
        }
    }

}
