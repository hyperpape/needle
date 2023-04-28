package com.justinblank.strings;

import com.justinblank.classcompiler.*;
import com.justinblank.classcompiler.Operation;
import com.justinblank.classcompiler.lang.*;
import com.justinblank.classcompiler.lang.Void;
import com.justinblank.strings.RegexAST.Node;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Opcodes;

import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;

import static com.justinblank.classcompiler.CompilerUtil.descriptor;
import static com.justinblank.classcompiler.lang.ArrayRead.arrayRead;
import static com.justinblank.classcompiler.lang.BinaryOperator.*;
import static com.justinblank.classcompiler.lang.CodeElement.*;
import static com.justinblank.classcompiler.lang.Literal.literal;
import static com.justinblank.classcompiler.lang.UnaryOperator.not;
import static org.objectweb.asm.Opcodes.*;

public class DFAClassBuilder extends ClassBuilder {

    static final String OFFSETS_ATTRIBUTE = "offsets";
    static final String USED_BYTECLASSES = "usedByteClasses";
    static final String STATE_NUMBER = "stateNumber";
    static final String FORWARDS = "forwards";

    protected static final String STATE_FIELD = "state";
    protected static final String CHAR_FIELD = "c";
    protected static final String LENGTH_FIELD = "length";
    protected static final String STRING_FIELD = "string";
    protected static final String INDEX_FIELD = "index";
    protected static final String NEXT_START_FIELD = "nextStart";
    protected static final String BYTE_CLASSES_CONSTANT = "BYTE_CLASSES";
    protected static final String STATES_CONSTANT = "STATES";
    protected static final String STATES_BACKWARDS_CONSTANT = "STATES_BACKWARDS";
    protected static final String PREFIX_CONSTANT = "PREFIX";
    protected static final String WAS_ACCEPTED_METHOD = "wasAccepted";
    protected static final String WAS_ACCEPTED_BACKWARDS_METHOD = "wasAcceptedBackwards";
    protected static final String INDEX_FORWARDS = "indexForwards";
    protected static final String INDEX_BACKWARDS = "indexBackwards";
    protected static final String SEEK_MATCH = "seekMatch";
    protected static final String COMPILATION_POLICY = "COMPILATION_POLICY";

    static final int LARGE_STATE_COUNT = 64;

    private final DFA dfa;
    private final DFA reversed;
    private final Factorization factorization;
    private final CompilationPolicy compilationPolicy;
    private final Map<Integer, Offset> forwardOffsets;
    private final byte[] byteClasses;
    private final DebugOptions debugOptions;

    final List<Method> stateMethods = new ArrayList<>();
    final List<Method> backwardsStateMethods = new ArrayList<>();
    final List<Method> findMethods = new ArrayList<>();

    /**
     * @param className the simple class name of the class to be created
     * @param superClass the superclass's descriptor
     * @param interfaces a possibly empty array of interfaces implemented
     */
    DFAClassBuilder(String className, String superClass, String[] interfaces, DFA dfa, DFA reversed,
                    Factorization factorization) {
        this(className, superClass, interfaces, dfa, reversed, factorization, DebugOptions.none());
    }

    /**
     * @param className  the simple class name of the class to be created
     * @param superClass the superclass's descriptor
     * @param interfaces a possibly empty array of interfaces implemented
     */
    DFAClassBuilder(String className, String superClass, String[] interfaces, DFA dfa, DFA reversed,
                    Factorization factorization, DebugOptions debugOptions) {
        super(className, "", superClass, interfaces);
        this.dfa = dfa;
        this.reversed = reversed;
        this.factorization = factorization;
        this.compilationPolicy = new CompilationPolicy();
        this.debugOptions = debugOptions;
        // YOLO
        this.forwardOffsets = dfa != null ? dfa.calculateOffsets(factorization) : null;
        if (dfa != null) {
            compilationPolicy.stateArraysUseShorts = stateArraysUseShorts();
        }
        if (dfa != null && dfa.isAllAscii()) {
            byteClasses = dfa.byteClasses();
            if (factorization != null && factorization.getSharedPrefix().isEmpty()) {
                compilationPolicy.useByteClassesForAllStates = true;
            }
        } else {
            byteClasses = null;
        }
    }

    private boolean stateArraysUseShorts() {
        return dfa.statesCount() > 127;
    }

    void initMethods() {
        addStateMethods(dfa);
        if (compilationPolicy.usedByteClasses) {
            addByteClasses();
            if (compilationPolicy.useByteClassesForAllStates) {
                populateByteClassArrays();
            }
        }
        if (shouldSeek()) {
            factorization.getSharedPrefix().ifPresent(prefix -> {
                addConstant(PREFIX_CONSTANT, CompilerUtil.STRING_DESCRIPTOR, prefix);
            });
        }

        findMethods.add(createMatchesMethod());
        findMethods.add(createContainedInMethod());
        findMethods.add(createFindMethod());
        findMethods.add(createFindMethodInternal());
        findMethods.add(createIndexMethod(true));
        findMethods.add(createIndexMethod(false));
        addWasAcceptedMethod(true);
        addWasAcceptedMethod(false);
        addConstructor();
        addFields();
    }

    /**
     * Create the array of byteClasses that maps a character <= 128 to a byteClass--this allows us to make our state
     * transition arrays smaller
     */
    private void addByteClasses() {
        addField(new Field(ACC_STATIC | ACC_PRIVATE | ACC_FINAL, BYTE_CLASSES_CONSTANT, "[B", null, null));
        var staticBlock = addStaticBlock();

        staticBlock.push(128)
                .newArray(T_BYTE)
                .putStatic(BYTE_CLASSES_CONSTANT, true, "[B")
                .readStatic(BYTE_CLASSES_CONSTANT, true, "[B")
                .push(0)
                .callStatic("fill", "java/util/Arrays", "([BB)V");
        int start = 0;
        int end = 0;
        int byteClass = -2;
        while (end < 129) {
            if (byteClass == -2) {
                byteClass = byteClasses[start];
            }
            if (byteClasses[end] != byteClass) {
                staticBlock.readStatic(BYTE_CLASSES_CONSTANT, true, "[B")
                        .push(byteClass)
                        .push(start)
                        .push(end - 1)
                        .callStatic("fillBytes", "com/justinblank/strings/ByteClassUtil", "([BBII)V");
                start = end;
                byteClass = byteClasses[end];
            }
            end++;
        }
    }

    /**
     * Create array of arrays of state transitions. Those arrays are of the form int[][]. This method does not populate
     * the underlying int[] values, it just adds them to the top level array. 
     */
    private void populateByteClassArrays() {
        var stateArrayType = compilationPolicy.getStateArrayType();
        addField(new Field(ACC_STATIC | ACC_PRIVATE | ACC_FINAL, STATES_CONSTANT, "[" + compilationPolicy.getStateArrayType(), null, null));
        addField(new Field(ACC_STATIC | ACC_PRIVATE | ACC_FINAL, STATES_BACKWARDS_CONSTANT, "[" + compilationPolicy.getStateArrayType(), null, null));
        var staticBlock = addStaticBlock();

        // Instantiate the top level array of state transition arrays
        staticBlock.push(dfa.statesCount())
                .newArray(compilationPolicy.getStateArrayType())
                .putStatic(STATES_CONSTANT, true, "[" + compilationPolicy.getStateArrayType());

        // Now fill it with empty arrays
        for (var i = 0; i < dfa.statesCount(); i++) {
            staticBlock.readStatic(STATES_CONSTANT, true, "[" + compilationPolicy.getStateArrayType())
                    .push(i)
                    .readStatic(stateArrayName(i, true), true, compilationPolicy.getStateArrayType())
                    .operate(AASTORE);
        }

        // Do the same for the reversed transition arrays
        staticBlock.push(reversed.statesCount())
                .newArray(compilationPolicy.getStateArrayType())
                .putStatic(STATES_BACKWARDS_CONSTANT, true, "[" + compilationPolicy.getStateArrayType());

        for (var i = 0; i < reversed.statesCount(); i++) {
            staticBlock.readStatic(STATES_BACKWARDS_CONSTANT, true, "[" + compilationPolicy.getStateArrayType())
                    .push(i)
                    .readStatic(stateArrayName(i, false), true, compilationPolicy.getStateArrayType())
                    .operate(AASTORE);
        }
    }

    private Method createIndexMethod(boolean forwards) {
        var vars = new MatchingVars(2, 1, 3, 4, 5);
        vars.setForwards(forwards);
        vars.setWasAcceptedVar(6);
        vars.setLastMatchVar(7);
        var method = mkMethod(forwards ? INDEX_FORWARDS : INDEX_BACKWARDS, List.of("I"), "I", vars);

        String wasAcceptedMethod = vars.forwards ? WAS_ACCEPTED_METHOD : WAS_ACCEPTED_BACKWARDS_METHOD;

        method.set(MatchingVars.LENGTH, get(MatchingVars.LENGTH, Builtin.I, thisRef()));
        if (vars.forwards) {
            if (factorization.getMinLength() > 0 && factorization.getMinLength() <= Short.MAX_VALUE) {
                method.cond(lt(read(MatchingVars.LENGTH), factorization.getMinLength())).withBody(
                        returnValue(-1)
                );
            }
        }

        method.set(MatchingVars.STRING, get(STRING_FIELD, ReferenceType.of(String.class), thisRef()));
        method.set(MatchingVars.STATE, 0);
        method.set(MatchingVars.LAST_MATCH, dfa.isAccepting() ? 0 : -1);

        List<CodeElement> outerLoopBody = new ArrayList<>();

        var loopBoundary = vars.forwards ? lt(read(MatchingVars.INDEX), read(MatchingVars.LENGTH)) :
                gt(read(MatchingVars.INDEX), 0);
        method.loop(loopBoundary, outerLoopBody);

        if (vars.forwards && shouldSeek()) {
            var prefix = factorization.getSharedPrefix().orElseThrow();
            var postPrefixState = dfa.after(prefix).orElseThrow(()
                    -> new IllegalStateException("No DFA state available after consuming prefix. This should be impossible"));
            int state = postPrefixState.getStateNumber();

            outerLoopBody.add(set(MatchingVars.INDEX, call("indexOf", Builtin.I, read(MatchingVars.STRING),
                    getStatic(PREFIX_CONSTANT, ReferenceType.of(getClassName()), ReferenceType.of(String.class)),
                    read(MatchingVars.INDEX))));
            outerLoopBody.add(cond(eq(-1, read(MatchingVars.INDEX))).withBody(
                    returnValue(-1)));
            outerLoopBody.add(set(MatchingVars.INDEX, plus(prefix.length(), read(MatchingVars.INDEX))));
            outerLoopBody.add(set(MatchingVars.STATE, state));
            if (postPrefixState.isAccepting()) {
                outerLoopBody.add(set(MatchingVars.LAST_MATCH, read(MatchingVars.INDEX)));
            }
        }
        else {
            outerLoopBody.add(set(MatchingVars.STATE, 0));
        }
        // TODO: sometimes emitting crap invocations of wasAccepted that can be statically known to be false
        // see regex ad*g
        Expression loopCondition = and(neq(-1, read(MatchingVars.STATE)), loopBoundary);
        Loop innerLoop = loop(loopCondition,
                List.of(
                        cond(call(wasAcceptedMethod, Builtin.BOOL, thisRef(), read(MatchingVars.STATE)))
                                .withBody(set(MatchingVars.LAST_MATCH, read(MatchingVars.INDEX))),
                        !vars.forwards ? set(MatchingVars.INDEX, plus(read(MatchingVars.INDEX), -1)) : new NoOpStatement(),
                        set(MatchingVars.CHAR, call("charAt", Builtin.C,
                                read(MatchingVars.STRING),
                                read(MatchingVars.INDEX))),
                        vars.forwards ? set(MatchingVars.INDEX, plus(read(MatchingVars.INDEX), 1)) : new NoOpStatement(),
                        buildStateSwitch(vars.forwards, -1),
                        cond(and(eq(-1, read(MatchingVars.STATE)), neq(-1, read(MatchingVars.LAST_MATCH)))).
                                withBody(returnValue(read(MatchingVars.LAST_MATCH))),
                        // In an indexBackwards method, we always know we're going to find a match, so don't restart on
                        // when state is -1
                        vars.forwards ? cond(eq(-1, read(MatchingVars.STATE))).withBody(
                                List.of(
                                        set(MatchingVars.STATE, 0),
                                        buildStateSwitch(vars.forwards,0)
                                )) : new NoOpStatement(),
                        cond(call(wasAcceptedMethod, Builtin.BOOL, thisRef(), read(MatchingVars.STATE))).withBody(
                                set(MatchingVars.LAST_MATCH, read(MatchingVars.INDEX))
                        )
                ));
        outerLoopBody.add(innerLoop);

        method.returnValue(read(MatchingVars.LAST_MATCH));

        return method;
    }

    private Method createFindMethod() {

        var method = mkMethod("find", List.of(), descriptor(MatchResult.class));
        method.returnValue(call("find", MatchResult.class, thisRef(),
                get(NEXT_START_FIELD, Builtin.I, thisRef()),
                get(LENGTH_FIELD, Builtin.I, thisRef())));
        return method;
    }

    private Method createFindMethodInternal() {
        var vars = new GenericVars(MatchingVars.INDEX, INDEX_BACKWARDS);
        var method = mkMethod("find", List.of("I", "I"), descriptor(MatchResult.class), vars);

        method.set(MatchingVars.INDEX,
                call(INDEX_FORWARDS, Builtin.I, thisRef(),
                        get(NEXT_START_FIELD, Builtin.I, thisRef())));
        method.fieldSet(get(NEXT_START_FIELD, ReferenceType.of(getClassName()), thisRef()), read(MatchingVars.INDEX));

        // If the string can only have one length, no need to search backwards, we can just compute the starting point
        if (factorization.getMinLength() == factorization.getMaxLength().orElse(Integer.MAX_VALUE)) {
            method.cond(neq(-1, read(MatchingVars.INDEX))).withBody(
                    List.of(
                            returnValue(
                            callStatic(MatchResult.class, "success", ReferenceType.of(MatchResult.class),
                                    sub(read(MatchingVars.INDEX), factorization.getMinLength()), read(MatchingVars.INDEX))
                    )))
                    .orElse(List.of(returnValue(callStatic(MatchResult.class, "failure",
                                            ReferenceType.of(MatchResult.class)))));
        }
        else {
            method.cond(neq(-1, read(MatchingVars.INDEX))).
                    withBody(List.of(
                            returnValue(
                                    callStatic(MatchResult.class, "success", ReferenceType.of(MatchResult.class),
                                            call(INDEX_BACKWARDS, Builtin.I, thisRef(), read(MatchingVars.INDEX)),
                                            read(MatchingVars.INDEX)))))
                    .orElse(List.of(
                            returnValue(callStatic(MatchResult.class, "failure",
                                    ReferenceType.of(MatchResult.class)))));
        }
        return method;
    }

    private void addConstructor() {
        var vars = new MatchingVars(-1, -1, -1, -1, 1);
        var method = mkMethod("<init>", Arrays.asList(CompilerUtil.STRING_DESCRIPTOR), "V", vars);

        var block = method.addBlock();
        block.readThis();
        block.call("<init>", "java/lang/Object", "()V", true); // TODO
        block.readThis();
        block.readVar(vars, MatchingVars.STRING, CompilerUtil.STRING_DESCRIPTOR);
        block.addOperation(Operation.mkSetField(MatchingVars.STRING, getClassName(), CompilerUtil.STRING_DESCRIPTOR));

        block.readThis();
        block.readVar(vars, MatchingVars.STRING, CompilerUtil.STRING_DESCRIPTOR);
        block.call("length", "java/lang/String", "()I");
        block.addOperation(Operation.mkSetField(MatchingVars.LENGTH, getClassName(), "I"));
        if (debugOptions.trackStates) {
//            block.readThis()
//                    .construct("java/util/ArrayList")
//                    .setField("visitedStates", getClassName(), CompilerUtil.descriptor(List.class));
        }
        block.addReturn(RETURN);
        addMethod(method);
    }

    private void addFields() {
        addField(new Field(ACC_PRIVATE, INDEX_FIELD, "I", null, 0));
        addField(new Field(ACC_PRIVATE, CHAR_FIELD, "C", null, null));
        addField(new Field(ACC_PRIVATE, STRING_FIELD, CompilerUtil.STRING_DESCRIPTOR, null, null));
        addField(new Field(ACC_PRIVATE, LENGTH_FIELD, "I", null, 0));
        addField(new Field(ACC_PRIVATE, STATE_FIELD, "I", null, 0));
        addField(new Field(ACC_PRIVATE, NEXT_START_FIELD, "I", null, 0));
        addField(new Field(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "CONTAINED_IN_FAILURE", "I", null, -2));
        if (debugOptions.trackStates) {
            // addField(new Field(ACC_PRIVATE, VISITED_STATES_CONSTANT, CompilerUtil.descriptor(List.class), null, null));
            addConstant("CURRENT_STATE", CompilerUtil.STRING_DESCRIPTOR, "CURRENT_STATE");
            addConstant("INDEX", CompilerUtil.STRING_DESCRIPTOR, "INDEX");
        }
    }

    private void addWasAcceptedMethod(boolean backwards) {
        var accepting = new ArrayList<>(backwards ? reversed.acceptingStates() : dfa.acceptingStates());
        accepting.sort(Comparator.comparingInt(DFA::getStateNumber));
        Method method = mkMethod(backwards ? WAS_ACCEPTED_BACKWARDS_METHOD : WAS_ACCEPTED_METHOD,
                Collections.singletonList("I"), "Z");

        var block = method.addBlock();

        if (accepting.size() == 1) {

            var successBlock = method.addBlock();
            block.readVar(1, "I");
            block.push(accepting.get(0).getStateNumber());
            var failBlock = method.addBlock();
            failBlock.push(0);
            failBlock.addReturn(IRETURN);
            block.cmp(failBlock, IF_ICMPNE);
            successBlock.push(1);
            successBlock.addReturn(IRETURN);
        }
        // TODO: implement switch for small sets and measure impact
        else {
            String name = backwards ? "ACCEPTED_SET_BACKWARDS" : "ACCEPTED_SET";
            addField(new Field(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, name, "Ljava/util/HashSet;", null, null));
            var b = addStaticBlock();
            construct(b, "java/util/HashSet");
            b.putStatic(name, true, "Ljava/util/HashSet;");

            for (var state : accepting) {
                b.readStatic(name, true, "Ljava/util/HashSet;");
                b.push(state.getStateNumber());
                b.callStatic("valueOf", "java/lang/Integer", "(I)Ljava/lang/Integer;");
                b.callInterface("add", "java/util/Set", "(Ljava/lang/Object;)Z");
                b.operate(Opcodes.POP);
            }

            block.readStatic(name, true, "Ljava/util/HashSet;");
            block.readVar(1, "I");
            block.callStatic("valueOf", "java/lang/Integer", "(I)Ljava/lang/Integer;");
            block.callInterface("contains", "java/util/Set", "(Ljava/lang/Object;)Z");
            block.addReturn(IRETURN);
        }
    }

    void addStateMethods(DFA dfa) {
        // Warning to future me: I find these next few lines annoying, and I wanted to refactor it to make these
        // Method[] instead of lists. That then made a test stop compiling, in a way that was probably possible to fix,
        // but annoying.
        for (int i = 0; i < dfa.statesCount(); i++) {
            stateMethods.add(null);
        }
        for (var i = 0; i < reversed.statesCount(); i++) {
            backwardsStateMethods.add(null);
        }
        for (DFA dfaState : dfa.allStates()) {
            addStateMethod(dfaState, true);
        }
        for (DFA dfaState : reversed.allStates()) {
            addStateMethod(dfaState, false);
        }
        var statesCount = dfa.statesCount();
        if (statesCount > LARGE_STATE_COUNT) {
            for (var i = 0; i < statesCount; i += LARGE_STATE_COUNT) {
                addStateGroupMethod(i, Math.min(i + LARGE_STATE_COUNT, statesCount), true);
            }
        }
        var reversedStateCount = reversed.statesCount();
        if (reversedStateCount > LARGE_STATE_COUNT) {
            for (var i = 0; i < reversedStateCount; i += LARGE_STATE_COUNT) {
                addStateGroupMethod(i, Math.min(i + LARGE_STATE_COUNT, reversedStateCount), false);
            }
        }
    }

    private void addStateGroupMethod(int start, int end, boolean forwards) {
        String name = stateGroupName(start, forwards);
        var method = mkMethod(name, List.of("C", "I"), "I", new MatchingVars(-1, -1, -1, -1, -1), ACC_PRIVATE);
        var mainBlock = method.addBlock();
        mainBlock.readThis().readVar(1, "C").readVar(2, "I");
        var switchBlocks = new ArrayList<Block>();
        for (var i = start; i < end; i++) {
            var block = method.addBlock();
            switchBlocks.add(block);
            var descriptor = "(C)I";
            block.call(stateMethodName(i, forwards), getClassName(), descriptor);
        }
        var returnBlock = method.addBlock();
        returnBlock.addReturn(IRETURN);

        for (var b : switchBlocks) {
            b.jump(returnBlock, GOTO);
        }

        mainBlock.addOperation(Operation.mkTableSwitch(switchBlocks, switchBlocks.get(0), start, start + switchBlocks.size() - 1));
    }

    static String stateGroupName(int start, boolean forwards) {
        var name = "stateGroup";
        if (!forwards) {
            name += "Backwards";
        }
        name += (start / LARGE_STATE_COUNT);
        return name;
    }

    static String stateMethodName(int state, boolean forwards) {
        return "state" + (forwards ? "" : "Backwards") + state;
    }

    boolean usesOffsetCalculation(int stateNumber) {
        return forwardOffsets.containsKey(stateNumber) && isUsefulOffset(forwardOffsets.get(stateNumber));
    }

    private void addStateMethod(DFA dfaState, boolean forwards) {
        String name = stateMethodName(dfaState.getStateNumber(), forwards);
        List<String> arguments = Arrays.asList("C"); // dfa.hasSelfTransition() ? Arrays.asList("C", "I") : Arrays.asList("C");
        Vars vars = new GenericVars("c", "byteClass", "stateTransitions", "state");
        var method = mkMethod(name, arguments, "I", vars, ACC_PRIVATE);
        // TODO: these methods shouldn't be necessary when the rebuild on top of mako is done
        method.setAttribute(FORWARDS, forwards);
        method.setAttribute(COMPILATION_POLICY, compilationPolicy);
        if (forwards) {
            stateMethods.set(dfaState.getStateNumber(), method);
        } else {
            backwardsStateMethods.set(dfaState.getStateNumber(), method);
        }


        if (debugOptions.trackStates) {
            method.callStatic(CompilerUtil.internalName(DFADebugUtils.class), "debugState", Void.VOID,
                    literal(dfaState.getStateNumber()), read("c"));
        }
        // TODO: offsets
        if (willUseByteClasses(dfaState)) {
            prepareMethodToUseByteClasses(dfaState, forwards, method);
            compilationPolicy.usedByteClasses = true;
            String stateTransitionsName = "stateTransitions" + (forwards ? "" : "Backwards") + dfaState.getStateNumber();

            Type stateTransitionsType = stateArraysUseShorts() ? ArrayType.of(Builtin.S) : ArrayType.of(Builtin.OCTET) ;
            Type byteClassesType = ArrayType.of(Builtin.OCTET);
            // TODO: this type is never going to have a package for it
            method.set("byteClass", arrayRead(getStatic(BYTE_CLASSES_CONSTANT, ReferenceType.of(getClassName()), byteClassesType), read("c")));
            method.set("stateTransitions", getStatic(stateTransitionsName, ReferenceType.of(getClassName()), stateTransitionsType));
            method.returnValue(arrayRead(read("stateTransitions"), read("byteClass")));
        }
        else {

            List<Pair<CharRange, Integer>> transitions = dfaState.getTransitions().
                    stream().
                    map(t -> Pair.of(t.getLeft(), t.getRight().getStateNumber())).
                    collect(Collectors.toList());
            if (transitions.size() == 1 && transitions.get(0).getLeft().isSingleCharRange()) {
                var transition = transitions.get(0);
                var charToRecognize = transition.getLeft().getStart();
                method.cond(eq(read("c"), (int) charToRecognize)).withBody(returnValue(transition.getRight()))
                        .orElse(returnValue(-1));
            }
            else {
                for (var transition : transitions) {
                    var charRange = transition.getLeft();
                    method.cond(and(
                            gte(read("c"), (int) charRange.getStart()),
                            lte(read("c"), (int) charRange.getEnd()))).withBody(returnValue(transition.getRight()));
                }
                method.returnValue(-1);
            }
        }
    }

    private void prepareMethodToUseByteClasses(DFA dfaState, boolean forwards, Method method) {
        compilationPolicy.usedByteClasses = true;
        method.setAttribute(USED_BYTECLASSES, true);
        method.setAttribute(STATE_NUMBER, dfaState.getStateNumber());
        method.setAttribute(FORWARDS, forwards);
        var arrayName = stateArrayName(dfaState.getStateNumber(), forwards);
        addField(new Field(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, arrayName, compilationPolicy.getStateArrayType(), null, null));
        var staticBlock = addStaticBlock();

        staticBlock.push(getByteClassesMax() + 1);
        if (compilationPolicy.stateArraysUseShorts) {
            staticBlock.newArray(T_SHORT);
        }
        else {
            staticBlock.newArray(T_BYTE);
        }
        staticBlock.putStatic(arrayName, true, compilationPolicy.getStateArrayType());

        staticBlock.readStatic(arrayName, true, compilationPolicy.getStateArrayType())
                .push(-1);
        if (compilationPolicy.stateArraysUseShorts) {
            staticBlock.callStatic("fill", "java/util/Arrays", "([SS)V");
        }
        else {
            staticBlock.callStatic("fill", "java/util/Arrays", "([BB)V");
        }

        for (var transition : dfaState.getTransitions()) {
            var largestSeenByteClass = 0;
            for (var i = transition.getLeft().getStart(); i <= transition.getLeft().getEnd(); i++) {
                var byteClass = byteClasses[i];
                if (byteClass > largestSeenByteClass) {
                    var state = transition.getRight().getStateNumber();
                    staticBlock.readStatic(arrayName, true, compilationPolicy.getStateArrayType())
                            .push(byteClass)
                            .push(state);
                    if (compilationPolicy.stateArraysUseShorts) {
                        staticBlock.operate(SASTORE);
                    } else {
                        staticBlock.operate(BASTORE);
                    }
                    largestSeenByteClass = byteClass;
                }
            }
        }
    }

    private int getByteClassesMax() {
        var byteClassesMax = 0;
        for (var b : byteClasses) {
            if (b > byteClassesMax) {
                byteClassesMax = b;
            }
        }
        return byteClassesMax;
    }

    static String stateArrayName(int stateNumber, boolean forwards) {
        return "stateTransitions" + (forwards ? "" : "Backwards") + stateNumber;
    }

    private boolean willUseByteClasses(DFA dfaState) {
        if (byteClasses == null) {
            return false;
        }
        return true; // dfaState.getTransitions().size() > 3 || !dfa.allTransitionsLeadToSameState();
    }

    private boolean shouldSeek() {
        return factorization.getSharedPrefix().map(StringUtils::isNotEmpty).orElse(false);
    }

    // TODO: measure breakeven point for offsets
    static boolean isUsefulOffset(Offset offset) {
        return offset != null && offset.length > 3;
    }

    private Method createMatchesMethod() {
        var vars = new MatchingVars(1, 2, 3, 4, 5);
        var method = mkMethod("matches", new ArrayList<>(), "Z", vars);

        method.set(MatchingVars.LENGTH,
                call("length", Builtin.I,
                        get(STRING_FIELD, ReferenceType.of(String.class), thisRef())));
        if (factorization.getMinLength() > 0 || factorization.getMaxLength().isPresent()) {
            Expression expression = null;
            if (factorization.getMinLength() > 0) {
                expression = gt(factorization.getMinLength(), read(MatchingVars.LENGTH));
            }
            if (factorization.getMaxLength().isPresent()) {
                var secondExpression = gt(read(LENGTH_FIELD), factorization.getMaxLength().get());

                if (expression == null) {
                    expression = secondExpression;
                }
                else {
                    expression = or(expression, secondExpression);
                }
            }
            method.cond(expression).withBody(returnValue(0));
        }
        method.set(MatchingVars.STRING, get(STRING_FIELD, ReferenceType.of(String.class), thisRef()));

        int offsetCheckState = 0;
        if (shouldSeek()) {
            var prefix = factorization.getSharedPrefix().orElseThrow();
            offsetCheckState = dfa.after(prefix).orElseThrow().getStateNumber();

            method.cond(not(call("startsWith", Builtin.BOOL, read(MatchingVars.STRING),
                            getStatic(PREFIX_CONSTANT, ReferenceType.of(getClassName()), ReferenceType.of(String.class)))))
                            .withBody(returnValue(0));
            method.set(MatchingVars.INDEX, prefix.length());
            method.set(MatchingVars.STATE, offsetCheckState);
        }
        else {
            method.set(MatchingVars.INDEX, 0);
            method.set(MatchingVars.STATE, 0);
        }
        if (usesOffsetCalculation(offsetCheckState)) {
            var offset = forwardOffsets.get(offsetCheckState);
            List<CodeElement> onFailure;
            if (debugOptions.trackStates) {
                onFailure = List.of(callStatic(DFADebugUtils.class, "failedLookAheadCheck",
                                Void.VOID, read(MatchingVars.INDEX), plus(read(MatchingVars.INDEX), offset.length)),
                        returnValue(0));
            }
            else {
                 onFailure = List.of(returnValue(0));
            }
            for (var element : createOffsetCheck(offset, onFailure)) {
                method.addElement(element);
            }
        }

        method.loop(neq(read(MatchingVars.STATE), -1), List.of(
                cond(eq(read(MatchingVars.INDEX),
                        read(MatchingVars.LENGTH)))
                        .withBody(List.of(debugOptions.trackStates ?
                                        callStatic(DFADebugUtils.class, "returnWasAccepted", Void.VOID, read(MatchingVars.STATE)) :
                                        new NoOpStatement(),
                                returnValue(
                                        call(WAS_ACCEPTED_METHOD, Builtin.BOOL, thisRef(), read(MatchingVars.STATE))))),
                set(MatchingVars.CHAR, call("charAt", Builtin.C,
                        read(MatchingVars.STRING),
                        read(MatchingVars.INDEX))),
                buildStateSwitch(true, -1),
                set(MatchingVars.INDEX, plus(read(MatchingVars.INDEX), 1))
                ));

        if (debugOptions.trackStates) {
            method.callStatic(CompilerUtil.internalName(DFADebugUtils.class), "returnWasAccepted", Void.VOID, read(MatchingVars.STATE));
        }
        method.returnValue(
                call(WAS_ACCEPTED_METHOD, Builtin.BOOL, thisRef(), read(MatchingVars.STATE)));
        return method;
    }

    private CodeElement buildStateSwitch(boolean forwards, int onFailure) {
        boolean largeStates = dfa.statesCount() > LARGE_STATE_COUNT;
        Switch stateSwitchStatement = null;
        if (largeStates) {
            stateSwitchStatement = new Switch(div(read(MatchingVars.STATE), 64));
        }
        else {
            stateSwitchStatement = new Switch(read(MatchingVars.STATE));
        }
        if (largeStates) {
            for (var i = 0; i < dfa.statesCount(); i += 64) {
                stateSwitchStatement.setCase(i / 64,
                        set(MatchingVars.STATE,
                                call(stateGroupName(i, forwards), Builtin.I, thisRef(), read(MatchingVars.CHAR), read(MatchingVars.STATE))));
            }
        }
        else {
            for (var i = 0; i < dfa.statesCount(); i++) {
                var methodName = stateMethodName(i, forwards);
                stateSwitchStatement.setCase(i,
                        set(MatchingVars.STATE,
                                call(methodName, Builtin.I, thisRef(), read(MatchingVars.CHAR))));
            }
        }

        stateSwitchStatement.setDefault(List.of(set(MatchingVars.STATE, onFailure)));
        if (compilationPolicy.usedByteClasses) {
            var dfaMaxChar = dfa.maxChar();
            var c = cond(gt(read(MatchingVars.CHAR), (int) dfaMaxChar)).withBody(
                    set(MatchingVars.STATE, onFailure));
            c.orElse(stateSwitchStatement);
            return c;
        }
        return stateSwitchStatement;
    }

    Method createContainedInMethod() {
        var vars = new MatchingVars(1, 2, 3, 4, 5);
        var method = mkMethod("containedIn", new ArrayList<>(), "Z", vars);

        method.set(MatchingVars.LENGTH,
                call("length", Builtin.I,
                        get(STRING_FIELD, ReferenceType.of(String.class), thisRef())));
        if (factorization.getMinLength() > 0) {
            Expression expression = gt(factorization.getMinLength(), read(MatchingVars.LENGTH));
            method.cond(expression).withBody(returnValue(0));
        }
        method.set(MatchingVars.STRING, get(STRING_FIELD, ReferenceType.of(String.class), thisRef()));

        method.set(MatchingVars.INDEX, 0);
        method.set(MatchingVars.STATE, 0);

        List<CodeElement> outerLoopBody = new ArrayList<>();
        method.loop(lt(read(MatchingVars.INDEX), read(MatchingVars.LENGTH)), outerLoopBody);

        if (shouldSeek()) {
            var prefix = factorization.getSharedPrefix().orElseThrow();
            int postPrefixState = dfa.after(prefix).orElseThrow().getStateNumber();

            outerLoopBody.add(set(MatchingVars.INDEX, call("indexOf", Builtin.I, read(MatchingVars.STRING),
                    getStatic(PREFIX_CONSTANT, ReferenceType.of(getClassName()), ReferenceType.of(String.class)),
                    read(MatchingVars.INDEX))));
            outerLoopBody.add(cond(eq(-1, read(MatchingVars.INDEX))).withBody(
                    returnValue(0)));

            if (usesOffsetCalculation(postPrefixState)) {
                var offset = forwardOffsets.get(postPrefixState);
                outerLoopBody.addAll(createOffsetCheck(offset, List.of(
                        set(MatchingVars.INDEX, plus(read(MatchingVars.INDEX), 1)),
                        set(MatchingVars.STATE, -1))));
            }
            else {
                outerLoopBody.add(set(MatchingVars.STATE, postPrefixState));
            }
            outerLoopBody.add(set(MatchingVars.INDEX, plus(prefix.length(), read(MatchingVars.INDEX))));
        }
        else {
            outerLoopBody.add(set(MatchingVars.STATE, 0));
        }
        // TODO: sometimes emitting crap invocations of wasAccepted that can be statically known to be false
        // see regex ad*g
        Loop innerLoop = loop(and(
                lt(read(MatchingVars.INDEX), read(MatchingVars.LENGTH)),
                        neq(-1, read(MatchingVars.STATE))),
                        List.of(
                                cond(call(WAS_ACCEPTED_METHOD, Builtin.BOOL, thisRef(), read(MatchingVars.STATE)))
                        .withBody(returnValue(1)),
                                set(MatchingVars.CHAR, call("charAt", Builtin.C,
                                read(MatchingVars.STRING),
                                read(MatchingVars.INDEX))),
                                buildStateSwitch(true,0),
                                cond(eq(-1, read(MatchingVars.STATE))).withBody(
                                        List.of(
                                                set(MatchingVars.STATE, 0),
                                                buildStateSwitch(true,-1)
                                )),
                                set(MatchingVars.INDEX, plus(read(MatchingVars.INDEX), 1))
                                ));
        outerLoopBody.add(innerLoop);
        method.returnValue(
                call(WAS_ACCEPTED_METHOD, Builtin.BOOL, thisRef(), read(MatchingVars.STATE)));

        return method;
    }

    public static DFAClassBuilder build(String name, DFA dfa, Node node) {
        return build(name, dfa, node, DebugOptions.none());
    }

    private List<CodeElement> createOffsetCheck(Offset offset, List<CodeElement> onFailure) {

        List<CodeElement> elementsToAdd = new ArrayList<>();

        var length = offset.length;
        elementsToAdd.add(set(MatchingVars.CHAR,
                call("charAt", Builtin.C, read(MatchingVars.STRING),
                        plus(read(MatchingVars.INDEX), length))));
        var charRange = offset.charRange;
        Expression offsetCheck;
        if (charRange.isSingleCharRange()) {
            offsetCheck = neq(read(MatchingVars.CHAR), literal((int) charRange.getStart()));
        }
        else {
            offsetCheck = or(lt(read(MatchingVars.CHAR), literal((int) charRange.getStart())),
                    gt(read(MatchingVars.CHAR), literal((int) charRange.getEnd())));
        }

        elementsToAdd.add(cond(not(offsetCheck)).withBody(onFailure));
        return elementsToAdd;
    }

    public static DFAClassBuilder build(String name, DFA dfa, Node node, DebugOptions debugOptions) {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(dfa, "dfa cannot be null");
        Objects.requireNonNull(node, "node cannot be null");

        var factorization = node.bestFactors();
        factorization.setMinLength(node.minLength());
        node.maxLength().ifPresent(factorization::setMaxLength);
        DFA dfaReversed = NFAToDFACompiler.compile(new NFA(RegexInstrBuilder.createNFA(node.reversed())));

        var builder = new DFAClassBuilder(name, "java/lang/Object", new String[]{"com/justinblank/strings/Matcher"}, dfa, dfaReversed, factorization, debugOptions);
        builder.initMethods();
        return builder;
    }

    public Collection<Method> allMethods() {
        return new ArrayList<>(super.allMethods());
    }
}
