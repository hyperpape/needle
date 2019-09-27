package com.justinblank.strings;

import com.justinblank.classloader.MyClassLoader;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.objectweb.asm.Opcodes.*;

public class DFACompiler {

    protected static final String STATE_FIELD = "state";
    protected static final String CHAR_FIELD = "c";
    protected static final String LENGTH_FIELD = "length";
    protected static final String STRING_FIELD = "string";
    protected static final String INDEX_FIELD = "index";
    // TODO: measure threshold, 8 is just a random choice
    public static final int MAX_STATES_FOR_SWITCH = 8;

    private Map<DFA, Integer> dfaMethodMap = new IdentityHashMap<>();
    private int methodCount = 0;
    private String className;
    private ClassWriter classWriter;
    private DFA dfa;
    private final Map<Character, String> rangeConstants = new HashMap<>();

    protected DFACompiler(ClassWriter classWriter, String className, DFA dfa) {
        if (dfa.statesCount() > Short.MAX_VALUE) {
            throw new IllegalArgumentException("Can't compile DFAs with more than " + Short.MAX_VALUE + " states");
        }
        this.classWriter = classWriter;
        this.className = className;
        this.dfa = dfa;
    }

    public static Pattern compileString(String regex, String className) {
        NFA nfa = ThompsonNFABuilder.createNFA(RegexParser.parse(regex));
        return compile(NFAToDFACompiler.compile(nfa), className);
    }

    public static Pattern compile(DFA dfa, String name) {
        byte[] classBytes = generateClassAsBytes(dfa, name);
        Class<?> matcherClass = MyClassLoader.getInstance().loadClass(name, classBytes);
        Class<? extends Pattern> c = createPatternClass("Pattern"  + name, (Class<? extends Matcher>) matcherClass);
        try {
            return (Pattern) c.getDeclaredConstructors()[0].newInstance();
        }
        catch (Exception e) {
            // TODO: determine good exceptions/result types
            throw new RuntimeException(e);
        }
    }

    static byte[] generateClassAsBytes(DFA dfa, String name) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V9, ACC_PUBLIC, name, null, "java/lang/Object", new String[]{"com/justinblank/strings/Matcher"});
        DFACompiler compiler = new DFACompiler(cw, name, dfa);
        compiler.compile();

        return cw.toByteArray();
    }

    public static void writeClass(DFA dfa, String name, OutputStream os) throws IOException {
        os.write(generateClassAsBytes(dfa, name));
    }

    private static Class<? extends Pattern> createPatternClass(String name, Class<? extends Matcher> m) {
        DynamicType.Builder<? extends Pattern> builder = new ByteBuddy().subclass(Pattern.class).name(name);
        builder = builder.method(named("matcher")).intercept(MethodDelegation.toConstructor(m));
        return builder.make().load(MyClassLoader.getInstance()).getLoaded();
    }

    protected void compile() {
        addFields();
        addCharConstants();
        addConstructor();
        // addIterMethod();
        addMatchMethod();
        generateTransitionMethods();
        // current impl requires that accepted be called last
        List<DFA> acceptingStates = getAcceptingStates();
        if (acceptingStates.size() > MAX_STATES_FOR_SWITCH) {
            addLargeWasAcceptedMethod();
        }
        else if (acceptingStates.size() > 1) {
            addSmallWasAcceptedMethod(acceptingStates);
        }
        else {
            addSingleStateAcceptedMethod(acceptingStates);
        }
    }

    private List<DFA> getAcceptingStates() {
        List<DFA> acceptingStates = new ArrayList<>();
        for (Map.Entry<DFA, Integer> e : dfaMethodMap.entrySet()) {
            if (e.getKey().isAccepting()) {
                acceptingStates.add(e.getKey());
            }
        }
        return acceptingStates;
    }

    protected void addFields() {
        this.classWriter.visitField(ACC_PRIVATE, INDEX_FIELD, "I", null,0);
        this.classWriter.visitField(ACC_PRIVATE, CHAR_FIELD, "C", null, null);
        this.classWriter.visitField(ACC_PRIVATE, STRING_FIELD, "Ljava/lang/String;", null, null);
        this.classWriter.visitField(ACC_PRIVATE, LENGTH_FIELD,  "I", null, 0);
        this.classWriter.visitField(ACC_PRIVATE, STATE_FIELD, "I", null, 0);
    }

    protected void addMatchMethod() {
        MethodVisitor mv = this.classWriter.visitMethod(ACC_PUBLIC, "matches", "()Z", null, null);

        Label returnLabel = new Label();
        Label iterateLabel = new Label();
        Label failLabel = new Label();

        final int counterVar = 1;
        final int lengthVar = 2;
        final int stringVar = 3;
        final int charVar = 4;

        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, counterVar);

        // push string to local var
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, STRING_FIELD, "Ljava/lang/String;");
        mv.visitVarInsn(ASTORE, stringVar);
        mv.visitVarInsn(ALOAD, stringVar);

        // push string length to local var
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "length", "()I", false);
        mv.visitVarInsn(ISTORE, lengthVar);
        mv.visitVarInsn(ILOAD, lengthVar);

        mv.visitLabel(iterateLabel);

        // check state and return if need be
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, STATE_FIELD, "I");
        mv.visitInsn(ICONST_M1);
        mv.visitJumpInsn(IF_ICMPEQ, failLabel);

        // read next char, store in local var
        mv.visitVarInsn(ILOAD, counterVar);
        mv.visitVarInsn(ILOAD, lengthVar);
        mv.visitJumpInsn(IF_ICMPEQ, returnLabel);
        mv.visitVarInsn(ALOAD, stringVar);
        mv.visitVarInsn(ILOAD, counterVar);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "charAt", "(I)C", false);
        mv.visitVarInsn(ISTORE, charVar);
        mv.visitIincInsn(counterVar, 1);

        // dispatch to method associated with current state
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, STATE_FIELD, "I");
        int labelCount = dfa.statesCount();
        Label[] stateLabels = new Label[labelCount];
        for (int i = 0; i < labelCount; i++) {
            stateLabels[i] = new Label();
        }
        mv.visitTableSwitchInsn(0, labelCount - 1, failLabel, stateLabels);

        for (int i = 0; i < labelCount; i++) {
            mv.visitLabel(stateLabels[i]);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ILOAD, charVar);
            mv.visitMethodInsn(INVOKEVIRTUAL, className, "state" + i, "(C)V", false);
            mv.visitJumpInsn(GOTO, iterateLabel);
        }

        // handle case of failure
        mv.visitLabel(failLabel);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(IRETURN);

        // check if the dfa had a match
        mv.visitLabel(returnLabel);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, className, "wasAccepted", "()Z", false);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(-1,-1);
        mv.visitEnd();
    }

    private void addSingleStateAcceptedMethod(List<DFA> acceptingStates) {
        MethodVisitor mv = classWriter.visitMethod(ACC_PRIVATE, "wasAccepted", "()Z", null, null);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, STATE_FIELD, "I");
        int acceptingState = acceptingStates.get(0).getStateNumber();
        pushShortInt(mv, acceptingState);
        Label success = new Label();
        mv.visitJumpInsn(IF_ICMPEQ, success);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(IRETURN);
        mv.visitLabel(success);
        mv.visitInsn(ICONST_1);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(-1,-1);
        mv.visitEnd();
    }

    private void addSmallWasAcceptedMethod(List<DFA> acceptingStates) {
        MethodVisitor mv = classWriter.visitMethod(ACC_PRIVATE, "wasAccepted", "()Z", null, null);
        Label[] labels = new Label[acceptingStates.size()];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = new Label();
        }
        int[] keys = new int[acceptingStates.size()];
        for (int i = 0; i < acceptingStates.size(); i++) {
            keys[i] = acceptingStates.get(i).getStateNumber();
        }
        Label noMatch = new Label();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, STATE_FIELD, "I");
        mv.visitLookupSwitchInsn(noMatch, keys, labels);
        for (int i = 0; i < labels.length; i++) {
            mv.visitLabel(labels[i]);
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IRETURN);
        }
        mv.visitLabel(noMatch);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(-1,-1);
        mv.visitEnd();
    }

    private void addLargeWasAcceptedMethod() {
        MethodVisitor mv = classWriter.visitMethod(ACC_PRIVATE, "wasAccepted", "()Z", null, null);
        BitSet acceptanceBits = new BitSet(dfaMethodMap.size());
        for (Map.Entry<DFA, Integer> e : dfaMethodMap.entrySet()) {
            if (e.getKey().isAccepting()) {
                acceptanceBits.set(e.getValue());
            }
        }
        byte[] places = acceptanceBits.toByteArray();

        Label preReturnLabel = new Label();
        Label[] labels = new Label[places.length + 1];
        labels[0] = new Label();
        for (int i = 0; i < places.length; i++) {
            labels[i + 1] = new Label();
        }
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, STATE_FIELD, "I");
        mv.visitInsn(DUP);
        mv.visitIntInsn(BIPUSH, 8);
        mv.visitInsn(IREM); // stack = state, state % 8
        mv.visitIntInsn(BIPUSH, 1);
        mv.visitInsn(SWAP); // stack = state, 1, state % 8
        mv.visitInsn(ISHL); // stack = state, 1 << (state % 8)
        mv.visitInsn(SWAP);
        mv.visitIntInsn(BIPUSH, 8);
        mv.visitInsn(IDIV); // stack = (1 << state % 8), state / 8
        // default is impossible, so just use labels[0] for simplicity
        mv.visitTableSwitchInsn(-1, places.length -1, labels[labels.length - 1], labels);
        // push a label for state -1, which indicates failure
        mv.visitLabel(labels[0]);
        mv.visitIntInsn(BIPUSH, 0);
        mv.visitInsn(IAND);
        mv.visitJumpInsn(GOTO, preReturnLabel);
        for (int i = 0; i < places.length; i++) {
            byte accepting = places[i];
            mv.visitLabel(labels[i + 1]);
            mv.visitIntInsn(BIPUSH, accepting);
            mv.visitInsn(IAND);
            mv.visitJumpInsn(GOTO, preReturnLabel);
        }
        mv.visitLabel(preReturnLabel);
        Label returnLabel = new Label();
        Label zeroLabel = new Label();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(SWAP);

        mv.visitInsn(ICONST_0);
        mv.visitJumpInsn(IF_ICMPEQ, zeroLabel);
        mv.visitInsn(ICONST_1);
        mv.visitJumpInsn(GOTO, returnLabel);
        mv.visitLabel(zeroLabel);
        mv.visitIntInsn(BIPUSH, 0);
        mv.visitLabel(returnLabel);

        mv.visitInsn(IRETURN);
        mv.visitMaxs(-1, -1);
        mv.visitEnd();
    }

    protected void addIterMethod() {
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

    protected void generateTransitionMethods() {
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

    protected void generateTransitionMethod(DFA node, Integer transitionNumber) {
        MethodVisitor mv = this.classWriter.visitMethod(ACC_PRIVATE, "state" + transitionNumber, "(C)V", null, null);

        Label returnLabel = new Label();
        Label failLabel = new Label();
        List<Pair<CharRange, DFA>> transitions = node.getTransitions();
        if (!transitions.isEmpty()) {
            if (transitions.size() > 1 || !transitions.get(0).getLeft().isSingleCharRange()) {
                generateTransitionJumps(node, mv, returnLabel, failLabel);
            } else {
                generateSingleCharTransition(node, mv, returnLabel, failLabel);
            }
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

    private void generateTransitionJumps(DFA node, MethodVisitor mv, Label returnLabel, Label failLabel) {
        Map<DFA, Label> transitionTargets = new IdentityHashMap<>();

        for (Pair<CharRange, DFA> transition : node.getTransitions()) {
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

            if (charRange.getEnd() <= 128) {
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
            int nextState = methodDesignator(e.getKey());
            pushShortInt(mv, nextState);
            mv.visitFieldInsn(PUTFIELD, className, STATE_FIELD,  "I");
            mv.visitJumpInsn(GOTO, returnLabel);
        }
    }

    private void generateSingleCharTransition(DFA node, MethodVisitor mv, Label returnLabel, Label failLabel) {
        CharRange charRange = node.getTransitions().get(0).getLeft();
        DFA next = node.getTransitions().get(0).getRight();
        mv.visitVarInsn(ILOAD, 1);
        if (charRange.getStart() <= 128) {
            mv.visitIntInsn(BIPUSH, charRange.getStart());
        }
        else {
            mv.visitFieldInsn(GETSTATIC, this.className, rangeConstants.get(charRange.getStart()), "C");
        }
        mv.visitJumpInsn(IF_ICMPNE, failLabel);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitIntInsn(BIPUSH, methodDesignator(next));
        mv.visitFieldInsn(PUTFIELD, className, STATE_FIELD, "I");
        mv.visitJumpInsn(GOTO, returnLabel);

    }

    protected void addCharConstants() {
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

    protected Integer methodDesignator(DFA right) {
        Integer stateNumber = dfaMethodMap.computeIfAbsent(right, d -> methodCount++);
        if (stateNumber > 1 << 15) {
            // TODO: decide best approach to large state automata
            throw new IllegalStateException("Too many states");
        }
        return stateNumber;
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
        mw.visitInsn(RETURN);
        // this code uses a maximum of one stack element and one local variable
        mw.visitMaxs(1, 1);
        mw.visitEnd();
    }

    /**
     * Utility method, push an int onto the stack, assuming it is no bigger than a short
     * @param mv method visitor
     * @param constant the constant
     */
    private void pushShortInt(MethodVisitor mv, int constant) {
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
        }
        else if (constant <= 127) {
            mv.visitIntInsn(BIPUSH, constant);
        }
        else {
            mv.visitIntInsn(SIPUSH, constant);
        }
    }


    // Intellij won't debug through generated bytecode that lacks an accompanying classfile
    public static void debug() {
        if (false) {
            System.out.println("");
        }
    }

    // Intellij won't debug through generated bytecode that lacks an accompanying classfile
    public static void debug(int i) {
        System.out.println("debug: " + i);
    }

    protected ClassWriter getClassWriter() {
        return classWriter;
    }

    protected String getClassName() {
        return className;
    }

    protected DFA getDFA() {
        return dfa;
    }

    public static void main(String[] args) {
        try {
            DFA dfa = NFAToDFACompiler.compile(new ThompsonNFABuilder().build(RegexParser.parse("[0-9]+")));
            DFACompiler.writeClass(dfa, "DigitPattern", new FileOutputStream("/home/justin/code/BytecodeRegex/target/DigitPattern.class"));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

