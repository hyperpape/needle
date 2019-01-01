package com.justinblank.strings;

import com.justinblank.classloader.MyClassLoader;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.objectweb.asm.Opcodes.*;

public class DFACompiler {

    private static final String STATE_FIELD = "state";
    private static final String CHAR_FIELD = "c";
    private static final String ACCEPTING_FIELD = "accepting";
    private static final String LENGTH_FIELD = "length";
    private static final String STRING_FIELD = "string";
    private static final String INDEX_FIELD = "index";

    private Map<DFA, Integer> dfaMethodMap = new IdentityHashMap<>();
    private int methodCount = 0;
    private String className;
    private ClassWriter classWriter;
    private DFA dfa;
    private final Map<Character, String> rangeConstants = new HashMap<>();

    private DFACompiler(ClassWriter classWriter, String className, DFA dfa) {
        this.classWriter = classWriter;
        this.className = className;
        this.dfa = dfa;
    }

    public static void compileString(String regex, String className) {
        NFA nfa = ASTToNFA.createNFA(RegexParser.parse(regex));
        compile(NFAToDFACompiler.compile(nfa), className);
    }

    public static void compile(DFA dfa, String name) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V9, ACC_PUBLIC, name, null, "java/lang/Object", new String[]{"com/justinblank/strings/Matcher"});
        DFACompiler compiler = new DFACompiler(cw, name, dfa);
        compiler.addFields();
        compiler.addCharConstants();
        compiler.addConstructor();
        compiler.addIterMethod();
        compiler.addMatchMethod(dfa);
        compiler.generateTransitionMethods(dfa);
        // current impl requires that accepted be called last
        compiler.addWasAcceptedMethod();

        byte[] classBytes = cw.toByteArray();
        MyClassLoader.getInstance().loadClass(name, classBytes);
    }

    private void addFields() {
        this.classWriter.visitField(ACC_PRIVATE, ACCEPTING_FIELD, "Z", null, 0);
        this.classWriter.visitField(ACC_PRIVATE, INDEX_FIELD, "I", null,0);
        this.classWriter.visitField(ACC_PRIVATE, CHAR_FIELD, "C", null, null);
        this.classWriter.visitField(ACC_PRIVATE, STRING_FIELD, "Ljava/lang/String;", null, null);
        this.classWriter.visitField(ACC_PRIVATE, LENGTH_FIELD,  "I", null, 0);
        this.classWriter.visitField(ACC_PRIVATE, STATE_FIELD, "I", null, 0);
    }

    private void addMatchMethod(DFA dfa) {
        MethodVisitor mv = this.classWriter.visitMethod(ACC_PUBLIC, "matches", "()Z", null, null);

        Label returnLabel = new Label();
        Label iterateLabel = new Label();

        mv.visitLabel(iterateLabel);
        mv.visitVarInsn(ALOAD, 0);

        // Move to next char, return if necessary
        mv.visitMethodInsn(INVOKEVIRTUAL, className, "iterate", "()Z", false);
        mv.visitInsn(ICONST_1);
        mv.visitJumpInsn(IF_ICMPEQ, returnLabel);

        // dispatch to method associated with current state
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, STATE_FIELD, "I");
        int labelCount = dfa.statesCount();
        Label[] stateLabels = new Label[labelCount];
        Label failLabel = new Label();
        for (int i = 0; i < labelCount; i++) {
            stateLabels[i] = new Label();
        }
        mv.visitTableSwitchInsn(0, labelCount - 1, failLabel, stateLabels);

        for (int i = 0; i < labelCount; i++) {
            mv.visitLabel(stateLabels[i]);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, className, "state" + i, "()V", false);
            mv.visitJumpInsn(GOTO, iterateLabel);
        }

        // handle case of failure
        mv.visitLabel(failLabel);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(IRETURN);

        // check if the dfa had a match
        mv.visitLabel(returnLabel);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, className, "wasAccepted", "()V", false);
        mv.visitIntInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, ACCEPTING_FIELD, "Z");
        mv.visitInsn(IRETURN);
        mv.visitMaxs(-1,-1);
        mv.visitEnd();
    }

    private void addWasAcceptedMethod() {
        MethodVisitor mv = classWriter.visitMethod(ACC_PRIVATE, "wasAccepted", "()V", null, null);
        BitSet acceptanceBits = new BitSet(dfaMethodMap.size());
        for (Map.Entry<DFA, Integer> e : dfaMethodMap.entrySet()) {
            if (e.getKey().isAccepting()) {
                acceptanceBits.set(e.getValue());
            }
        }
        byte[] places = acceptanceBits.toByteArray();

        Label returnLabel = new Label();
        Label[] labels = new Label[places.length + 1];
        labels[0] = new Label();
        for (int i = 0; i < places.length; i++) {
            labels[i + 1] = new Label();
        }
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, STATE_FIELD, "I");
        mv.visitInsn(DUP);
        // states are 0 indexed, but we're computing state | bitfield
        mv.visitInsn(ICONST_1);
        mv.visitInsn(IADD);
        mv.visitIntInsn(BIPUSH, 8);
        mv.visitInsn(IREM);
        mv.visitInsn(SWAP);
        mv.visitIntInsn(BIPUSH, 8);
        mv.visitInsn(IDIV);
        // default is impossible, so just use labels[0] for simplicity
        mv.visitTableSwitchInsn(-1, places.length -1, labels[0], labels);
        // push a label for state -1, which indicates failure
        mv.visitLabel(labels[0]);
        mv.visitIntInsn(BIPUSH, 0);
        mv.visitInsn(IAND);
        mv.visitJumpInsn(GOTO, returnLabel);
        for (int i = 0; i < places.length; i++) {
            byte accepting = places[i];
            mv.visitLabel(labels[i + 1]);
            mv.visitIntInsn(BIPUSH, accepting);
            mv.visitInsn(IAND);
            mv.visitJumpInsn(GOTO, returnLabel);
        }
        mv.visitLabel(returnLabel);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(SWAP);
        mv.visitFieldInsn(PUTFIELD, className, ACCEPTING_FIELD, "Z");
        mv.visitInsn(RETURN);
        mv.visitMaxs(-1, -1);
        mv.visitEnd();
    }

    private void addIterMethod() {
        MethodVisitor mv = this.classWriter.visitMethod(ACC_PRIVATE, "iterate","()Z", null, null);

        // Bounds check
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, INDEX_FIELD, "I");
        mv.visitVarInsn(ISTORE, 1);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, LENGTH_FIELD, "I");
        Label iterationDone = new Label();
        mv.visitJumpInsn(IF_ICMPGE, iterationDone);

        // Retrieve next char
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, STRING_FIELD, "Ljava/lang/String;");
        mv.visitVarInsn(ILOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "charAt", "(I)C", false);
        mv.visitFieldInsn(PUTFIELD, className, CHAR_FIELD, "C");

        // Increment index
        mv.visitVarInsn(ALOAD, 0);
        mv.visitIincInsn(1, 1);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitFieldInsn(PUTFIELD, className, INDEX_FIELD, "I");

        mv.visitInsn(ICONST_0);
        mv.visitInsn(IRETURN);

        mv.visitLabel(iterationDone);
        mv.visitInsn(ICONST_1);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void generateTransitionMethods(DFA dfa) {
        Set<DFA> seen = new HashSet<>();
        Queue<DFA> pending = new LinkedList<>();
        pending.add(dfa);
        seen.add(dfa);

        while (!pending.isEmpty()) {
            DFA next = pending.poll();
            for (Pair<CharRange, DFA> toAdd : next.getTransitions()) {
                if (!seen.contains(toAdd.getRight())) {
                    seen.add(toAdd.getRight());
                    pending.add(toAdd.getRight());
                }
            }
            generateTransitionMethod(next, methodDesignator(next));
        }
    }

    private void generateTransitionMethod(DFA dfa, Integer transitionNumber) {
        MethodVisitor mv = this.classWriter.visitMethod(ACC_PRIVATE, "state" + transitionNumber, "()V", null, null);

        Label returnLabel = new Label();
        Label failLabel = new Label();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, CHAR_FIELD, "C");
        mv.visitVarInsn(ISTORE, 1);
        Map<DFA, Label> transitionTargets = new IdentityHashMap<>();

        for (Pair<CharRange, DFA> transition : dfa.getTransitions()) {
            Label transitionLabel = transitionTargets.computeIfAbsent(transition.getRight(), d -> new Label());
            CharRange charRange = transition.getLeft();
            mv.visitVarInsn(ILOAD, 1);
            if (charRange.getStart() <= 128) {
                mv.visitIntInsn(BIPUSH, charRange.getStart());
            }
            else {
                mv.visitFieldInsn(GETSTATIC, this.className, rangeConstants.get(charRange.getStart()), "C");
            }
            mv.visitJumpInsn(IF_ICMPLT, failLabel);

            if (charRange.getStart() <= 128) {
                mv.visitIntInsn(BIPUSH, charRange.getEnd());
            }
            else {
                mv.visitFieldInsn(GETSTATIC, this.className, rangeConstants.get(charRange.getEnd()), "C");
            }

            mv.visitIntInsn(ILOAD, 1);
            mv.visitJumpInsn(IF_ICMPGE, transitionLabel);
        }
        mv.visitJumpInsn(GOTO, failLabel);
        for (Map.Entry<DFA, Label> e : transitionTargets.entrySet()) {
            mv.visitLabel(e.getValue());
            mv.visitVarInsn(ALOAD, 0);
            // TODO: this implicitly limits the number of states..change or decide how to handle explicitly
            mv.visitIntInsn(BIPUSH, methodDesignator(e.getKey()));
            mv.visitFieldInsn(PUTFIELD, className, STATE_FIELD,  "I");
            mv.visitJumpInsn(GOTO, returnLabel);
        }
        mv.visitLabel(failLabel);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(ICONST_M1);
        mv.visitFieldInsn(PUTFIELD, className, STATE_FIELD, "I");
        mv.visitLabel(returnLabel);
        mv.visitInsn(RETURN);

        mv.visitMaxs(-1, -1);
        mv.visitEnd();
    }

    private void addCharConstants() {
        AtomicInteger constCount = new AtomicInteger(0);
        dfa.allStates().stream().map(DFA::getTransitions).flatMap(List::stream).map(Pair::getLeft).forEach(charRange -> {
            if (charRange.getStart() > 128) {
                rangeConstants.computeIfAbsent(charRange.getStart(), c -> {
                    String constName = "CHAR_CONST_" + constCount.incrementAndGet();
                    classWriter.visitField(ACC_STATIC | ACC_PRIVATE | ACC_FINAL, constName, "C", null, charRange.getStart());
                    return constName;
                });
            }
            if (charRange.getEnd() > 128) {
                rangeConstants.computeIfAbsent(charRange.getEnd(), c -> {
                    String constName = "CHAR_CONST_" + constCount.incrementAndGet();
                    classWriter.visitField(ACC_STATIC | ACC_PRIVATE | ACC_FINAL, constName, "C", null, charRange.getEnd());
                    return constName;
                });
            }
        });
    }

    private Integer methodDesignator(DFA right) {
        return dfaMethodMap.computeIfAbsent(right, d -> methodCount++);
    }

    protected void addConstructor() {
        MethodVisitor mw = this.classWriter.visitMethod(ACC_PUBLIC,
                "<init>",
                "(Ljava/lang/String;)V",
                null,
                null);
        mw.visitVarInsn(ALOAD, 0);
        mw.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mw.visitVarInsn(ALOAD, 0);
        mw.visitVarInsn(ALOAD, 1);
        mw.visitFieldInsn(PUTFIELD, className, STRING_FIELD, "Ljava/lang/String;");
        mw.visitVarInsn(ALOAD, 0);
        mw.visitVarInsn(ALOAD, 1);
        mw.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "length", "()I", false);
        mw.visitFieldInsn(PUTFIELD, className, LENGTH_FIELD, "I");
        mw.visitVarInsn(ALOAD, 0);
        if (dfa.isAccepting()) {
            mw.visitInsn(ICONST_1);
        }
        else {
            mw.visitInsn(ICONST_0);
        }
        mw.visitFieldInsn(PUTFIELD, className, ACCEPTING_FIELD, "Z");
        mw.visitInsn(RETURN);
        // this code uses a maximum of one stack element and one local variable
        mw.visitMaxs(1, 1);
        mw.visitEnd();
    }
}

