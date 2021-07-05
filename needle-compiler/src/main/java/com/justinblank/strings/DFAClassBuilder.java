package com.justinblank.strings;

import com.justinblank.strings.RegexAST.Node;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.Opcodes;

import java.util.*;

import static org.objectweb.asm.Opcodes.*;

public class DFAClassBuilder extends ClassBuilder {

    public static final String OFFSETS_ATTRIBUTE = "offsets";

    protected static final String STATE_FIELD = "state";
    protected static final String CHAR_FIELD = "c";
    protected static final String LENGTH_FIELD = "length";
    protected static final String STRING_FIELD = "string";
    protected static final String INDEX_FIELD = "index";
    protected static final String NEXT_START_FIELD = "nextStart";

    protected static final String WAS_ACCEPTED_METHOD = "wasAccepted";
    protected static final String WAS_ACCEPTED_BACKWARDS_METHOD = "wasAcceptedBackwards";
    protected static final String INDEX_FORWARDS = "indexForwards";
    protected static final String INDEX_BACKWARDS = "indexBackwards";

    static final int LARGE_STATE_COUNT = 64;

    private final DFA dfa;
    private final DFA reversed;
    private final Factorization factorization;
    private final Map<Integer, Offset> forwardOffsets;

    final List<Method> stateMethods = new ArrayList<>();
    final List<Method> backwardsStateMethods = new ArrayList<>();
    final List<Method> findMethods = new ArrayList<>();

    /**
     * @param className
     * @param superClass the superclass's descriptor
     * @param interfaces a possibly empty array of interfaces implemented
     */
    DFAClassBuilder(String className, String superClass, String[] interfaces, DFA dfa, DFA reversed,
                    Factorization factorization) {
        super(className, superClass, interfaces);
        this.dfa = dfa;
        this.reversed = reversed;
        this.factorization = factorization;
        // YOLO
        this.forwardOffsets = dfa != null ? dfa.calculateOffsets() : null;
    }

    void initMethods() {
        addStateMethods(dfa);
        if (shouldSeek()) {
            factorization.getSharedPrefix().ifPresent(prefix -> {
                if (shouldSeek()) {
                    findMethods.add(createSeekMatchMethod(prefix));
                    findMethods.add(createSeekContainedInMethod(prefix));
                }
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

    private Method createIndexMethod(boolean forwards) {
        var vars = new MatchingVars(2, 1, 3, 4, 5);
        vars.setForwards(forwards);
        vars.setWasAcceptedVar(6);
        vars.setLastMatchVar(7);
        var method = mkMethod(forwards ? INDEX_FORWARDS : INDEX_BACKWARDS, List.of("I"), "I", vars);

        var setupBlock = method.addBlock();
        var seekBlock = method.addBlock();
        var matchLoopBlock = method.addBlock();
        var returnBlock = method.addBlock();
        returnBlock.readVar(vars, MatchingVars.LAST_MATCH, "I");
        returnBlock.addReturn(IRETURN);
        var failureBlock = addFailureBlock(method, -1);

        addReadStringLength(vars, setupBlock);
        if (dfa.isAccepting()) {
            setupBlock.push(0).setVar(vars, MatchingVars.LAST_MATCH, "I");
        }
        else {
            setupBlock.push(-1).setVar(vars, MatchingVars.LAST_MATCH, "I");
        }

        if (vars.forwards) {
            addLengthCheck(vars, setupBlock, failureBlock, false);
        }

        addContainedInPrefaceBlock(vars, seekBlock, returnBlock);
        if (vars.forwards && shouldSeek()) {
            // If we consumed a prefix, then we need to save the last match
            var lastMatchBlock = method.addBlockAfter(seekBlock);
            lastMatchBlock.readThis();
            lastMatchBlock.readVar(vars, MatchingVars.STATE, "I");
            lastMatchBlock.call(forwards ? WAS_ACCEPTED_METHOD : WAS_ACCEPTED_BACKWARDS_METHOD, getClassName(), "(I)Z");
            lastMatchBlock.jump(matchLoopBlock, IFEQ);
            lastMatchBlock.readVar(vars, MatchingVars.INDEX, "I");
            lastMatchBlock.setVar(vars, MatchingVars.LAST_MATCH, "I");
        }
        fillMatchLoopBlock(vars, method, matchLoopBlock, returnBlock, seekBlock, false, true);

        return method;
    }

    private Method createFindMethod() {
        var method = mkMethod("find", List.of(), "Lcom/justinblank/strings/MatchResult;");
        var body = method.addBlock();
        body.readThis();
        body.readThis().readField(NEXT_START_FIELD, true, "I");
        body.readThis().readField(LENGTH_FIELD, true, "I");
        body.call("find", getClassName(), "(II)Lcom/justinblank/strings/MatchResult;").addReturn(ARETURN);
        return method;
    }

    private Method createFindMethodInternal() {
        var vars = new MapVars();
        vars.addVar(MatchingVars.INDEX, 1);
        vars.addVar("indexBackwards", 2);
        var method = mkMethod("find", List.of("I", "I"), "Lcom/justinblank/strings/MatchResult;", vars);
        var block = method.addBlock();
        var failureBlock = method.addBlock();
        failureBlock.callStatic("failure", "com/justinblank/strings/MatchResult", "()Lcom/justinblank/strings/MatchResult;");
        failureBlock.readThis()
                .readThis()
                .readField(LENGTH_FIELD, true, "I")
                .setField(NEXT_START_FIELD, getClassName(), "I");
        failureBlock.addReturn(ARETURN);

        block.readThis();
        block.readThis().readVar(vars, MatchingVars.INDEX, "I");
        block.call(INDEX_FORWARDS, getClassName(), "(I)I");
        block.setVar(vars, MatchingVars.INDEX, "I");
        block.readVar(vars, MatchingVars.INDEX, "I");
        block.push(-1);
        block.cmp(failureBlock, IF_ICMPEQ);
        block.readThis();
        block.readVar(vars, MatchingVars.INDEX, "I");
        block.call(INDEX_BACKWARDS, getClassName(), "(I)I");
        block.setVar(vars, "indexBackwards", "I");
        block.readVar(vars, "indexBackwards", "I");
        block.push(-1);
        block.cmp(failureBlock, IF_ICMPEQ);

        block.readThis().readVar(vars, MatchingVars.INDEX, "I").setField(NEXT_START_FIELD, getClassName(), "I");

        block.readVar(vars, "indexBackwards", "I");
        block.readVar(vars, MatchingVars.INDEX, "I");
        block.callStatic("success", "com/justinblank/strings/MatchResult", "(II)Lcom/justinblank/strings/MatchResult;");
        block.addReturn(ARETURN);

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
        var method = mkMethod(name, List.of("C", "I"),"I");
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
        MatchingVars vars = new MatchingVars(1, -1, -1, -1, -1);
        var method = mkMethod(name, arguments, "I", vars);
        if (forwards) {
            stateMethods.set(dfaState.getStateNumber(), method);
        } else {
            backwardsStateMethods.set(dfaState.getStateNumber(), method);
        }

        Block charBlock = method.addBlock();

        // So long as we only use the backwards methods to get the starting index of a found substring, checking offsets
        // would be redundant
        var offset = forwards ? forwardOffsets.get(dfaState.getStateNumber()) : null;
        if (isUsefulOffset(offset)) {
            var successBlock = method.addBlock();

            var prefailBlock = method.addBlock();
            prefailBlock.operate(POP);
            var failBlock = method.addBlock();
            failBlock.push(-1);
            failBlock.addReturn(IRETURN);

            successBlock
                    .readThis()
                    .readField(INDEX_FIELD, true, "I")
                    // -1, because we've incremented the value earlier
                    // TODO: this can end up doing an IADD with value 0
                    .push(offset.length - 1)
                    .operate(IADD)
                    .readThis()
                    .readField(LENGTH_FIELD, true, "I")
                    .jump(prefailBlock, IF_ICMPGE)
                    .readThis()
                    .readField(INDEX_FIELD, true, "I")
                    .push(offset.length - 1)
                    .operate(IADD)
                    .readThis()
                    .readField(STRING_FIELD, true, CompilerUtil.STRING_DESCRIPTOR)
                    .operate(SWAP) // GROSS
                    .call("charAt", "java/lang/String", "(I)C")
                    .setVar(vars, MatchingVars.CHAR, "C");
            if (offset.charRange.isSingleCharRange()) {
                successBlock.readVar(vars, MatchingVars.CHAR, "C");

                successBlock.push(offset.charRange.getStart())
                        .jump(prefailBlock, IF_ICMPNE);
            } else {
                successBlock.
                        readVar(vars, MatchingVars.CHAR, "C")
                        .push(offset.charRange.getStart())
                        .jump(prefailBlock, IF_ICMPLE)
                        .readVar(vars, MatchingVars.CHAR, "C")
                        .push(offset.charRange.getEnd())
                        .jump(prefailBlock, IF_ICMPGT);
            }
            successBlock.addReturn(IRETURN);
            charBlock.operations.add(CheckCharsOperation.checkChars(dfaState, failBlock, successBlock));
        } else {
            var failBlock = method.addBlock();
            failBlock.push(-1);
            failBlock.addReturn(IRETURN);
            charBlock.operations.add(CheckCharsOperation.checkChars(dfaState, failBlock, null));
        }

    }

    private boolean shouldSeek() {
        return factorization.getSharedPrefix().map(StringUtils::isNotEmpty).orElse(false);
    }

    // TODO: measure breakeven point for offsets
    static boolean isUsefulOffset(Offset offset) {
        return offset != null && offset.length > 1;
    }

    private Method createMatchesMethod() {
        var vars = new MatchingVars(1, 2, 3, 4, 5);
        var method = mkMethod("matches", new ArrayList<>(), "Z", vars);

        var setupAndSeekBlock = method.addBlock();
        var matchLoopBlock = method.addBlock();
        var returnBlock = addReturnBlock(method, vars);
        var failBlock = addFailureBlock(method, 0);

        addMatchesPrefaceBlock(vars, setupAndSeekBlock, failBlock);
        fillMatchLoopBlock(vars, method, matchLoopBlock, returnBlock, failBlock, true, true);

        return method;
    }

    // TODO: seekMatch is kinda a silly name for this
    protected Method createSeekMatchMethod(String prefix) {
        var vars = new MatchingVars(-1, 1, -1, -1, -1);
        var method = mkMethod(seekMethodName(true), List.of("I"), "I", vars);
        var body = method.addBlock();
        var failure = method.addBlock();
        prefix = getEffectivePrefix(prefix, true);
        for (int i = 0; i < prefix.length(); i++) {
            char c = prefix.charAt(i);
            body.push(c);
            body.addOperation(Operation.mkReadChar());
            body.cmp(failure, IF_ICMPNE);
            body.addOperation(Operation.mkOperation(Operation.Inst.INCREMENT_INDEX));
        }
        body.readVar(vars, MatchingVars.INDEX, "I");
        body.addReturn(IRETURN);
        failure.push(-1);
        failure.addReturn(IRETURN);
        return method;
    }

    protected Method createSeekContainedInMethod(String prefix) {
        var vars = new MatchingVars(-1, 1, -1, 2, -1);
        var method = mkMethod(seekMethodName(false), List.of("I"), "I", vars);
        var head = method.addBlock();
        var body = method.addBlock();
        var failure = method.addBlock();

        head.readThis();
        head.readField(LENGTH_FIELD, true, "I");
        head.setVar(vars, MatchingVars.LENGTH, "I");

        prefix = getEffectivePrefix(prefix, false);
        char needle = prefix.charAt(0);
        body.addOperation(Operation.checkBounds(failure));
        body.push(needle);
        body.addOperation(Operation.mkReadChar());
        body.addOperation(Operation.mkOperation(Operation.Inst.INCREMENT_INDEX));
        body.cmp(body, IF_ICMPNE);

        body.readVar(vars, MatchingVars.INDEX, "I");
        body.addReturn(IRETURN);
        failure.push(-1);
        failure.addReturn(IRETURN);
        return method;
    }

    private String getEffectivePrefix(String prefix, boolean isMatch) {
        var i = isMatch ? Math.min(prefix.length(), 8) : Math.min(prefix.length(), 1);
        return prefix.substring(0, i);
    }

    private String seekMethodName(boolean isMatch) {
        return "seek" + (isMatch ? "Match" : "ContainedIn");
    }

    void fillMatchLoopBlock(final MatchingVars vars, final Method method, Block head, final Block returnBlock,
                            final Block failTarget, final boolean isMatch, final boolean isGreedy) {
        var postCallStateBlock = method.addBlockAfter(head);
        var loopPreface = head;
        if (isMatch) {
            head.readVar(vars, MatchingVars.STATE, "I");
            head.push(-1);
            head.cmp(failTarget, IF_ICMPEQ);
        } else if (isGreedy) {
            head = method.addBlockAfter(head);
            loopPreface.push(-1);
            loopPreface.readVar(vars, MatchingVars.LAST_MATCH, "I");
            loopPreface.jump(head, IF_ICMPEQ);
            loopPreface.push(-1);
            loopPreface.readVar(vars, MatchingVars.STATE, "I");
            loopPreface.jump(head, IF_ICMPNE);
            loopPreface.push(0);
            loopPreface.setVar(vars, MatchingVars.STATE, "I");
            loopPreface.jump(returnBlock, GOTO);
        }

        // Check boundaries
        if (vars.forwards) {
            head.addOperation(Operation.checkBounds(returnBlock));
        } else {
            head.readVar(vars, MatchingVars.INDEX, "I");
            // TODO: isn't this wrong? Could be nonzero if we're searching backwards in a substring
            head.push(0);
            head.jump(returnBlock, IF_ICMPEQ);
        }

        // Increment/decrement and read character
        if (!vars.forwards) {
            head.addOperation(Operation.mkOperation(Operation.Inst.DECREMENT_INDEX));
        }
        head.addOperation(Operation.mkReadChar());
        head.setVar(vars, MatchingVars.CHAR, "C");
        if (vars.forwards) {
            head.addOperation(Operation.mkOperation(Operation.Inst.INCREMENT_INDEX));
        }

        // Call state
        head.readThis();
        head.readVar(vars.charVar, "I");
        head.readVar(vars, STATE_FIELD, "I");
        var stateOp = Operation.mkCallState(postCallStateBlock);
        stateOp.addAttribute(OFFSETS_ATTRIBUTE, forwardOffsets);
        head.addOperation(stateOp);

        // If we're doing a containedIn style match, we have to reconsider the initial state whenever we hit a failure
        // mode--if we're doing a containedIn backwards for finding the length of a match, we know we'll never hit the
        // failure state until we're done
        if (!isMatch && vars.forwards) {
            var checkForMatchInDeadState = postCallStateBlock;

            if (isGreedy) {
                postCallStateBlock = method.addBlockAfter(postCallStateBlock);
            }

            var stateResetBlock = postCallStateBlock;
            postCallStateBlock = method.addBlockAfter(postCallStateBlock);

            if (isGreedy) {
                checkForMatchInDeadState.setVar(vars, MatchingVars.STATE, "I");

                checkForMatchInDeadState.
                        push(-1)
                        .readVar(vars, MatchingVars.LAST_MATCH, "I")
                        .jump(stateResetBlock, IF_ICMPEQ);
            checkForMatchInDeadState
                    .push(-1)
                    .readVar(vars, MatchingVars.STATE, "I")
                    .jump(stateResetBlock, IF_ICMPNE)
                    .jump(returnBlock, GOTO);
            }

            if (!isGreedy) {
                stateResetBlock.setVar(vars, MatchingVars.STATE, "I");
            }
            stateResetBlock.push(-1).readVar(vars, MatchingVars.STATE, "I").jump(postCallStateBlock, IF_ICMPNE);
            stateResetBlock.push(0).setVar(vars, MatchingVars.STATE, "I");
            // We have to have index set as a field to handle an offset state
            if (forwardOffsets.containsKey(0) && isUsefulOffset(forwardOffsets.get(0))) {
                stateResetBlock
                        .readThis()
                        .readVar(vars, MatchingVars.INDEX, "I")
                        .setField(MatchingVars.INDEX, getClassName(), "I");
            }
            stateResetBlock.readThis().readVar(vars, MatchingVars.CHAR, "C");
            stateResetBlock.call("state0", getClassName(), "(C)I");
            stateResetBlock.setVar(vars, MatchingVars.STATE, "I");
            if (shouldSeek()) {
                // TODO: this block is confusing--is it even correct?
                var reseekBlock = postCallStateBlock;
                postCallStateBlock = method.addBlockAfter(postCallStateBlock);
                reseekBlock.push(-1)
                    .readVar(vars, MatchingVars.STATE, "I")
                    .jump(failTarget, IF_ICMPEQ);
            }
        }
        else {
            postCallStateBlock.setVar(vars.stateVar, "I");
        }

        if (isMatch) {
            if (isGreedy) {
                postCallStateBlock.readVar(vars.stateVar, "I");
                postCallStateBlock.push(-1);
                postCallStateBlock.cmp(failTarget, IF_ICMPEQ);
                postCallStateBlock.jump(head, GOTO);
            }
        } else {
            if (isGreedy) {
                postCallStateBlock.readThis();
                postCallStateBlock.readVar(vars, MatchingVars.STATE, "I");
                postCallStateBlock.call(vars.forwards ? WAS_ACCEPTED_METHOD : WAS_ACCEPTED_BACKWARDS_METHOD, getClassName(), "(I)Z");
                postCallStateBlock.setVar(vars, MatchingVars.WAS_ACCEPTED, "I");
                postCallStateBlock.readVar(vars, MatchingVars.WAS_ACCEPTED, "I");

                var setMatchBlock = method.addBlock();
                setMatchBlock.readVar(vars, MatchingVars.INDEX, "I");
                setMatchBlock.setVar(vars, MatchingVars.LAST_MATCH, "I");
                setMatchBlock.jump(head, GOTO);

                postCallStateBlock.jump(setMatchBlock, Opcodes.IFNE);
                postCallStateBlock.jump(loopPreface, GOTO);
            } else {
                postCallStateBlock.readThis();
                postCallStateBlock.readVar(vars, MatchingVars.STATE, "I");
                postCallStateBlock.call(vars.forwards ? WAS_ACCEPTED_METHOD : WAS_ACCEPTED_BACKWARDS_METHOD, getClassName(), "(I)Z");
                postCallStateBlock.jump(loopPreface, IFEQ);
            }
        }
    }

    protected void addMatchesPrefaceBlock(MatchingVars vars, Block initialBlock, Block failureBlock) {

        addReadStringLength(vars, initialBlock);
        addLengthCheck(vars, initialBlock, failureBlock, true);

        // Initialize variables
        initialBlock.push(0);
        initialBlock.setVar(vars, MatchingVars.INDEX, "I");
        if (shouldSeek()) {
            var prefix = factorization.getSharedPrefix().get();
            initialBlock.readThis();
            initialBlock.readVar(vars, MatchingVars.INDEX, "I");
            initialBlock.call(seekMethodName(true), getClassName(), "(I)I");
            initialBlock.setVar(vars, MatchingVars.INDEX, "I");
            initialBlock.readVar(vars, MatchingVars.INDEX, "I");
            initialBlock.push(-1);
            initialBlock.cmp(failureBlock, IF_ICMPEQ);
            int state = dfa.after(getEffectivePrefix(prefix, true)).get().getStateNumber();
            initialBlock.push(state);
            initialBlock.setVar(vars, MatchingVars.STATE, "I");
        } else {
            initialBlock.push(0);
            initialBlock.setVar(vars, MatchingVars.STATE, "I");
        }

        initialBlock.readThis();
        initialBlock.readField(DFAClassCompiler.STRING_FIELD, true, CompilerUtil.STRING_DESCRIPTOR);
        initialBlock.setVar(vars, MatchingVars.STRING, CompilerUtil.STRING_DESCRIPTOR);
    }

    protected void addContainedInPrefaceBlock(MatchingVars vars, Block initialBlock, Block failureBlock) {
        if (vars.forwards && shouldSeek()) {
            var prefix = factorization.getSharedPrefix().get();
            initialBlock.readThis();
            initialBlock.readVar(vars, MatchingVars.INDEX, "I");
            initialBlock.call(seekMethodName(false), getClassName(), "(I)I");
            initialBlock.setVar(vars, MatchingVars.INDEX, "I");
            initialBlock.readVar(vars, MatchingVars.INDEX, "I");
            initialBlock.push(-1);
            initialBlock.cmp(failureBlock, IF_ICMPEQ);
            int state = dfa.after(getEffectivePrefix(prefix, false)).get().getStateNumber();
            initialBlock.push(state);
            initialBlock.setVar(vars, MatchingVars.STATE, "I");
        } else {
            initialBlock.push(0);
            initialBlock.setVar(vars, MatchingVars.STATE, "I");
        }

        initialBlock.readThis();
        initialBlock.readField(DFAClassCompiler.STRING_FIELD, true, CompilerUtil.STRING_DESCRIPTOR);
        initialBlock.setVar(vars, MatchingVars.STRING, CompilerUtil.STRING_DESCRIPTOR);
    }

    private void addLengthCheck(MatchingVars vars, Block initialBlock, Block failureBlock, boolean isMatch) {
        if (factorization.getMinLength() > 0 && factorization.getMinLength() <= Short.MAX_VALUE) {
            initialBlock.readVar(vars, MatchingVars.LENGTH, "I");
            initialBlock.push(factorization.getMinLength());
            initialBlock.cmp(failureBlock, IF_ICMPLT);
        }
        if (isMatch) {
            factorization.getMaxLength().ifPresent(max -> {
                if (max <= Short.MAX_VALUE) {
                    initialBlock.readVar(vars, MatchingVars.LENGTH, "I");
                    initialBlock.push(max);
                    initialBlock.cmp(failureBlock, IF_ICMPGT);
                }
            });
        }
    }

    private void addReadStringLength(MatchingVars vars, Block initialBlock) {
        initialBlock.readThis();
        initialBlock.readField(LENGTH_FIELD, true, "I");
        initialBlock.setVar(vars, MatchingVars.LENGTH, "I");
    }

    Block addReturnBlock(Method method, MatchingVars vars) {
        var returnBlock = method.addBlock();
        returnBlock.readThis();
        returnBlock.readVar(vars.stateVar, "I");
        returnBlock.call(WAS_ACCEPTED_METHOD, getClassName(), "(I)Z");
        returnBlock.addReturn(IRETURN);

        return returnBlock;
    }

    Method createContainedInMethod() {
        var vars = new MatchingVars(1, 2, 3, 4, 5);
        var method = mkMethod("containedIn", new ArrayList<>(), "Z", vars);

        var setupBlock = method.addBlock();
        var seekBlock = method.addBlock();
        var matchLoopBlock = method.addBlock();
        var returnBlock = addReturnBlock(method, vars);
        var failureBlock = addFailureBlock(method, 0);

        // Initialize variables
        setupBlock.push(0);
        setupBlock.setVar(vars, MatchingVars.INDEX, "I");
        addReadStringLength(vars, setupBlock);
        addLengthCheck(vars, setupBlock, failureBlock, false);

        addContainedInPrefaceBlock(vars, seekBlock, failureBlock);
        var prefix = factorization.getSharedPrefix().orElse("");
        if (dfa.after(prefix.substring(0, Math.min(1, prefix.length()))).get().isAccepting()) {
            var wasAcceptedPostPrefixBlock = method.addBlockAfter(seekBlock);
            wasAcceptedPostPrefixBlock.readThis();
            wasAcceptedPostPrefixBlock.readVar(vars, MatchingVars.STATE, "I");
            wasAcceptedPostPrefixBlock.call(WAS_ACCEPTED_METHOD, getClassName(), "(I)Z");
            wasAcceptedPostPrefixBlock.jump(returnBlock, IFNE);
        }

        fillMatchLoopBlock(vars, method, matchLoopBlock, returnBlock, seekBlock, false, false);
        return method;
    }

    public static DFAClassBuilder build(String name, DFA dfa, Node node) {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(dfa, "dfa cannot be null");
        Objects.requireNonNull(node, "node cannot be null");

        var factorization = node.bestFactors();
        factorization.setMinLength(node.minLength());
        node.maxLength().ifPresent(factorization::setMaxLength);

        DFA dfaReversed = NFAToDFACompiler.compile(new NFA(RegexInstrBuilder.createNFA(node.reversed())));

        var builder = new DFAClassBuilder(name, "java/lang/Object", new String[]{"com/justinblank/strings/Matcher"}, dfa, dfaReversed, factorization);
        builder.initMethods();
        return builder;
    }

    public Collection<Method> allMethods() {
        return new ArrayList<>(super.allMethods());
    }

    public Block addFailureBlock(Method method, int value) {
        var failBlock = method.addBlock();
        failBlock.push(value);
        failBlock.addReturn(IRETURN);
        return failBlock;
    }
}
