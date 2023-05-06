package com.justinblank.strings;

import com.justinblank.classcompiler.ClassCompiler;
import com.justinblank.classcompiler.Field;
import com.justinblank.classcompiler.Operation;
import com.justinblank.strings.DFAClassBuilder;
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

import static com.justinblank.classcompiler.CompilerUtil.STRING_DESCRIPTOR;
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
        builder.addMethod(builder.createContainedInMethod(new FindMethodSpec(dfa, "")));
        builder.addStateMethods();

        var compiler = new ClassCompiler(builder);
        byte[] classBytes = compiler.generateClassAsBytes();

        MyClassLoader.getInstance().loadClass("testContainedIn", classBytes);
    }

    private Class<?> compileFromBuilder(DFAClassBuilder builder, String name) throws IOException {
        var compiler = new ClassCompiler(builder);
        byte[] classBytes = compiler.generateClassAsBytes();
        return MyClassLoader.getInstance().loadClass(name, classBytes);
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
    public void testBuildPossiblyEmptyRange() {
        // This triggers handling of an empty string prefix
        testCompilable("[B-i]{0,2}");
    }

    @Test
    public void testRangeWithOffset() {
        testCompilable("[X-k]{3,6}}");
    }

    @Test
    public void testStateMethodIsCompilable() throws Exception {
        var dfa = DFA.createDFA("a");
        var builder = new DFAClassBuilder("testStateMethod", "java/lang/Object", null, dfa, dfa, null);
        builder.addStateMethods();
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
