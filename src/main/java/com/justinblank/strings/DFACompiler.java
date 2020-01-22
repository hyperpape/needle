package com.justinblank.strings;

import com.justinblank.classloader.MyClassLoader;
import com.justinblank.strings.RegexAST.Node;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

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
    private Map<Integer, DFA> stateMap = new HashMap<>();
    private String className;
    private ClassWriter classWriter;
    private DFA dfa;
    private Factorization factors;
    private final Map<Character, String> rangeConstants = new HashMap<>();

    protected DFACompiler(ClassWriter classWriter, String className, DFA dfa, Factorization factors) {
        // Somewhere between this value and Short.MAX_VALUE, we run into classes that can't be created because they're
        // so large
        if (dfa.statesCount() > Short.MAX_VALUE / 2) {
            throw new IllegalArgumentException("Can't compile DFAs with more than " + (Short.MAX_VALUE / 2) + " states");
        }
        this.classWriter = classWriter;
        this.className = className;
        this.dfa = dfa;
        this.factors = factors;
    }

    public static Pattern compile(String regex, String className) {
        Node node = RegexParser.parse(regex);
        Factorization factors = node.bestFactors();

        NFA nfa = ThompsonNFABuilder.createNFA(node);
        return compile(NFAToDFACompiler.compile(nfa), factors, className);
    }

    private static Pattern compile(DFA dfa, Factorization factors, String name) {
        byte[] classBytes = generateClassAsBytes(dfa, factors, name);
        Class<?> matcherClass = MyClassLoader.getInstance().loadClass(name, classBytes);
        Class<? extends Pattern> c = createPatternClass("Pattern"  + name, (Class<? extends Matcher>) matcherClass);
        try {
            return (Pattern) c.getDeclaredConstructors()[0].newInstance();
        }
        catch (Throwable t) {
            // TODO: determine good exceptions/result types
            throw new RuntimeException(t);
        }
    }

    static byte[] generateClassAsBytes(DFA dfa, Factorization factors, String name) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V9, ACC_PUBLIC, name, null, "java/lang/Object", new String[]{"com/justinblank/strings/Matcher"});
        DFACompiler compiler = new DFACompiler(cw, name, dfa, factors);
        compiler.compile();

        return cw.toByteArray();
    }

    public static void writeClass(DFA dfa, Factorization factors, String name, OutputStream os) throws IOException {
        os.write(generateClassAsBytes(dfa, factors, name));
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
        generateTransitionMethods();
        addContainedInMethod();
        addMatchMethod();
        if (isLargeStateCount()) {
            generateStateGroupMethods();
        }
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

    /**
     * When we have a large number of states to consider, we don't want a single top-level method that switches on
     * states, but instead we want a smaller number of methods, each of which handles dispatching to some subset of
     * states.
     */
    private void generateStateGroupMethods() {
        for (int i = 0; i < getStateGroupCount(); i++) {
            MethodVisitor mv = this.classWriter.visitMethod(ACC_PUBLIC, "stateGroup" + i, "(CII)I", null, null);
            MatchingVars vars = new MatchingVars(1, 2, 3, -4, -5);
            Label returnLabel = new Label();
            Label failLabel = new Label();

            int startState = i * 64;
            int endState = Math.min((i + 1) * 64, dfa.statesCount());
            Label[] stateLabels = makeLabels(endState - startState);

            mv.visitVarInsn(ILOAD, vars.stateVar);
            mv.visitTableSwitchInsn(startState, endState - 1, failLabel, stateLabels);
            for (int j = startState; j < endState; j++) {
                mv.visitLabel(stateLabels[j - startState]);
                stateMatch(mv, vars, j, j);
                mv.visitJumpInsn(GOTO, returnLabel);
            }
            mv.visitLabel(failLabel);
            mv.visitInsn(ICONST_M1);
            mv.visitInsn(IRETURN);
            mv.visitLabel(returnLabel);
            mv.visitIntInsn(ILOAD, vars.stateVar); // TODO: decide if it's better to avoid this store/load
            mv.visitInsn(IRETURN);
            mv.visitMaxs(-1,-1);
            mv.visitEnd();
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

    protected void addContainedInMethod() {
        addMatchMethodInternal(true);
    }

    protected void addMatchMethod() {
        addMatchMethodInternal(false);
    }

    protected void addMatchMethodInternal(boolean containedIn) {
        MethodVisitor mv;
        if (containedIn) {
            mv = this.classWriter.visitMethod(ACC_PUBLIC, "containedIn", "()Z", null, null);
        }
        else {
            mv = this.classWriter.visitMethod(ACC_PUBLIC, "matches", "()Z", null, null);
        }

        Label returnLabel = new Label();
        Label iterateLabel = new Label();
        Label failLabel = new Label();
        Label postStateCheckLabel = new Label();

        MatchingVars vars = new MatchingVars( 4, 1, 5, 2, 3);

        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, vars.stateVar);

        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, vars.counterVar);

        // push string to local var
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, STRING_FIELD, "Ljava/lang/String;");
        mv.visitVarInsn(ASTORE, vars.stringVar);
        mv.visitVarInsn(ALOAD, vars.stringVar);

        // push string length to local var
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, LENGTH_FIELD, "I");
        mv.visitVarInsn(ISTORE, vars.lengthVar);
        mv.visitVarInsn(ILOAD, vars.lengthVar);

        mv.visitLabel(iterateLabel);

        // Check state. If we're in matching mode, return when state is negative, otherwise continue.
        // If we're in searching mode, reset the search on a negative state, otherwise check for acceptance
        if (containedIn) {
            emitInvokeWasAccepted(mv, vars);
            mv.visitInsn(ICONST_1);
            mv.visitJumpInsn(IF_ICMPEQ, returnLabel);

            Optional<String> prefix = factors.getSharedPrefix();
            if (prefix.isPresent()) {
                emitPrefixFindingLoop(mv, vars, prefix.get(), postStateCheckLabel, failLabel);
            }
            else {
                mv.visitVarInsn(ILOAD, vars.stateVar);
                mv.visitInsn(ICONST_M1);
                mv.visitJumpInsn(IF_ICMPNE, postStateCheckLabel);
                mv.visitInsn(ICONST_0);
                mv.visitVarInsn(ISTORE, vars.stateVar);
            }
        }
        else {
            mv.visitVarInsn(ILOAD, vars.stateVar);
            mv.visitInsn(ICONST_M1);
            mv.visitJumpInsn(IF_ICMPEQ, failLabel);
        }

        mv.visitLabel(postStateCheckLabel);
        // read next char, store in local var
        emitBoundsCheck(mv, vars, returnLabel);
        emitReadChar(mv, vars);
        mv.visitVarInsn(ISTORE, vars.charVar);

        // dispatch to method associated with current state
        mv.visitVarInsn(ILOAD, vars.stateVar);
        boolean largeStateCount = isLargeStateCount();
        int labelCount = dfa.statesCount();
        if (largeStateCount) {
            labelCount = getStateGroupCount();
        }
        Label[] stateLabels = new Label[labelCount];
        for (int i = 0; i < labelCount; i++) {
            stateLabels[i] = new Label();
        }
        if (largeStateCount) {
            pushShortInt(mv, 64);
            mv.visitInsn(IDIV);
        }
        mv.visitTableSwitchInsn(0, labelCount - 1, failLabel, stateLabels);

        for (int i = 0; i < labelCount; i++) {
            mv.visitLabel(stateLabels[i]);
            int start = i;
            int end = i;
            if (largeStateCount) {
                start *= 64;
                end += 64;
            }
            stateMatch(mv, vars, start, end);
            mv.visitJumpInsn(GOTO, iterateLabel);
        }

        // handle case of failure
        mv.visitLabel(failLabel);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(IRETURN);

        // check if the dfa had a match
        mv.visitLabel(returnLabel);
        emitInvokeWasAccepted(mv, vars);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(-1,-1);
        mv.visitEnd();
    }

    /**
     * Emits bytecodes to invoke the was accepted method, reading the state variable from the local variables
     *
     * This method consumes nothing from the stack and pushes an int to the stack
     * @param mv the visitor for the current method
     * @param vars the variable indices for the current method
     */
    private void emitInvokeWasAccepted(MethodVisitor mv, MatchingVars vars) {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ILOAD, vars.stateVar);
        mv.visitMethodInsn(INVOKEVIRTUAL, className, "wasAccepted", "(I)Z", false);
    }

    /**
     * Emits bytecodes to check that we haven't reached the end of the string, jumping to the return label if we have
     *
     * This method does not modify the stack.
     *
     * @param mv the current method's visitor
     * @param vars the current method variable indices
     * @param returnLabel the label to jump to in case we've reached the end of the string
     */
    private void emitBoundsCheck(MethodVisitor mv, MatchingVars vars, Label returnLabel) {
        mv.visitVarInsn(ILOAD, vars.counterVar);
        mv.visitVarInsn(ILOAD, vars.lengthVar);
        mv.visitJumpInsn(IF_ICMPGE, returnLabel);
    }

    /**
     * Read a character from the string and increments the counter variable. Uses a local variable or field for the
     * string depending on what is available.
     *
     * Modifies the stack by consuming nothing and pushing a character.
     *
     * This method does not do a bounds check.
     * @param mv the current method visitor
     * @param vars the variable indices for the current method
     */
    private void emitReadChar(MethodVisitor mv, MatchingVars vars) {
        if (vars.stringVar < 0) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, STRING_FIELD, "Ljava/lang/String;");
        }
        else {
            mv.visitVarInsn(ALOAD, vars.stringVar);
        }
        mv.visitVarInsn(ILOAD, vars.counterVar);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "charAt", "(I)C", false);
        mv.visitIincInsn(vars.counterVar, 1);
    }

    /**
     * Emit an explicit loop searching for the first character of a pattern without entering the full state machine.
     * @param mv the current method visitor
     * @param vars the indexed variables for the current method
     * @param prefix the prefix of the pattern
     * @param postStateChangeLabel the label to jump to when a match is found
     * @param failLabel the label to jump to if we exhaust the string without finding our initial character
     */
    private void emitPrefixFindingLoop(MethodVisitor mv, MatchingVars vars, String prefix, Label postStateChangeLabel, Label failLabel) {
        Label innerIterationLabel = new Label();
        mv.visitLabel(innerIterationLabel);
        mv.visitVarInsn(ILOAD, vars.stateVar);
        mv.visitInsn(ICONST_M1);
        mv.visitJumpInsn(IF_ICMPNE, postStateChangeLabel);
        emitBoundsCheck(mv, vars, failLabel);
        emitReadChar(mv, vars);
        pushShortInt(mv, prefix.charAt(0));
        mv.visitJumpInsn(IF_ICMPNE, innerIterationLabel);
        pushShortInt(mv, dfa.getTransitions().get(0).getRight().getStateNumber());
        mv.visitVarInsn(ISTORE, vars.stateVar);
        mv.visitJumpInsn(GOTO, postStateChangeLabel);
    }

    // TODO: better name
    private void stateMatch(MethodVisitor mv, MatchingVars vars, int start, int end) {
        boolean selfTransitioning = start == end && stateMap.get(start).hasSelfTransition();
        boolean largeStateCount = start != end;

        if (selfTransitioning || largeStateCount) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ILOAD, vars.counterVar);
            mv.visitFieldInsn(PUTFIELD, className, INDEX_FIELD, "I");
        }
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ILOAD, vars.charVar);
        if (largeStateCount) {
            mv.visitVarInsn(ILOAD, vars.counterVar);
            mv.visitVarInsn(ILOAD, vars.stateVar);
            mv.visitMethodInsn(INVOKEVIRTUAL, className, "stateGroup" + (start / 64), "(CII)I", false);
        }
        else if (selfTransitioning) {
            mv.visitVarInsn(ILOAD, vars.counterVar);
            mv.visitMethodInsn(INVOKEVIRTUAL, className, "state" + start, "(CI)I", false);
        }
        else {
            mv.visitMethodInsn(INVOKEVIRTUAL, className, "state" + start, "(C)I", false);
        }
        mv.visitVarInsn(ISTORE, vars.stateVar);
        if (selfTransitioning || largeStateCount) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, INDEX_FIELD, "I");
            mv.visitVarInsn(ISTORE, vars.counterVar);
        }
    }

    private boolean isLargeStateCount() {
        return dfa.statesCount() > 64;
    }

    private int getStateGroupCount() {
        return 1 + dfa.statesCount() / 64;
    }

    private void addSingleStateAcceptedMethod(List<DFA> acceptingStates) {
        MethodVisitor mv = classWriter.visitMethod(ACC_PRIVATE, "wasAccepted", "(I)Z", null, null);
        int acceptingState = acceptingStates.get(0).getStateNumber();
        mv.visitVarInsn(ILOAD, 1);
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
        MethodVisitor mv = classWriter.visitMethod(ACC_PRIVATE, "wasAccepted", "(I)Z", null, null);
        mv.visitVarInsn(ILOAD, 1);
        Label[] labels = makeLabels(acceptingStates.size());

        int[] keys = new int[acceptingStates.size()];
        for (int i = 0; i < acceptingStates.size(); i++) {
            keys[i] = acceptingStates.get(i).getStateNumber();
        }
        Arrays.sort(keys);
        Label noMatch = new Label();
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
        MethodVisitor mv = classWriter.visitMethod(ACC_PRIVATE, "wasAccepted", "(I)Z", null, null);
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
        mv.visitVarInsn(ILOAD, 1);
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

    protected void generateTransitionMethods() {
        for (DFA target : dfa.allStates()) {
            generateTransitionMethod(target, methodDesignator(target));
        }
    }

    protected void generateTransitionMethod(DFA node, Integer transitionNumber) {
        MethodVisitor mv;
        boolean hasSelfTransition = node.hasSelfTransition();
        if (hasSelfTransition) {
            mv = this.classWriter.visitMethod(ACC_PRIVATE, "state" + transitionNumber, "(CI)I", null, null);
        }
        else {
            mv = this.classWriter.visitMethod(ACC_PRIVATE, "state" + transitionNumber, "(C)I", null, null);
        }

        MatchingVars vars = new MatchingVars(1, 2, 4, 3, -1);

        // put state in var
        if (hasSelfTransition) {
            mv.visitIntInsn(SIPUSH, transitionNumber);
        }
        else {
            mv.visitIntInsn(SIPUSH, -1);
        }
        mv.visitVarInsn(ISTORE, vars.stateVar);
        // put length in var
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, LENGTH_FIELD, "I");
        mv.visitVarInsn(ISTORE, vars.lengthVar);

        Label startLabel = new Label();
        Label iterLabel = new Label();
        Label pushStateBeforeReturnLabel = new Label();
        Label returnLabel = new Label();
        Label failLabel = new Label();

        mv.visitLabel(startLabel);
        if (!hasSelfTransition) {
            iterLabel = pushStateBeforeReturnLabel;
        }

        List<Pair<CharRange, DFA>> transitions = node.getTransitions();
        if (!transitions.isEmpty()) {
            if (transitions.size() == 1 && transitions.get(0).getLeft().isSingleCharRange()) {
                generateSingleCharTransition(node, mv, iterLabel, failLabel);
            }
            else {
                if (node.charCount() <= MAX_STATES_FOR_SWITCH) {
                    generateSwitchTransitions(node, mv, iterLabel, failLabel);
                }
                else {
                    generateTransitionJumps(node, mv, iterLabel, failLabel);
                }
            }
        }

        if (hasSelfTransition) {
            // check state and return if need be
            mv.visitLabel(iterLabel);
            mv.visitVarInsn(ILOAD, vars.stateVar);
            pushShortInt(mv, transitionNumber);
            mv.visitJumpInsn(IF_ICMPNE, pushStateBeforeReturnLabel);
            // check position and return if need be
            emitBoundsCheck(mv, vars, pushStateBeforeReturnLabel);

            // now load char var
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, STRING_FIELD, "Ljava/lang/String;");
            mv.visitVarInsn(ILOAD, vars.counterVar);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "charAt", "(I)C", false);
            mv.visitVarInsn(ISTORE, vars.charVar);
            mv.visitIincInsn(vars.counterVar, 1);
            mv.visitJumpInsn(GOTO, startLabel);
        }

        mv.visitLabel(pushStateBeforeReturnLabel);
        mv.visitVarInsn(ILOAD, vars.stateVar);
        mv.visitJumpInsn(GOTO, returnLabel);

        mv.visitLabel(failLabel);
        mv.visitInsn(ICONST_M1);
        mv.visitLabel(returnLabel);
        if (hasSelfTransition) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ILOAD, vars.counterVar);
            mv.visitFieldInsn(PUTFIELD, className, INDEX_FIELD, "I");
        }
        mv.visitInsn(IRETURN);

        mv.visitMaxs(-1, -1);
        mv.visitEnd();
    }

    private void generateSwitchTransitions(DFA node, MethodVisitor mv, Label iterLabel, Label failLabel) {
        int[] chars = getChars(node);
        Label[] labels = makeLabelsForCollection(node.getTransitions());

        mv.visitVarInsn(ILOAD, 1);

        mv.visitLookupSwitchInsn(failLabel, chars, labels);
        int index = 0;
        for (Pair<CharRange, DFA> transition : node.getTransitions()) {
            mv.visitLabel(labels[index++]);
            int nextState = methodDesignator(transition.getRight());

            pushShortInt(mv, nextState);
            mv.visitVarInsn(ISTORE, 4);
            mv.visitJumpInsn(GOTO, iterLabel);
        }
    }

    private int[] getChars(DFA node) {
        int[] chars = new int[node.charCount()];
        int i = 0;
        for (Pair<CharRange, DFA> pair : node.getTransitions()) {
            if (pair.getLeft().isSingleCharRange()) {
                chars[i++] = pair.getLeft().getStart();
            }
            else {
                CharRange range = pair.getLeft();
                int c = range.getStart();
                while (c <= range.getEnd()) {
                    chars[i++] = c++;
                }
            }
        }
        return chars;
    }

    private Label[] makeLabelsForCollection(Collection<?> collection) {
        int labelSize = collection.size();
        return makeLabels(labelSize);
    }

    private Label[] makeLabels(int labelSize) {
        Label[] labels = new Label[labelSize];
        for (int i = 0; i < labelSize; i++) {
            labels[i] = new Label();
        }
        return labels;
    }

    private void generateTransitionJumps(DFA node, MethodVisitor mv, Label returnLabel, Label failLabel) {
        Map<DFA, Label> transitionTargets = new IdentityHashMap<>();

        for (Pair<CharRange, DFA> transition : node.getTransitions()) {
            Label transitionLabel = transitionTargets.computeIfAbsent(transition.getRight(), d -> new Label());
            CharRange charRange = transition.getLeft();
            mv.visitVarInsn(ILOAD, 1);
            if (charRange.isSingleCharRange()) {
                pushCharConst(mv, charRange.getStart());
                mv.visitJumpInsn(IF_ICMPEQ, transitionLabel);
            }
            else {
                pushCharConst(mv, charRange.getStart());
                mv.visitJumpInsn(IF_ICMPLT, failLabel);

                pushCharConst(mv, charRange.getEnd());

                mv.visitIntInsn(ILOAD, 1);
                mv.visitJumpInsn(IF_ICMPGE, transitionLabel);
            }
        }
        mv.visitJumpInsn(GOTO, failLabel);
        for (Map.Entry<DFA, Label> e : transitionTargets.entrySet()) {
            mv.visitLabel(e.getValue());
            int nextState = methodDesignator(e.getKey());

            pushShortInt(mv, nextState);
            mv.visitVarInsn(ISTORE, 4);
            // pushShortInt(mv, nextState);
            mv.visitJumpInsn(GOTO, returnLabel);
        }
    }

    /**
     * Push a char constant onto the stack. Requires that the character has already been added to rangeconstants.
     * @param mv the current method visitor
     * @param c the character constant
     */
    private void pushCharConst(MethodVisitor mv, char c) {
        if ((int) c <= Short.MAX_VALUE) {
            pushShortInt(mv, (int) c);
        }
        else {
            mv.visitFieldInsn(GETSTATIC, this.className, rangeConstants.get(c), "C");
        }
    }

    private void generateSingleCharTransition(DFA node, MethodVisitor mv, Label returnLabel, Label failLabel) {
        CharRange charRange = node.getTransitions().get(0).getLeft();
        DFA next = node.getTransitions().get(0).getRight();
        mv.visitVarInsn(ILOAD, 1);
        pushCharConst(mv, charRange.getStart());
        mv.visitJumpInsn(IF_ICMPNE, failLabel);
        pushShortInt(mv, methodDesignator(next));
        mv.visitVarInsn(ISTORE, 4);
        // pushShortInt(mv, methodDesignator(next));
        mv.visitJumpInsn(GOTO, returnLabel);
    }

    /**
     * Ensure that any large unicode character values that we need to match against are stored as constants.
     */
    protected void addCharConstants() {
        AtomicInteger constCount = new AtomicInteger(0);
        dfa.allStates().stream().map(DFA::getTransitions).flatMap(List::stream).map(Pair::getLeft).forEach(charRange -> {
            if (charRange.getStart() > Short.MAX_VALUE) {
                rangeConstants.computeIfAbsent(charRange.getStart(), c -> {
                    String constName = "CHAR_CONST_" + constCount.incrementAndGet();
                    classWriter.visitField(ACC_STATIC | ACC_PRIVATE | ACC_FINAL, constName, "C", null, charRange.getStart());
                    return constName;
                });
            }
            if (charRange.getEnd() > Short.MAX_VALUE) {
                rangeConstants.computeIfAbsent(charRange.getEnd(), c -> {
                    String constName = "CHAR_CONST_" + constCount.incrementAndGet();
                    classWriter.visitField(ACC_STATIC | ACC_PRIVATE | ACC_FINAL, constName, "C", null, charRange.getEnd());
                    return constName;
                });
            }
        });
    }

    protected Integer methodDesignator(DFA right) {
        Integer stateNumber = dfaMethodMap.computeIfAbsent(right, DFA::getStateNumber);
        stateMap.put(dfaMethodMap.get(right), right);
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

    public void emitDebug(MethodVisitor mv) {
        mv.visitMethodInsn(INVOKESTATIC, "com/justinblank/strings/DFACompiler", "debug", "()V", false);
    }

    public void emitDebug(MethodVisitor mv, int i) {
        pushShortInt(mv, i);
        mv.visitMethodInsn(INVOKESTATIC, "com/justinblank/strings/DFACompiler", "debug", "(I)V", false);
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
}

