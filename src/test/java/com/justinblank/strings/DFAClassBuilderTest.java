package com.justinblank.strings;

import com.justinblank.classloader.MyClassLoader;
import com.justinblank.strings.RegexAST.Node;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.justinblank.strings.CompilerUtil.STRING_DESCRIPTOR;
import static org.junit.Assert.*;

public class DFAClassBuilderTest {

    @Test
    public void testContainedIn() {
        var dfa = DFA.createDFA("abcd");
        var factorization = RegexParser.parse("abcd").bestFactors();
        var builder = new DFAClassBuilder("testContainedIn", "java/lang/Object", new String[]{}, dfa, dfa, factorization);
        builder.addMethod(builder.createContainedInMethod());
        builder.addStateMethods(dfa);

        var compiler = new DFAClassCompiler(builder);
        byte[] classBytes = compiler.generateClassAsBytes();

        MyClassLoader.getInstance().loadClass("testContainedIn", classBytes);
    }

    @Test
    public void testReadChar() {
        try {
            var builder = new DFAClassBuilder("testReadChar", "java/lang/Object", new String[]{}, null, null, null);
            var vars = new MatchingVars(-1, 2, -1, -1, 1);
            var method = builder.mkMethod("readChar", List.of(STRING_DESCRIPTOR), "V", vars);
            var block = method.addBlock();
            block.push(0);
            block.setVar(vars,MatchingVars.INDEX,"I");
            block.addOperation(Operation.mkReadChar());
            block.operate(Opcodes.POP);
            block.addReturn(Opcodes.RETURN);
            builder.addMethod(method);
            Class<?> matcherClass = compileFromBuilder(builder, "testReadChar");
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    private Class<?> compileFromBuilder(DFAClassBuilder builder, String name) throws IOException {
        var compiler = new DFAClassCompiler(builder);
        byte[] classBytes = compiler.generateClassAsBytes();
        return MyClassLoader.getInstance().loadClass(name, classBytes);
    }

    @Test
    public void testMatchesPrefaceBlock() {
        try {
            var builder = new DFAClassBuilder("matchesPreface", "java/lang/Object", new String[]{}, null, null, Factorization.empty());
            var vars = new MatchingVars( -1, -1, -1, -1, 1);

            builder.addField(new Field(Opcodes.ACC_PRIVATE, DFAClassCompiler.STRING_FIELD, STRING_DESCRIPTOR, null, null));
            var constructorBlock = builder.constructorSkeleton(List.of(STRING_DESCRIPTOR), vars);

            constructorBlock.readThis();
            constructorBlock.readVar(vars, MatchingVars.STRING, STRING_DESCRIPTOR);
            constructorBlock.addOperation(Operation.mkSetField(DFAClassCompiler.STRING_FIELD, "matchesPreface", STRING_DESCRIPTOR));

            vars = new MatchingVars(-1, 1, 2, 3, 4);
            var matches = builder.mkMethod("matches", List.of(), "Z", vars);
            var initialBlock = matches.addBlock();
            builder.addReturnBlock(matches, vars);
            var failureBlock = builder.addFailureBlock(matches, 0);

            builder.addMatchesPrefaceBlock(vars, initialBlock, failureBlock);

            Class<?> c = compileFromBuilder(builder, "matchesPreface");
            Object o = c.getDeclaredConstructors()[0].newInstance("a");
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    @Test
    public void testCallState() {
        try {
            var builder = new DFAClassBuilder("testCallState", "java/lang/Object", new String[]{}, null, null, null);

            builder.emptyConstructor();
            addTrivialStateMethod(builder, "state0");

            var vars = new MatchingVars(-1, -1, -1, -1, -1);
            var method = builder.mkMethod("callState", new ArrayList<>(), "I", vars );
            var block = method.addBlock();
            var returnBlock = method.addBlock();

            block.readThis();
            block.push(0);
            block.push(0);
            var callState = Operation.mkCallState(returnBlock);
            callState.addAttribute(DFAClassBuilder.OFFSETS_ATTRIBUTE, new HashMap<>());
            block.addOperation(callState);
            returnBlock.push(0);
            returnBlock.addReturn(Opcodes.IRETURN);

            builder.findMethods.add(method);
            Class<?> matcherClass = compileFromBuilder(builder, "testCallState");
            assertNotNull(matcherClass.getDeclaredConstructors()[0].newInstance());
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private void addTrivialStateMethod(DFAClassBuilder builder, String name) {
        var stateMethod = builder.mkMethod(name, List.of("I"), "I");
        builder.stateMethods.add(stateMethod);
        var stateBlock = stateMethod.addBlock();
        stateBlock.push(0);
        stateBlock.addReturn(Opcodes.IRETURN);
    }

    @Test
    public void testStateMethod() throws Exception {
        var dfa = DFA.createDFA("a");
        var builder = new DFAClassBuilder("testStateMethod", "java/lang/Object", null, dfa, dfa, null);
        builder.addStateMethods(dfa);
        Class<?> c = compileFromBuilder(builder, "testStateMethod");
    }
}
