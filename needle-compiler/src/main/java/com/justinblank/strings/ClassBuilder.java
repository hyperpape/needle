package com.justinblank.strings;

import org.objectweb.asm.Opcodes;

import java.util.*;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.DUP;

public class ClassBuilder {

    protected static final String STATE_FIELD = "state";
    protected static final String CHAR_FIELD = "c";
    protected static final String LENGTH_FIELD = "length";
    protected static final String STRING_FIELD = "string";
    protected static final String INDEX_FIELD = "index";

    final Map<String, List<Method>> methods = new HashMap<>();
    final List<Field> fields = new ArrayList<>();
    final String superClass;
    final String[] interfaces;
    final List<Block> staticBlocks = new ArrayList<>();
    private final String className;

    protected ClassBuilder(String className, String superClass, String[] interfaces) {
        this.className = className;
        this.superClass = superClass;
        this.interfaces = interfaces;
    }

    protected void addMethod(Method method) {
        this.methods.computeIfAbsent(method.methodName, (s) -> new ArrayList<>()).add(method);
    }

    /**
     * Make an empty method and add it
     *
     * @param methodName the name
     * @param arguments  as descriptors
     * @param returnType as descriptor
     * @return the newly created method
     */
    protected Method mkMethod(String methodName, List<String> arguments, String returnType) {
        var method = new Method(methodName, arguments, returnType, null);
        addMethod(method);
        return method;
    }

    /**
     * Make an empty method and add it
     *
     * @param methodName the name
     * @param arguments  as descriptors
     * @param returnType as descriptor
     * @param vars a set of vars for this method
     * @return the newly created method
     */
    protected Method mkMethod(String methodName, List<String> arguments, String returnType, Vars vars) {
        var method = new Method(methodName, arguments, returnType, vars);
        addMethod(method);
        return method;
    }

    protected Block addStaticBlock() {
        var b = new Block(staticBlocks.size(), new ArrayList<>());
        staticBlocks.add(b);
        return b;
    }

    public Method emptyConstructor() {
        var method = new Method("<init>", List.of(), "V", null);
        var block = method.addBlock();
        block.readThis();
        block.call("<init>", "java/lang/Object", "()V", true);
        block.addReturn(Opcodes.RETURN);
        addMethod(method);
        return method;
    }

    public Block constructorSkeleton(List<String> args, MatchingVars vars) {
        var method = new Method("<init>", args, "V", vars);
        var block = method.addBlock();
        block.readThis();
        block.call("<init>", "java/lang/Object", "()V", true);

        var returnBlock = method.addBlock();
        returnBlock.addReturn(Opcodes.RETURN);
        addMethod(method);
        return block;
    }

    public void construct(Block b, String type) {
        b.construct(type);
        b.operate(DUP);
        b.call("<init>", type, "()V", true);
    }

    protected Collection<Method> allMethods() {
        return methods.values().stream().flatMap(List::stream).collect(Collectors.toSet());
    }

    protected List<Field> getFields() {
        return fields;
    }

    protected void addField(Field field) {
        fields.add(field);
    }

    protected String getClassName() {
        return className;
    }
}
