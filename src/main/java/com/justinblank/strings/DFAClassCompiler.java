package com.justinblank.strings;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

public class DFAClassCompiler extends ClassCompiler {

    protected static final String STRING_FIELD = "string";

    private final List<Method> stateMethods;
    private final List<Method> backwardsStateMethods;

    DFAClassCompiler(DFAClassBuilder classBuilder) {
        this(classBuilder, false);
    }

    DFAClassCompiler(DFAClassBuilder classBuilder, boolean debug) {
        super(classBuilder, debug);
        this.stateMethods = classBuilder.stateMethods;
        this.backwardsStateMethods = classBuilder.backwardsStateMethods;
        for (Method method : classBuilder.allMethods()) {
            transformMethod(method);
        }
    }

    void transformMethod(Method method) {
        var blocksToAdd = new ArrayList<Pair<Block, List<Block>>>();
        for (var block : method.blocks) {
            var transformed = new ArrayList<Operation>();
            for (var op : block.operations) {
                switch (op.inst) {
                    case CHECK_BOUNDS:
                        var vars = method.getMatchingVars().get();
                        transformed.add(Operation.mkReadVar(vars, MatchingVars.INDEX, "I"));
                        transformed.add(Operation.mkReadVar(vars, MatchingVars.LENGTH, "I"));
                        transformed.add(Operation.mkJump(op.target, IF_ICMPGE));
                        break;
                    case CHECK_CHARS:
                        CheckCharsOperation ccOp = (CheckCharsOperation) op;
                        var newBlocks = new ArrayList<Block>();
                        blocksToAdd.add(Pair.of(block, newBlocks));

                        vars = method.getMatchingVars().get();
                        // TODO: less sophisticated than previous impl
                        if (ccOp.transitions.size() == 1 && ccOp.transitions.get(0).getLeft().isSingleCharRange()) {
                            var transition = ccOp.transitions.get(0);
                            transformed.add(Operation.mkReadVar(vars, MatchingVars.CHAR, "C"));
                            transformed.add(Operation.pushValue(transition.getLeft().getStart()));
                            transformed.add(Operation.mkJump(ccOp.target, Opcodes.IF_ICMPNE));
                            if (ccOp.getSuccessTarget() == null) {
                                transformed.add(Operation.pushValue(transition.getRight().getStateNumber()));
                                transformed.add(Operation.mkReturn(IRETURN));
                            }
                            else {
                                transformed.add(Operation.pushValue(transition.getRight().getStateNumber()));
                                transformed.add(Operation.mkJump(ccOp.getSuccessTarget(), GOTO));
                            }
                        }
                        else {
                            for (int i = 0; i < ccOp.transitions.size(); i++) {
                                var transition = ccOp.transitions.get(i);

                                var transitionBlock = new Block(-1, new ArrayList<>());
                                newBlocks.add(transitionBlock);
                                transitionBlock.push(transition.getRight().getStateNumber());
                                if (ccOp.getSuccessTarget() == null) {
                                    transitionBlock.addReturn(IRETURN);
                                }
                                else {
                                    transitionBlock.operate(POP).jump(ccOp.getSuccessTarget(), GOTO);
                                }

                                transformed.add(Operation.mkReadVar(vars, MatchingVars.CHAR, "C"));
                                transformed.add(Operation.pushValue(transition.getLeft().getStart()));
                                transformed.add(Operation.mkJump(op.target, Opcodes.IF_ICMPLT));
                                transformed.add(Operation.mkReadVar(vars, MatchingVars.CHAR, "C"));
                                transformed.add(Operation.pushValue(transition.getLeft().getEnd()));
                                transformed.add(Operation.mkJump(transitionBlock, Opcodes.IF_ICMPLE));
                            }
                        }
                        transformed.add(Operation.mkJump(ccOp.target, GOTO));
                        break;
                    case READ_CHAR:
                        vars = method.getMatchingVars().get();
                        if (vars.stringVar < 0) {
                            transformed.add(Operation.mkReadThis());
                            transformed.add(Operation.mkReadField(STRING_FIELD, true, CompilerUtil.STRING_DESCRIPTOR));
                        } else {
                            transformed.add(Operation.mkReadVar(vars, MatchingVars.STRING, CompilerUtil.STRING_DESCRIPTOR));
                        }
                        transformed.add(Operation.mkReadVar(vars, MatchingVars.INDEX, "I"));
                        transformed.add(Operation.call("charAt", "java/lang/String", "(I)C"));
                        break;
                    case CALL_STATE:
                        var offsets = (Map<Integer, Offset>) op.getAttribute(DFAClassBuilder.OFFSETS_ATTRIBUTE);
                        vars = method.getMatchingVars().get();
                        List<Block> stateBlocks = new ArrayList<>();
                        newBlocks = new ArrayList<Block>();
                        blocksToAdd.add(Pair.of(block, newBlocks));
                        for (var m : (vars.forwards ? stateMethods : backwardsStateMethods)) {
                            var b = new Block(0, new ArrayList<>());
                            if (vars.forwards && isOffsetMethod(offsets, m)) {
                                b.readThis().
                                        readVar(vars, MatchingVars.INDEX, "I").
                                        setField(MatchingVars.INDEX, getClassName(), "I");
                            }
                            b.call(m.methodName,getClassName(),m.descriptor());
                            b.jump(op.target,GOTO);
                            newBlocks.add(b);
                            stateBlocks.add(b);
                        }

                        // stateBlocks.get(0) here is meaningless, we always cover all cases...
                        transformed.add(Operation.mkTableSwitch(stateBlocks, stateBlocks.get(0), 0, stateBlocks.size() - 1));
                        break;
                    default:
                        transformed.add(op);
                }
            }
            block.operations = transformed;
        }
        // TODO: fix n-squared
        for (var pair : blocksToAdd) {
            var i = method.blocks.indexOf(pair.getLeft()) + 1;
            for (var block : pair.getRight()) {
                block.number = i;
                method.blocks.add(i++, block);
                for (var j = i; j < method.blocks.size(); j++) {
                    method.blocks.get(j).number++;
                }
            }
        }
    }

    private boolean isOffsetMethod(Map<Integer, Offset> offsets, Method m) {
        // TODO: UGH
        var s = m.methodName.substring("state".length());
        var isNumber = NumberUtils.isDigits(s);

        return isNumber && offsets.containsKey(Integer.valueOf(s))
                && DFAClassBuilder.isUsefulOffset(offsets.get(Integer.valueOf(s)));
    }
}
