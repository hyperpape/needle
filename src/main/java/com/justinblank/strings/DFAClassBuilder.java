package com.justinblank.strings;

import com.justinblank.strings.RegexAST.Node;
import org.objectweb.asm.Opcodes;

import java.util.*;

import static org.objectweb.asm.Opcodes.*;

public class DFAClassBuilder extends ClassBuilder {

    protected static final String STATE_FIELD = "state";
    protected static final String CHAR_FIELD = "c";
    protected static final String LENGTH_FIELD = "length";
    protected static final String STRING_FIELD = "string";
    protected static final String INDEX_FIELD = "index";

    protected static final String WAS_ACCEPTED_METHOD = "wasAccepted";
    protected static final String WAS_ACCEPTED_BACKWARDS_METHOD = "wasAcceptedBackwards";
    protected static final String INDEX_FORWARDS = "indexForwards";
    protected static final String INDEX_BACKWARDS = "indexBackwards";

    private final DFA dfa;
    private final DFA reversed;
    private final Factorization factorization;

    final List<Method> stateMethods = new ArrayList<>();
    final List<Method> backwardsStateMethods = new ArrayList<>();
    final List<Method> findMethods = new ArrayList<>();

    /**
     *
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
    }

    void initMethods() {
        addStateMethods(dfa);
        factorization.getSharedPrefix().ifPresent(prefix -> {
            findMethods.add(createSeekMatchMethod(prefix));
            findMethods.add(createSeekContainedInMethod(prefix));
        });

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

        var ultraInitialBlock = method.addBlock();
        var initialBlock = method.addBlock();
        var matchLoopBlock = method.addBlock();
        var returnBlock = method.addBlock();
        returnBlock.readVar(vars, MatchingVars.LAST_MATCH, "I");
        returnBlock.addReturn(IRETURN);
        var failureBlock = addFailureBlock(method, -1);

        addReadStringLength(vars, ultraInitialBlock);
        if (vars.forwards) {
            addLengthCheck(vars, ultraInitialBlock, failureBlock, false);
        }

        addContainedInPrefaceBlock(vars, initialBlock, failureBlock);
        if (dfa.isAccepting()) {
            initialBlock.push(0);
            initialBlock.setVar(vars, MatchingVars.LAST_MATCH, "I");
        }
        else {
            initialBlock.push(-1);
            initialBlock.setVar(vars, MatchingVars.LAST_MATCH, "I");
        }
        if (vars.forwards && factorization.getSharedPrefix().isPresent()) {
            // If we consumed a prefix, then we need to save the last match
            var lastMatchBlock = method.addBlockAfter(initialBlock);
            lastMatchBlock.readThis();
            lastMatchBlock.readVar(vars, MatchingVars.STATE, "I");
            lastMatchBlock.call(WAS_ACCEPTED_METHOD,getClassName(),"(I)Z");
            lastMatchBlock.jump(matchLoopBlock,IFEQ);
            lastMatchBlock.readVar(vars, MatchingVars.INDEX, "I");
            lastMatchBlock.setVar(vars,MatchingVars.LAST_MATCH,"I");
        }
        fillMatchLoopBlock(vars, method, matchLoopBlock, returnBlock, initialBlock, false, true);

        return method;
    }

    private Method createFindMethod() {
        var method = mkMethod("find", List.of(), "Lcom/justinblank/strings/MatchResult;");
        var body = method.addBlock();
        body.readThis().push(0);
        body.readThis().readField(LENGTH_FIELD, true, "I");
        body.call("find", getClassName(), "(II)Lcom/justinblank/strings/MatchResult;").addReturn(ARETURN);
        return method;
    }

    private Method createFindMethodInternal() {
        var vars = new MatchingVars(-1, 1, -1, -1, -1);
        var method = mkMethod("find", List.of("I", "I"), "Lcom/justinblank/strings/MatchResult;", vars);
        var block = method.addBlock();
        var failureBlock = method.addBlock();
        failureBlock.callStatic("failure","com/justinblank/strings/MatchResult","()Lcom/justinblank/strings/MatchResult;");
        failureBlock.addReturn(ARETURN);

        block.readThis();
        block.push(0);
        block.call(INDEX_FORWARDS,getClassName(),"(I)I");
        block.setVar(1,"I");
        block.readVar(1,"I");
        block.push(-1);
        block.cmp(failureBlock, IF_ICMPEQ);
        block.readThis();
        block.readVar(1,"I");
        // these should be unnecessary
//        block.addOperation(Operation.pushValue(-1));
//        block.addOperation(Operation.mkOperation(Operation.Inst.ADD));
        block.call(INDEX_BACKWARDS,getClassName(),"(I)I");
        block.setVar(2,"I");
        block.readVar(2,"I");
        block.push(-1);
        block.cmp(failureBlock, IF_ICMPEQ);

        block.readVar(2,"I");
        block.readVar(1,"I");
        block.callStatic("success","com/justinblank/strings/MatchResult","(II)Lcom/justinblank/strings/MatchResult;");
        block.addReturn(ARETURN);

        return method;
    }

    private void addConstructor() {
        var vars = new MatchingVars(-1, -1, -1, -1, 1);
        var method = mkMethod("<init>", Arrays.asList(CompilerUtil.STRING_DESCRIPTOR), "V", vars);

        var block = method.addBlock();
        block.readThis();
        block.call("<init>","java/lang/Object","()V", true); // TODO
        block.readThis();
        block.readVar(vars, MatchingVars.STRING, CompilerUtil.STRING_DESCRIPTOR);
        block.addOperation(Operation.mkSetField(MatchingVars.STRING, getClassName(), CompilerUtil.STRING_DESCRIPTOR));

        block.readThis();
        block.readVar(vars, MatchingVars.STRING, CompilerUtil.STRING_DESCRIPTOR);
        block.call("length","java/lang/String","()I");
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
            block.readVar(1,"I");
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
                b.readStatic(name,true,"Ljava/util/HashSet;");
                b.push(state.getStateNumber());
                b.callStatic("valueOf","java/lang/Integer","(I)Ljava/lang/Integer;");
                b.callInterface("add","java/util/Set","(Ljava/lang/Object;)Z");
                b.operate(Opcodes.POP);
            }

            block.readStatic(name,true,"Ljava/util/HashSet;");
            block.readVar(1,"I");
            block.callStatic("valueOf","java/lang/Integer","(I)Ljava/lang/Integer;");
            block.callInterface("contains","java/util/Set","(Ljava/lang/Object;)Z");
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

    }

    private void addStateMethod(DFA dfaState, boolean forwards) {
        String name = "state" + (forwards ? "" : "Backwards") + dfaState.getStateNumber();
        List<String> arguments = Arrays.asList("C"); // dfa.hasSelfTransition() ? Arrays.asList("C", "I") : Arrays.asList("C");
        MatchingVars vars = new MatchingVars(1, -1, -1, -1, -1);
        var method = mkMethod(name, arguments, "I", vars);
        if (forwards) {
            stateMethods.set(dfaState.getStateNumber(), method);
        }
        else {
            backwardsStateMethods.set(dfaState.getStateNumber(), method);
        }

        Block charBlock = method.addBlock();

        var successBlock = method.addBlock();
        successBlock.addReturn(IRETURN);

        var failBlock = method.addBlock();
        failBlock.push(-1);
        failBlock.addReturn(IRETURN);
        charBlock.operations.add(Operation.checkChars(dfaState, failBlock));
    }

    private Method createMatchesMethod() {
        var vars = new MatchingVars(1, 2, 3, 4, 5);
        var method = mkMethod("matches", new ArrayList<>(), "Z", vars);

        var initialBlock = method.addBlock();
        var matchLoopBlock = method.addBlock();
        var returnBlock = addReturnBlock(method, vars);
        var failBlock = addFailureBlock(method, 0);

        addMatchesPrefaceBlock(vars, initialBlock, failBlock);
        fillMatchLoopBlock(vars, method, matchLoopBlock, returnBlock, failBlock, true, true);

        return method;
    }

    // TODO: seekMatch is kinda a silly name for this
    protected Method createSeekMatchMethod(String prefix) {
        var vars = new MatchingVars(-1, 1, -1, -1,-1);
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
        var vars = new MatchingVars(-1, 1, -1, 2,-1);
        var method = mkMethod(seekMethodName(false), List.of("I"), "I", vars);
        var head = method.addBlock();
        var body = method.addBlock();
        var failure = method.addBlock();

        head.readThis();
        head.readField(LENGTH_FIELD,true, "I");
        head.setVar(vars,MatchingVars.LENGTH,"I");

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

    void fillMatchLoopBlock(MatchingVars vars, Method method, Block head, Block returnBlock, Block failTarget,
                            boolean isMatch, boolean isGreedy) {
        var tail = method.addBlockAfter(head);
        var loopPreface = head;
        if (isMatch) {
            head.readVar(vars, MatchingVars.STATE, "I");
            head.push(-1);
            head.cmp(failTarget, IF_ICMPEQ);
        }
        else if (isGreedy) {
            head = method.addBlockAfter(head);
            loopPreface.push(-1);
            loopPreface.readVar(vars, MatchingVars.LAST_MATCH, "I");
            loopPreface.jump(head,IF_ICMPEQ);
            loopPreface.push(-1);
            loopPreface.readVar(vars, MatchingVars.STATE, "I");
            loopPreface.jump(head,IF_ICMPNE);
            loopPreface.jump(returnBlock,GOTO);
        }

        if (vars.forwards) {
            head.addOperation(Operation.checkBounds(returnBlock));
        }
        else {
            head.readVar(vars, MatchingVars.INDEX, "I");
            head.push(0);
            head.jump(returnBlock,IF_ICMPEQ);
        }


        if (!vars.forwards) {
            head.addOperation(Operation.mkOperation(Operation.Inst.DECREMENT_INDEX));
        }
        head.addOperation(Operation.mkReadChar());
        head.setVar(vars,MatchingVars.CHAR,"C");

        if (vars.forwards) {
            head.addOperation(Operation.mkOperation(Operation.Inst.INCREMENT_INDEX));
        }
        head.readThis();
        head.readVar(vars.charVar,"I");
        head.readVar(vars, STATE_FIELD, "I");
        head.addOperation(Operation.mkCallState(tail));

        tail.setVar(vars.stateVar,"I");
        if (isMatch) {
            if (isGreedy) {
                tail.readVar(vars.stateVar,"I");
                tail.push(-1);
                tail.cmp(failTarget, IF_ICMPEQ);
                tail.jump(head,GOTO);
            }
        }
        else {
            if (isGreedy) {
                tail.readThis();
                tail.readVar(vars, MatchingVars.STATE, "I");
                tail.call(WAS_ACCEPTED_METHOD,getClassName(),"(I)Z");
                tail.setVar(vars,MatchingVars.WAS_ACCEPTED,"I");
                tail.readVar(vars, MatchingVars.WAS_ACCEPTED, "I");

                var setMatchBlock = method.addBlock();
                setMatchBlock.readVar(vars, MatchingVars.INDEX, "I");
                setMatchBlock.setVar(vars,MatchingVars.LAST_MATCH,"I");
                setMatchBlock.jump(head,GOTO);

                tail.jump(setMatchBlock,Opcodes.IFNE);
                tail.jump(loopPreface,GOTO);
            }
            else {
                tail.readThis();
                tail.readVar(vars, MatchingVars.STATE, "I");
                tail.call(WAS_ACCEPTED_METHOD,getClassName(),"(I)Z");
                tail.jump(loopPreface,IFEQ);
            }
        }
    }

    protected void addMatchesPrefaceBlock(MatchingVars vars, Block initialBlock, Block failureBlock) {

        addReadStringLength(vars, initialBlock);
        addLengthCheck(vars, initialBlock, failureBlock, true);

        // Initialize variables
        initialBlock.push(0);
        initialBlock.setVar(vars,MatchingVars.INDEX,"I");
        if (factorization.getSharedPrefix().isPresent()) {
            var prefix = factorization.getSharedPrefix().get();
            initialBlock.readThis();
            initialBlock.readVar(vars, MatchingVars.INDEX, "I");
            initialBlock.call(seekMethodName(true),getClassName(),"(I)I");
            initialBlock.setVar(vars,MatchingVars.INDEX,"I");
            initialBlock.readVar(vars, MatchingVars.INDEX, "I");
            initialBlock.push(-1);
            initialBlock.cmp(failureBlock, IF_ICMPEQ);
            int state = dfa.after(getEffectivePrefix(prefix, true)).get().getStateNumber();
            initialBlock.push(state);
            initialBlock.setVar(vars,MatchingVars.STATE,"I");
        }
        else {
            initialBlock.push(0);
            initialBlock.setVar(vars,MatchingVars.STATE,"I");
        }

        initialBlock.readThis();
        initialBlock.readField(DFAClassCompiler.STRING_FIELD,true,CompilerUtil.STRING_DESCRIPTOR);
        initialBlock.setVar(vars,MatchingVars.STRING,CompilerUtil.STRING_DESCRIPTOR);
    }

    protected void addContainedInPrefaceBlock(MatchingVars vars, Block initialBlock, Block failureBlock) {
        // TODO: this is just a simplification ignoring the backwards case
        if (vars.forwards && factorization.getSharedPrefix().isPresent()) {
            var prefix = factorization.getSharedPrefix().get();
            initialBlock.readThis();
            initialBlock.readVar(vars, MatchingVars.INDEX, "I");
            initialBlock.call(seekMethodName(false),getClassName(),"(I)I");
            initialBlock.setVar(vars,MatchingVars.INDEX,"I");
            initialBlock.readVar(vars, MatchingVars.INDEX, "I");
            initialBlock.push(-1);
            initialBlock.cmp(failureBlock, IF_ICMPEQ);
            int state = dfa.after(getEffectivePrefix(prefix, false)).get().getStateNumber();
            initialBlock.push(state);
            initialBlock.setVar(vars,MatchingVars.STATE,"I");
        }
        else {
            initialBlock.push(0);
            initialBlock.setVar(vars,MatchingVars.STATE,"I");
        }

        initialBlock.readThis();
        initialBlock.readField(DFAClassCompiler.STRING_FIELD,true,CompilerUtil.STRING_DESCRIPTOR);
        initialBlock.setVar(vars,MatchingVars.STRING,CompilerUtil.STRING_DESCRIPTOR);
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
        initialBlock.readField(LENGTH_FIELD,true, "I");
        initialBlock.setVar(vars,MatchingVars.LENGTH,"I");
    }

    Block addReturnBlock(Method method, MatchingVars vars) {
        var returnBlock = method.addBlock();
        returnBlock.readThis();
        returnBlock.readVar(vars.stateVar,"I");
        returnBlock.call(WAS_ACCEPTED_METHOD,getClassName(),"(I)Z");
        returnBlock.addReturn(IRETURN);

        return returnBlock;
    }

    private Method createContainedInMethod() {
        var vars = new MatchingVars(1, 2, 3, 4, 5);
        var method = mkMethod("containedIn", new ArrayList<>(), "Z", vars);

        var ultraInitialBlock = method.addBlock();
        var initialBlock = method.addBlock();
        var matchLoopBlock = method.addBlock();
        var returnBlock = addReturnBlock(method, vars);
        var failureBlock = addFailureBlock(method, 0);

        // Initialize variables
        ultraInitialBlock.push(0);
        ultraInitialBlock.setVar(vars,MatchingVars.INDEX,"I");
        addReadStringLength(vars, ultraInitialBlock);
        addLengthCheck(vars, ultraInitialBlock, failureBlock, false);

        addContainedInPrefaceBlock(vars, initialBlock, failureBlock);
        var prefix = factorization.getSharedPrefix().orElse("");
        if (dfa.after(prefix.substring(0, Math.min(1, prefix.length()))).get().isAccepting()) {
            var wasAcceptedPostPrefixBlock = method.addBlockAfter(initialBlock);
            wasAcceptedPostPrefixBlock.readThis();
            wasAcceptedPostPrefixBlock.readVar(vars, MatchingVars.STATE, "I");
            wasAcceptedPostPrefixBlock.call(WAS_ACCEPTED_METHOD,getClassName(),"(I)Z");
            wasAcceptedPostPrefixBlock.jump(returnBlock, IFNE);
        }

        fillMatchLoopBlock(vars, method, matchLoopBlock, returnBlock, initialBlock, false, false);
        return method;
    }

    public static DFAClassBuilder build(String name, DFA dfa, Node node) {

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
