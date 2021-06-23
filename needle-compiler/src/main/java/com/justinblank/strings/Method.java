package com.justinblank.strings;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class Method {

    final String methodName;
    final List<String> arguments;
    final List<Block> blocks;
    final String returnType;
    private final Vars matchingVars;

    Method(String methodName, List<String> arguments, String returnType, Vars matchingVars) {
        this.methodName = methodName;
        this.arguments = arguments;
        this.blocks = new ArrayList<>();
        this.returnType = returnType;
        this.matchingVars = matchingVars;
    }

    Block addBlock() {
        Block block = new Block(this.blocks.size(), new ArrayList<>());
        this.blocks.add(block);
        return block;
    }

    void addBlock(Block block) {
        block.number = this.blocks.size();
        this.blocks.add(block);
    }

    /**
     * Create a new block and insert after the passed block
     *
     * @param block the block after which the new block will be inserted
     * @return the new block
     */
    Block addBlockAfter(Block block) {
        var inserted = new Block(block.number, new ArrayList<>());
        this.blocks.add(block.number + 1, inserted);
        for (int i = block.number + 1; i < blocks.size(); i++) {
            this.blocks.get(i).number++;
        }
        return inserted;
    }

    public String descriptor() {
        return "(" + StringUtils.join(arguments, "") + ")" + returnType;
    }

    public Optional<Vars> getMatchingVars() {
        return Optional.ofNullable(matchingVars);
    }

    @Override
    public String toString() {
        return "Method{" +
                "methodName='" + methodName + '\'' +
                ", arguments=" + arguments +
                ", returnType='" + returnType + '\'' +
                ", blockCount=" + blocks.size() +
                '}';
    }

    public int operationCount() {
        int count = 0;
        for (var block : blocks) {
            count += block.operations.size();
        }
        return count;
    }
}
