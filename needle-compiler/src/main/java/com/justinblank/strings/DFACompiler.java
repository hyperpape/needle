package com.justinblank.strings;

import com.justinblank.classcompiler.ClassBuilder;
import com.justinblank.classcompiler.ClassCompiler;
import com.justinblank.classcompiler.Method;
import com.justinblank.classloader.MyClassLoader;
import com.justinblank.strings.RegexAST.Node;
import org.objectweb.asm.Opcodes;

import java.util.*;

import static org.objectweb.asm.Opcodes.*;

public class DFACompiler {

    public static Pattern compile(String regex, String className) {
        return compile(regex, className, false);
    }

    static Pattern compile(String regex, String className, boolean debug) {
        if (debug) {
            System.out.println("Compiling " + className + "(" + regex + ")");
        }
        byte[] classBytes = compileToBytes(regex, className, debug ? new DebugOptions(true, true, true) : DebugOptions.none() );
        Class<?> matcherClass = MyClassLoader.getInstance().loadClass(className, classBytes);
        Class<? extends Pattern> c = createPatternClass(className, (Class<? extends Matcher>) matcherClass);
        try {
            return (Pattern) c.getDeclaredConstructors()[0].newInstance();
        } catch (Throwable t) {
            throw new PatternClassCompilationException("Failed to compile pattern from regex '" + regex + "'", t);
        }
    }

    public static byte[] compileToBytes(String regex, String className) {
        return compileToBytes(regex, className, new DebugOptions(false, false, false));
    }

    static byte[] compileToBytes(String regex, String className, DebugOptions debugOptions) {
        Objects.requireNonNull(className, "name cannot be null");
        Node node = RegexParser.parse(regex);
        Factorization factorization = buildFactorization(node);

        NFA forwardNFA = new NFA(RegexInstrBuilder.createNFA(node));
        NFA reversedNFA = new NFA(RegexInstrBuilder.createNFA(node.reversed()));

        DFA dfa = NFAToDFACompiler.compile(forwardNFA, ConversionMode.BASIC);
        DFA containedInDFA = NFAToDFACompiler.compile(forwardNFA, ConversionMode.CONTAINED_IN);
        DFA dfaReversed = NFAToDFACompiler.compile(reversedNFA, ConversionMode.BASIC);
        DFA dfaSearch = NFAToDFACompiler.compile(forwardNFA, ConversionMode.DFA_SEARCH);

        if (debugOptions.isDebug()) {
            printDFARepresentations(dfa, containedInDFA, dfaReversed, dfaSearch);
        }
        checkForOverLongDFAs(List.of(dfa, containedInDFA, dfaReversed, dfaSearch));

        var builder = new DFAClassBuilder(className, dfa, containedInDFA, dfaReversed, dfaSearch, factorization, debugOptions);
        builder.initMethods();
        ClassCompiler compiler = new ClassCompiler(builder, debugOptions.isDebug(), System.out);
        byte[] classBytes = compiler.generateClassAsBytes();
        return classBytes;
    }

    private static void checkForOverLongDFAs(List<DFA> dfas) {
        for (var dfa : dfas) {
            // TODO: Why Short.MAX_VALUE / 2--not obvious why this wouldn't work with Short.MAX_VALUE or Short.MAX_VALUE - 1;
            if (dfa.statesCount() > Short.MAX_VALUE / 2) {
                throw new IllegalArgumentException("Can't compile DFAs with more than " + (Short.MAX_VALUE / 2) + " states");
            }
        }
    }

    private static Factorization buildFactorization(Node node) {
        var factorization = node.bestFactors();
        factorization.setMinLength(node.minLength());
        node.maxLength().ifPresent(factorization::setMaxLength);
        return factorization;
    }

    private static void printDFARepresentations(DFA dfa, DFA containedInDFA, DFA dfaReversed, DFA dfaSearch) {
        System.out.println("----dfa----");
        System.out.println(GraphViz.toGraphviz(dfa));
        System.out.println("----selfTransitioningDFA----");
        System.out.println(GraphViz.toGraphviz(containedInDFA));
        System.out.println("----reversedDFA----");
        System.out.println(GraphViz.toGraphviz(dfaReversed));
        System.out.println("----dfaSearch----");
        System.out.println(GraphViz.toGraphviz(dfaSearch));
    }

    private static Class<? extends Pattern> createPatternClass(String name, Class<? extends Matcher> m) {
        ClassBuilder builder = new ClassBuilder("Pattern" + name, "", "java/lang/Object", new String[]{"com/justinblank/strings/Pattern"});
        builder.addEmptyConstructor();
        var method = new Method("matcher", List.of("Ljava/lang/String;"), "Lcom/justinblank/strings/Matcher;", null);
        builder.addMethod(method);
        method
                .addBlock()
                .construct(name)
                .operate(DUP)
                .readVar(1, "Ljava/lang/String;")
                .call("<init>", name, "(Ljava/lang/String;)V", true)
                .addReturn(Opcodes.ARETURN);

        ClassCompiler compiler = new ClassCompiler(builder);
        return (Class<? extends Pattern>) MyClassLoader.getInstance().loadClass("Pattern" + name, compiler.writeClassAsBytes());
    }

}

