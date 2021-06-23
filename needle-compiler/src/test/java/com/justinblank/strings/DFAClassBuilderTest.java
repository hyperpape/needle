package com.justinblank.strings;

import com.justinblank.classloader.MyClassLoader;
import com.justinblank.strings.RegexAST.Node;
import com.justinblank.strings.RegexAST.NodePrinter;
import org.junit.Test;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static com.justinblank.strings.CompilerUtil.STRING_DESCRIPTOR;
import static com.justinblank.strings.SearchMethodTestUtil.find;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

// Tests belong here if they're coupled to the internal structure of the classes that the DFAClassBuilder produces
public class DFAClassBuilderTest {

    private static final AtomicInteger CLASS_COUNTER = new AtomicInteger(0);

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
            block.setVar(vars, MatchingVars.INDEX, "I");
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
            var vars = new MatchingVars(-1, -1, -1, -1, 1);

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
    public void testIndexForwards() {
        try {
            var dfa = DFA.createDFA("abc");
            var node = RegexParser.parse("abc");
            var builder = new DFAClassBuilder("indexForwards", "java/lang/Object", new String[]{}, dfa, dfa, node.bestFactors());
            builder.initMethods();
            Class<?> c = compileFromBuilder(builder, "indexForwards");
            Object o = c.getDeclaredConstructors()[0].newInstance("abca");
            assertEquals(3, o.getClass().getDeclaredMethod("indexForwards", int.class).invoke(o, 0));
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    @Test
    public void testIndexForwardsSingleChar() {
        try {
            var dfa = DFA.createDFA("a");
            var node = RegexParser.parse("a");
            var builder = new DFAClassBuilder("indexForwardsSingleChar", "java/lang/Object", new String[]{}, dfa, dfa, node.bestFactors());
            builder.initMethods();
            Class<?> c = compileFromBuilder(builder, "indexForwardsSingleChar");
            Object o = c.getDeclaredConstructors()[0].newInstance("aba");
            assertEquals(1, o.getClass().getDeclaredMethod("indexForwards", int.class).invoke(o, 0));
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    @Test
    public void testSeekContainedIn2() {
        try {
            var dfa = DFA.createDFA("a");
            var node = RegexParser.parse("a");
            var builder = new DFAClassBuilder("seekContainedIn2", "java/lang/Object", new String[]{}, dfa, dfa, node.bestFactors());
            builder.initMethods();
            Class<?> c = compileFromBuilder(builder, "seekContainedIn2");
            Object o = c.getDeclaredConstructors()[0].newInstance("aba");
            assertEquals(1, o.getClass().getDeclaredMethod("seekContainedIn", int.class).invoke(o, 0));
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
            var method = builder.mkMethod("callState", new ArrayList<>(), "I", vars);
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

    @Test
    public void testBuildPossiblyEmptyRange() {
        // This triggers handling of an empty string prefix
        testCompilable("[B-i]{0,2}");
    }

    @Test
    public void testRangeWithOffset() {
        testCompilable("[X-k]{3,6}}");
    }

    private void addTrivialStateMethod(DFAClassBuilder builder, String name) {
        var stateMethod = builder.mkMethod(name, List.of("I"), "I");
        builder.stateMethods.add(stateMethod);
        var stateBlock = stateMethod.addBlock();
        stateBlock.push(0);
        stateBlock.addReturn(Opcodes.IRETURN);
    }

    @Test
    public void testStateMethodIsCompilable() throws Exception {
        var dfa = DFA.createDFA("a");
        var builder = new DFAClassBuilder("testStateMethod", "java/lang/Object", null, dfa, dfa, null);
        builder.addStateMethods(dfa);
        Class<?> c = compileFromBuilder(builder, "testStateMethod");
    }

    @Test
    public void generativeDFAMatchingTest() {
        Random random = new Random();
        for (int maxSize = 1; maxSize < 6; maxSize++) {
            int count = 20 * (int) Math.pow(2, 6 - maxSize);
            for (int i = 0; i < count; i++) {
                RegexGenerator regexGenerator = new RegexGenerator(random, maxSize);
                Node node = regexGenerator.generate();
                String regex = NodePrinter.print(node);
                try {
                    testCompilable(regex);
                } catch (Throwable t) {
                    System.out.println("failed to compile regex=" + regex);
                    throw t;
                }
            }
        }
    }

    private void testCompilable(String regex) {
        var p= DFACompiler.compile(regex, "DFAClassBuilderTest" + CLASS_COUNTER.incrementAndGet());
        // This ensures we load the matcher class, and thereby validate it
        assertNotNull(p.matcher(""));
    }
}
