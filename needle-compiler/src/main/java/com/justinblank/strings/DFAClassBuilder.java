package com.justinblank.strings;

import com.justinblank.classcompiler.*;
import com.justinblank.classcompiler.Operation;
import com.justinblank.classcompiler.lang.*;
import com.justinblank.classcompiler.lang.Void;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Opcodes;

import java.util.*;
import java.util.stream.Collectors;

import static com.justinblank.classcompiler.CompilerUtil.descriptor;
import static com.justinblank.classcompiler.lang.ArrayRead.arrayRead;
import static com.justinblank.classcompiler.lang.BinaryOperator.*;
import static com.justinblank.classcompiler.lang.CodeElement.*;
import static com.justinblank.classcompiler.lang.Literal.literal;
import static com.justinblank.classcompiler.lang.UnaryOperator.not;
import static org.objectweb.asm.Opcodes.*;

class DFAClassBuilder extends ClassBuilder {

    protected static final String STATE_FIELD = "state";
    protected static final String CHAR_FIELD = "c";
    protected static final String LENGTH_FIELD = "length";
    protected static final String STRING_FIELD = "string";
    protected static final String INDEX_FIELD = "index";
    protected static final String NEXT_START_FIELD = "nextStart";
    protected static final String BYTE_CLASSES_CONSTANT = "BYTE_CLASSES";
    protected static final String PREFIX_CONSTANT = "PREFIX";
    protected static final String INDEX_BACKWARDS = "indexBackwards";

    static final int LARGE_STATE_COUNT = 64;

    private final FindMethodSpec forwardFindMethodSpec;
    private final FindMethodSpec reversedFindMethodSpec;
    private final FindMethodSpec containedInFindMethodSpec;
    private final FindMethodSpec dfaSearchFindMethodSpec;
    private final Factorization factorization;
    private final CompilationPolicy compilationPolicy;
    private final Map<Integer, Offset> forwardOffsets;
    private final byte[] byteClasses;
    private final DebugOptions debugOptions;

    final List<Method> findMethods = new ArrayList<>();

    /**
     * @param className the simple class name of the class to be created
     */
    DFAClassBuilder(String className, DFA dfa, DFA containedInDFA, DFA reversed, DFA dfaSearch,
                    Factorization factorization, DebugOptions debugOptions) {
        super(className, "", "java/lang/Object", new String[]{"com/justinblank/strings/Matcher"});
        this.forwardFindMethodSpec = new FindMethodSpec(dfa, "", true);
        this.containedInFindMethodSpec = new FindMethodSpec(containedInDFA, "ContainedIn", true);
        this.reversedFindMethodSpec = new FindMethodSpec(reversed, "Backwards", false);
        this.dfaSearchFindMethodSpec = new FindMethodSpec(dfaSearch, "Forwards", true);
        this.factorization = factorization;
        this.compilationPolicy = new CompilationPolicy();
        this.debugOptions = debugOptions;
        this.forwardOffsets = dfa.calculateOffsets(factorization);
        compilationPolicy.stateArraysUseShorts = stateArraysUseShorts();
        if (dfa.isAllAscii()) {
            byteClasses = dfa.byteClasses();
            // TODO: remember and document the connection here
            if (factorization != null && factorization.getSharedPrefix().isEmpty()) {
                compilationPolicy.useByteClassesForAllStates = true;
            }
        } else {
            byteClasses = null;
        }
    }

    // TODO This isn't really a global policy, more specific to a particular type of find method
    @Deprecated
    private boolean stateArraysUseShorts() {
        return forwardFindMethodSpec.statesCount() > 127;
    }

    void initMethods() {
        addStateMethods();
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

        findMethods.add(createMatchesMethod(forwardFindMethodSpec));
        findMethods.add(createContainedInMethod(containedInFindMethodSpec));
        findMethods.add(createFindMethod());
        findMethods.add(createFindMethodInternal());
        findMethods.add(createIndexMethod(dfaSearchFindMethodSpec));
        findMethods.add(createIndexMethod(reversedFindMethodSpec));
        addWasAcceptedMethod(forwardFindMethodSpec);
        addWasAcceptedMethod(containedInFindMethodSpec);
        addWasAcceptedMethod(dfaSearchFindMethodSpec);
        addWasAcceptedMethod(reversedFindMethodSpec);
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
        populateByteClassArrays(forwardFindMethodSpec);
        populateByteClassArrays(reversedFindMethodSpec);
        populateByteClassArrays(containedInFindMethodSpec);
        populateByteClassArrays(dfaSearchFindMethodSpec);
    }

    private void populateByteClassArrays(FindMethodSpec spec) {
        addField(new Field(ACC_STATIC | ACC_PRIVATE | ACC_FINAL, spec.statesConstant(), "[" + compilationPolicy.getStateArrayType(), null, null));
        var staticBlock = addStaticBlock();

        // Instantiate the top level array of state transition arrays
        staticBlock.push(spec.statesCount())
                .newArray(compilationPolicy.getStateArrayType())
                .putStatic(spec.statesConstant(), true, "[" + compilationPolicy.getStateArrayType());

        // Now fill it with empty arrays
        for (var i = 0; i < spec.statesCount(); i++) {
            staticBlock.readStatic(spec.statesConstant(), true, "[" + compilationPolicy.getStateArrayType())
                    .push(i)
                    .readStatic(spec.stateArrayName(i), true, compilationPolicy.getStateArrayType())
                    .operate(AASTORE);
        }
    }

    private Method createIndexMethod(FindMethodSpec spec) {
        boolean forwards = spec.name.equals("");
        var vars = new MatchingVars(2, 1, 3, 4, 5);
        vars.setForwards(forwards);
        vars.setWasAcceptedVar(6);
        vars.setLastMatchVar(7);
        var method = mkMethod(spec.indexMethod(), List.of("I"), "I", vars);

        String wasAcceptedMethod = spec.wasAcceptedName();

        method.set(MatchingVars.LENGTH, get(MatchingVars.LENGTH, Builtin.I, thisRef()));
        if (spec.forwards) {
            if (factorization.getMinLength() > 0 && factorization.getMinLength() <= Short.MAX_VALUE) {
                method.cond(lt(read(MatchingVars.LENGTH), factorization.getMinLength())).withBody(
                        returnValue(-1)
                );
            }
        }

        method.set(MatchingVars.STRING, get(STRING_FIELD, ReferenceType.of(String.class), thisRef()));
        method.set(MatchingVars.STATE, 0);
        method.set(MatchingVars.LAST_MATCH, spec.dfa.isAccepting() ? 0 : -1);

        List<CodeElement> outerLoopBody = new ArrayList<>();

        var loopBoundary = spec.forwards ? lt(read(MatchingVars.INDEX), read(MatchingVars.LENGTH)) :
                gt(read(MatchingVars.INDEX), 0);
        method.loop(loopBoundary, outerLoopBody);

        if (spec.forwards && shouldSeek()) {
            var prefix = factorization.getSharedPrefix().orElseThrow();
            var postPrefixState = spec.dfa.after(prefix).orElseThrow(()
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
                        !spec.forwards ? set(MatchingVars.INDEX, plus(read(MatchingVars.INDEX), -1)) : new NoOpStatement(),
                        set(MatchingVars.CHAR, call("charAt", Builtin.C,
                                read(MatchingVars.STRING),
                                read(MatchingVars.INDEX))),
                        spec.forwards ? set(MatchingVars.INDEX, plus(read(MatchingVars.INDEX), 1)) : new NoOpStatement(),
                        // This check is necessary so that we don't get an array index out of bounds looking up a byteclass
                        // using non-ascii character from the haystack as our index
                        // TODO: this should be ok regardless of whether we're using byteclasses?
                        // TODO: consider an entirely separate version if we're using byteclasses
                        // current implementation seems overcomplicated
                        compilationPolicy.useByteClassesForAllStates ? cond(gt(read(MatchingVars.CHAR), literal((int) spec.dfa.maxChar()))).withBody(List.of(
                                set(MatchingVars.STATE, -1),
                                cond(gt(read(MatchingVars.LAST_MATCH), literal(-1))).withBody(
                                        returnValue(read(MatchingVars.LAST_MATCH))
                                ),
                                skip()
                        )) : new NoOpStatement(),
                        compilationPolicy.useByteClassesForAllStates ? buildStateLookup(spec) : buildStateSwitch(spec, -1),
                        cond(and(eq(-1, read(MatchingVars.STATE)), neq(-1, read(MatchingVars.LAST_MATCH)))).
                                withBody(returnValue(read(MatchingVars.LAST_MATCH))),
                        // In an indexBackwards method, we always know we're going to find a match, so don't restart on
                        // when state is -1
                        spec.forwards ? cond(eq(-1, read(MatchingVars.STATE))).withBody(
                                List.of(
                                        set(MatchingVars.STATE, 0),
                                        compilationPolicy.useByteClassesForAllStates ? buildStateLookup(spec) : buildStateSwitch(spec,0),
                                        cond(eq(-1, read(MatchingVars.STATE))).withBody(
                                                set(MatchingVars.STATE, 0)
                                        )
                                )) : new NoOpStatement(),
                        cond(call(wasAcceptedMethod, Builtin.BOOL, thisRef(), read(MatchingVars.STATE))).withBody(
                                set(MatchingVars.LAST_MATCH, read(MatchingVars.INDEX))
                        )
                ));
        outerLoopBody.add(innerLoop);

        method.returnValue(read(MatchingVars.LAST_MATCH));

        return method;
    }

    private CodeElement buildStateLookup(FindMethodSpec spec) {
        Type type = compilationPolicy.stateArraysUseShorts ? Builtin.S : Builtin.OCTET;
        var stateArrayRef = arrayRead(
                getStatic(spec.statesConstant(), ReferenceType.of(getFQCN()), ArrayType.of(ArrayType.of(type))), read(MatchingVars.STATE));
        var byteClassRef = getStatic(BYTE_CLASSES_CONSTANT, ReferenceType.of(getFQCN()), ArrayType.of(Builtin.OCTET));
        var byteClassLookup = arrayRead(byteClassRef, read(MatchingVars.CHAR));
        // TODO: this is a bit of a hack--we'll have to figure out the type inference story in mako
        return set(MatchingVars.STATE, cast(Builtin.I, arrayRead(stateArrayRef, byteClassLookup)));
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
                call(dfaSearchFindMethodSpec.indexMethod(), Builtin.I, thisRef(),
                        get(NEXT_START_FIELD, Builtin.I, thisRef())));
        method.fieldSet(get(NEXT_START_FIELD, ReferenceType.of(getClassName()), thisRef()), read(MatchingVars.INDEX));
        if (debugOptions.trackStates) {
            method.callStatic(ReferenceType.of(DFADebugUtils.class), "debugIndexForwards", Void.VOID, read(MatchingVars.INDEX));
        }

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
                                            call(reversedFindMethodSpec.indexMethod(), Builtin.I, thisRef(), read(MatchingVars.INDEX)),
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

    private void addWasAcceptedMethod(FindMethodSpec spec) {
        var accepting = new ArrayList<>(spec.dfa.acceptingStates());
        accepting.sort(Comparator.comparingInt(DFA::getStateNumber));

        String methodName = spec.wasAcceptedName();
        Method method = mkMethod(methodName, Collections.singletonList("I"), "Z", new GenericVars("state"));


        if (accepting.size() == 1) {
            method.returnValue(eq(accepting.get(0).getStateNumber(), read("state")));
        }
        // TODO: implement switch for small sets and measure impact
        // TODO: alternately, implement checking bits in a bytearray or something like that
        else {

            String setName = spec.wasAcceptedSetName();
            addSetOfAcceptingStates(accepting, setName);

            var block = method.addBlock();

            block.readStatic(setName, true, "Ljava/util/HashSet;");
            block.readVar(1, "I");
            block.callStatic("valueOf", "java/lang/Integer", "(I)Ljava/lang/Integer;");
            block.callInterface("contains", "java/util/Set", "(Ljava/lang/Object;)Z");
            block.addReturn(IRETURN);
        }
    }

    private void addSetOfAcceptingStates(ArrayList<DFA> accepting, String setName) {
        addField(new Field(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, setName, "Ljava/util/HashSet;", null, null));
        var b = addStaticBlock();
        construct(b, "java/util/HashSet");
        b.putStatic(setName, true, "Ljava/util/HashSet;");

        for (var state : accepting) {
            b.readStatic(setName, true, "Ljava/util/HashSet;");
            b.push(state.getStateNumber());
            b.callStatic("valueOf", "java/lang/Integer", "(I)Ljava/lang/Integer;");
            b.callInterface("add", "java/util/Set", "(Ljava/lang/Object;)Z");
            b.operate(Opcodes.POP);
        }
    }

    void addStateMethods() {
        addStateMethods(forwardFindMethodSpec);
        addStateMethods(containedInFindMethodSpec);
        addStateMethods(dfaSearchFindMethodSpec);
        addStateMethods(reversedFindMethodSpec);
    }

    private void addStateMethods(FindMethodSpec spec) {
        for (DFA dfaState : spec.dfa.allStates()) {
            addStateMethod(dfaState, spec);
        }

        var statesCount = spec.statesCount();
        if (statesCount > LARGE_STATE_COUNT) {
            for (var i = 0; i < statesCount; i += LARGE_STATE_COUNT) {
                addStateGroupMethod(spec, i, Math.min(i + LARGE_STATE_COUNT, statesCount));
            }
        }
    }

    private void addStateGroupMethod(FindMethodSpec spec, int start, int end) {
        String name = stateGroupName(spec, start);
        var method = mkMethod(name, List.of("C", "I"), "I", new MatchingVars(-1, -1, -1, -1, -1), ACC_PRIVATE);
        var mainBlock = method.addBlock();
        mainBlock.readThis().readVar(1, "C").readVar(2, "I");
        var switchBlocks = new ArrayList<Block>();
        for (var i = start; i < end; i++) {
            var block = method.addBlock();
            switchBlocks.add(block);
            var descriptor = "(C)I";
            block.call(stateMethodName(spec, i), getClassName(), descriptor);
        }
        var returnBlock = method.addBlock();
        returnBlock.addReturn(IRETURN);

        for (var b : switchBlocks) {
            b.jump(returnBlock, GOTO);
        }

        mainBlock.addOperation(Operation.mkTableSwitch(switchBlocks, switchBlocks.get(0), start, start + switchBlocks.size() - 1));
    }

    static String stateGroupName(FindMethodSpec spec, int start) {
        var name = "stateGroup";
        name += spec.name;
        name += (start / LARGE_STATE_COUNT);
        return name;
    }

    static String stateMethodName(FindMethodSpec spec, int state) {
        return "state" + spec.name + state;
    }

    boolean usesOffsetCalculation(int stateNumber) {
        return forwardOffsets.containsKey(stateNumber) && isUsefulOffset(forwardOffsets.get(stateNumber));
    }

    private void addStateMethod(DFA dfaState, FindMethodSpec spec) {
        String name = stateMethodName(spec, dfaState.getStateNumber());
        List<String> arguments = Arrays.asList("C"); // dfa.hasSelfTransition() ? Arrays.asList("C", "I") : Arrays.asList("C");
        Vars vars = new GenericVars("c", "byteClass", "stateTransitions", "state");
        var method = mkMethod(name, arguments, "I", vars, ACC_PRIVATE);

        if (debugOptions.trackStates) {
            method.callStatic(CompilerUtil.internalName(DFADebugUtils.class), "debugState", Void.VOID,
                    literal(dfaState.getStateNumber()), read("c"));
        }
        // TODO: offsets
        if (willUseByteClasses(dfaState)) {
            prepareMethodToUseByteClasses(spec, dfaState, method);
            compilationPolicy.usedByteClasses = true;
            String stateTransitionsName = "stateTransitions" + spec.name + dfaState.getStateNumber();

            Type stateTransitionsType = stateArraysUseShorts() ? ArrayType.of(Builtin.S) : ArrayType.of(Builtin.OCTET) ;
            Type byteClassesType = ArrayType.of(Builtin.OCTET);
            method.set("byteClass", arrayRead(getStatic(BYTE_CLASSES_CONSTANT, ReferenceType.of(getFQCN()), byteClassesType), read("c")));
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
                    var charToRecognize = transition.getLeft().getStart();
                    if (charRange.isSingleCharRange()) {
                        method.cond(eq(read("c"), (int) charToRecognize)).withBody(returnValue(transition.getRight()));
                    }
                    else {
                        method.cond(and(
                                gte(read("c"), (int) charRange.getStart()),
                                lte(read("c"), (int) charRange.getEnd()))).withBody(returnValue(transition.getRight()));
                    }
                }
                method.returnValue(-1);
            }
        }
    }

    private void prepareMethodToUseByteClasses(FindMethodSpec spec, DFA dfaState, Method method) {
        compilationPolicy.usedByteClasses = true;
        var arrayName = stateArrayName(spec, dfaState.getStateNumber());
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

    static String stateArrayName(FindMethodSpec spec, int stateNumber) {
        return "stateTransitions" + spec.name + stateNumber;
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

    private Method createMatchesMethod(FindMethodSpec spec) {
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
            offsetCheckState = spec.dfa.after(prefix).orElseThrow().getStateNumber();

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
            for (var element : createOffsetCheck(offset, 0, onFailure)) {
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
                                        call(spec.wasAcceptedName(), Builtin.BOOL, thisRef(), read(MatchingVars.STATE))))),
                set(MatchingVars.CHAR, call("charAt", Builtin.C,
                        read(MatchingVars.STRING),
                        read(MatchingVars.INDEX))),
                buildStateSwitch(spec, -1),
                set(MatchingVars.INDEX, plus(read(MatchingVars.INDEX), 1))
                ));

        if (debugOptions.trackStates) {
            method.callStatic(CompilerUtil.internalName(DFADebugUtils.class), "returnWasAccepted", Void.VOID, read(MatchingVars.STATE));
        }
        method.returnValue(
                call(spec.wasAcceptedName(), Builtin.BOOL, thisRef(), read(MatchingVars.STATE)));
        return method;
    }

    private CodeElement buildStateSwitch(FindMethodSpec spec, int onFailure) {
        boolean largeStates = spec.statesCount() > LARGE_STATE_COUNT;
        Switch stateSwitchStatement = null;
        if (largeStates) {
            stateSwitchStatement = new Switch(div(read(MatchingVars.STATE), 64));
        }
        else {
            stateSwitchStatement = new Switch(read(MatchingVars.STATE));
        }
        if (largeStates) {
            for (var i = 0; i < spec.statesCount(); i += 64) {
                stateSwitchStatement.setCase(i / 64,
                        set(MatchingVars.STATE,
                                call(stateGroupName(spec, i), Builtin.I, thisRef(), read(MatchingVars.CHAR), read(MatchingVars.STATE))));
            }
        }
        else {
            for (var i = 0; i < spec.statesCount(); i++) {
                var methodName = stateMethodName(spec, i);
                if (debugOptions.trackStates) {
                    stateSwitchStatement.setCase(i,
                            List.of(set(MatchingVars.STATE,
                                    call(methodName, Builtin.I, thisRef(), read(MatchingVars.CHAR))),
                            callStatic(DFADebugUtils.class, "debugStateTransition", Void.VOID,
                                    read(MatchingVars.STATE))));
                }
                else {
                    stateSwitchStatement.setCase(i,
                            set(MatchingVars.STATE,
                                    call(methodName, Builtin.I, thisRef(), read(MatchingVars.CHAR))));
                }
            }
        }

        stateSwitchStatement.setDefault(List.of(set(MatchingVars.STATE, onFailure)));
        if (compilationPolicy.usedByteClasses) {
            var dfaMaxChar = spec.dfa.maxChar();
            var c = cond(gt(read(MatchingVars.CHAR), (int) dfaMaxChar)).withBody(
                    set(MatchingVars.STATE, onFailure));
            c.orElse(stateSwitchStatement);
            return c;
        }
        return stateSwitchStatement;
    }

    Method createContainedInMethod(FindMethodSpec spec) {
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
            int postPrefixState = spec.dfa.after(prefix).orElseThrow().getStateNumber();

            outerLoopBody.add(set(MatchingVars.INDEX, call("indexOf", Builtin.I, read(MatchingVars.STRING),
                    getStatic(PREFIX_CONSTANT, ReferenceType.of(getClassName()), ReferenceType.of(String.class)),
                    read(MatchingVars.INDEX))));
            outerLoopBody.add(cond(eq(-1, read(MatchingVars.INDEX))).withBody(
                    returnValue(0)));
            outerLoopBody.add(set(MatchingVars.STATE, postPrefixState));

            if (usesOffsetCalculation(postPrefixState)) {
                var offset = forwardOffsets.get(postPrefixState);
                outerLoopBody.addAll(createOffsetCheck(offset, prefix.length(), List.of(
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
                                debugOptions.trackStates ?
                                        callStatic(DFADebugUtils.class, "debugCallWasAccepted", Void.VOID, read(MatchingVars.STATE))
                                        : new NoOpStatement(),
                                cond(call(spec.wasAcceptedName(), Builtin.BOOL, thisRef(), read(MatchingVars.STATE)))
                        .withBody(returnValue(1)),
                                set(MatchingVars.CHAR, call("charAt", Builtin.C,
                                read(MatchingVars.STRING),
                                read(MatchingVars.INDEX))),
                                buildStateSwitch(spec,0),
                                cond(eq(-1, read(MatchingVars.STATE))).withBody(
                                        List.of(
                                                // TODO: see if it matters that this is emitting a call to the state method
                                                // instead of lookup of next state via byteclass in cases where that's possible
                                                set(MatchingVars.STATE, call("state0", Builtin.I, thisRef(), read(MatchingVars.CHAR)))

                                )),
                                set(MatchingVars.INDEX, plus(read(MatchingVars.INDEX), 1))
                                ));
        outerLoopBody.add(innerLoop);
        if (debugOptions.trackStates) {
            method.addElement(callStatic(DFADebugUtils.class, "debugCallWasAccepted", Void.VOID, read(MatchingVars.STATE)));
        }
        method.returnValue(
                call(spec.wasAcceptedName(), Builtin.BOOL, thisRef(), read(MatchingVars.STATE)));

        return method;
    }

    private List<CodeElement> createOffsetCheck(Offset offset, int lookahead, List<CodeElement> onFailure) {

        List<CodeElement> elementsToAdd = new ArrayList<>();

        var length = offset.length;
        var targetChar = plus(read(MatchingVars.INDEX), length);
        if (lookahead > 0) {
            targetChar = plus(lookahead, targetChar);
        }
        elementsToAdd.add(set(MatchingVars.CHAR,
                call("charAt", Builtin.C, read(MatchingVars.STRING),
                        targetChar)));
        var charRange = offset.charRange;
        Expression offsetCheck;
        if (charRange.isSingleCharRange()) {
            offsetCheck = neq(read(MatchingVars.CHAR), literal((int) charRange.getStart()));
        }
        else {
            offsetCheck = or(lt(read(MatchingVars.CHAR), literal((int) charRange.getStart())),
                    gt(read(MatchingVars.CHAR), literal((int) charRange.getEnd())));
        }

        elementsToAdd.add(cond(offsetCheck).withBody(onFailure));
        return elementsToAdd;
    }

    public Collection<Method> allMethods() {
        return new ArrayList<>(super.allMethods());
    }
}
