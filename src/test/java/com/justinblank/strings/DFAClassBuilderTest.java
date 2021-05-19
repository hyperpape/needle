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
import java.util.List;

import static com.justinblank.strings.CompilerUtil.STRING_DESCRIPTOR;
import static org.junit.Assert.*;

public class DFAClassBuilderTest {
    
    @Test
    public void testCompilation() {
        try {
            Node node = RegexParser.parse("a");
            NFA nfa = new NFA(RegexInstrBuilder.createNFA(node));
            DFA dfa =  NFAToDFACompiler.compile(nfa);
            String className = "testCompilation";
            var builder = DFAClassBuilder.build(className, dfa, node);
            Class<?> matcherClass = compileFromBuilder(builder, className);
            Matcher o = (Matcher) matcherClass.getDeclaredConstructors()[0].newInstance("a");
            assertTrue(o.matches());
            assertTrue(o.containedIn());
            assertEquals(MatchResult.success(0, 1), o.find());
            o = (Matcher) matcherClass.getDeclaredConstructors()[0].newInstance("ab");
            assertFalse(o.matches());
            assertTrue(o.containedIn());
            assertEquals(MatchResult.success(0, 1), o.find());
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    @Test
    public void testCompilationRange() {
        try {
            Node node = RegexParser.parse("[0-9]");
            NFA nfa = new NFA(RegexInstrBuilder.createNFA(node));
            DFA dfa =  NFAToDFACompiler.compile(nfa);
            String className = "testCompilationRange";
            var builder = DFAClassBuilder.build(className, dfa, node);
            Class<?> matcherClass = compileFromBuilder(builder, className);
            Matcher o = (Matcher) matcherClass.getDeclaredConstructors()[0].newInstance("0");
            assertTrue(o.matches());
            assertTrue(o.containedIn());
            assertEquals(MatchResult.success(0, 1), o.find());
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    @Test
    public void testCompilationRepetition() {
        try {
            Node node = RegexParser.parse("[0-9]+");
            NFA nfa = new NFA(RegexInstrBuilder.createNFA(node));
            DFA dfa =  NFAToDFACompiler.compile(nfa);
            String className = "testCompilationRepetition";
            var builder = DFAClassBuilder.build(className, dfa, node);
            Class<?> matcherClass = compileFromBuilder(builder, className);
            Matcher m = (Matcher) matcherClass.getDeclaredConstructors()[0].newInstance("0");
            assertTrue(m.matches());
            assertTrue(m.containedIn());

            m = (Matcher) matcherClass.getDeclaredConstructors()[0].newInstance("01");
            assertTrue(m.matches());
            assertTrue(m.containedIn());
            assertEquals(MatchResult.success(0, 2), m.find());
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    @Test
    public void testCompilationMultiCharLiteral() {
        Matcher m;
        try {
            Node node = RegexParser.parse("ab");
            NFA nfa = new NFA(RegexInstrBuilder.createNFA(node));
            DFA dfa =  NFAToDFACompiler.compile(nfa);
            String className = "testCompilationMultiCharLiteral";
            var builder = DFAClassBuilder.build(className, dfa, node);
            Class<?> matcherClass = compileFromBuilder(builder, className);
            m = (Matcher) matcherClass.getDeclaredConstructors()[0].newInstance("ab");
            assertTrue(m.matches());
            assertTrue(m.containedIn());
            assertEquals(2, m.getClass().getDeclaredMethod("indexForwards", int.class).invoke(m, 0));
            assertEquals(0, m.getClass().getDeclaredMethod("indexBackwards", int.class).invoke(m, 2));
            assertEquals(MatchResult.success(0, 2), m.find());
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        }
        assertTrue(m.matches());
    }

    @Test
    public void test3CharLiteral() {
        try {
            Node node = RegexParser.parse("123");
            NFA nfa = new NFA(RegexInstrBuilder.createNFA(node));
            DFA dfa = NFAToDFACompiler.compile(nfa);
            String className = "testCompilation3CharLiteral";
            var builder = DFAClassBuilder.build(className, dfa, node);
            Class<?> matcherClass = compileFromBuilder(builder, className);
            var m = (Matcher) matcherClass.getDeclaredConstructors()[0].newInstance("123");
            assertTrue(m.matches());
            assertTrue(m.containedIn());
            m = (Matcher) matcherClass.getDeclaredConstructors()[0].newInstance("1234");
            assertFalse(m.matches());
            assertTrue(m.containedIn());
            m = (Matcher) matcherClass.getDeclaredConstructors()[0].newInstance("12");
            assertFalse(m.matches());
            assertFalse(m.containedIn());
        } catch (AssertionError e) {
            throw e;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    @Test
    public void testLongLiteral() {
        try {
            Node node = RegexParser.parse("123456789");
            NFA nfa = new NFA(RegexInstrBuilder.createNFA(node));
            DFA dfa = NFAToDFACompiler.compile(nfa);
            String className = "testCompilation9CharLiteral";
            var builder = DFAClassBuilder.build(className, dfa, node);
            Class<?> matcherClass = compileFromBuilder(builder, className);
            var m = (Matcher) matcherClass.getDeclaredConstructors()[0].newInstance("123456789");
            assertTrue(m.matches());
            assertTrue(m.containedIn());
            m = (Matcher) matcherClass.getDeclaredConstructors()[0].newInstance("123");
            assertFalse(m.matches());
            assertFalse(m.containedIn());
            m = (Matcher) matcherClass.getDeclaredConstructors()[0].newInstance("123456780");
            assertFalse(m.matches());
            assertFalse(m.containedIn());
        } catch (AssertionError e) {
            throw e;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    @Test
    public void testMethod9AcceptingStates() {
        try {
            Node node = RegexParser.parse("1?2?3?4?5?6?7?8?9?");
            NFA nfa = new NFA(RegexInstrBuilder.createNFA(node));
            DFA dfa = NFAToDFACompiler.compile(nfa);
            String className = "testCompilation9AcceptingStates";
            var builder = DFAClassBuilder.build(className, dfa, node);
            Class<?> matcherClass = compileFromBuilder(builder, className);
            var m = (Matcher) matcherClass.getDeclaredConstructors()[0].newInstance("123456789");
            assertTrue(m.matches());
            assertTrue(m.containedIn());
            // m = (Matcher) matcherClass.getDeclaredConstructors()[0].newInstance("123abc");
            // assertFalse(m.matches());
            m = (Matcher) matcherClass.getDeclaredConstructors()[0].newInstance("abcdef");
            assertFalse(m.matches());
        } catch (AssertionError e) {
            throw e;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    @Test
    public void testCharacterRepetition() throws Exception {
        try {
            Node node = RegexParser.parse("a*");
            NFA nfa = new NFA(RegexInstrBuilder.createNFA(node));
            DFA dfa =  NFAToDFACompiler.compile(nfa);
            String className = "testCharacterRepetition";
            var builder = DFAClassBuilder.build(className, dfa, node);
            Class<?> matcherClass = compileFromBuilder(builder, className);
            var m = (Matcher) matcherClass.getDeclaredConstructors()[0].newInstance("a");
            assertTrue(m.matches());
            assertTrue(m.containedIn());
            m = (Matcher) matcherClass.getDeclaredConstructors()[0].newInstance("e");
            assertFalse(m.matches());
            assertTrue(m.containedIn());
            m = (Matcher) matcherClass.getDeclaredConstructors()[0].newInstance("");
            assertTrue(m.matches());
            assertTrue(m.containedIn());
            assertEquals(MatchResult.success(0, 0), m.find());
        } catch (AssertionError e) {
            throw e;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    @Test
    public void testUnionOfCharsFollowedByChar() {
        try {
            Node node = RegexParser.parse("(a|b)a");
            NFA nfa = new NFA(RegexInstrBuilder.createNFA(node));
            DFA dfa =  NFAToDFACompiler.compile(nfa);
            String className = "testUnionOfCharsFollowedByChar";
            var builder = DFAClassBuilder.build(className, dfa, node);
            Class<?> matcherClass = compileFromBuilder(builder, className);
            Matcher o = (Matcher) matcherClass.getDeclaredConstructors()[0].newInstance("aa");
            assertTrue(o.matches());
            assertTrue(o.containedIn());

            o = (Matcher) matcherClass.getDeclaredConstructors()[0].newInstance("ba");
            assertTrue(o.matches());
            assertTrue(o.containedIn());
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        }
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
        try (FileOutputStream fos = new FileOutputStream("target/"  + name + ".class")) {
            fos.write(classBytes);
        }

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        CheckClassAdapter.verify(new ClassReader(classBytes), false, printWriter);
        // assertEquals("", stringWriter.toString());
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
            block.addOperation(Operation.mkCallState(returnBlock));
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
