package com.justinblank.strings;

import com.justinblank.strings.RegexAST.LengthPolynomial;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sanity checks that the length polynomial is a sound over-approximation of the lengths a regex can match. If
 * {@link LengthPolynomial#canMatch(int)} rejects a length that an expression can actually match, the generated
 * matches() pre-filter would reject valid matches.
 */
public class PolynomialValidationTest {

    @Test
    void polynomialAcceptsEveryMatchedLength() throws Exception {
        var errors = new ArrayList<String>();
        for (var spec : new RegexTestSpecParser().readTests()) {
            int flags = spec.flags == null ? 0 : spec.flags.flags;
            try {
                var node = RegexParser.parse(spec.pattern, flags);
                Optional<LengthPolynomial> polynomial = node.lengthPolynomial();
                if (polynomial.isEmpty() || !spec.successful) {
                    continue;
                }
                int matchedLength = spec.end - spec.start;
                if (!polynomial.get().canMatch(matchedLength)) {
                    errors.add(spec.pattern + ": polynomial " + polynomial.get() + " rejected matched length " + matchedLength);
                }
            } catch (UnsupportedOperationException | IllegalArgumentException e) {
                // Syntax that this version of the parser or the polynomial does not handle yet
            }
        }
        assertTrue(errors.isEmpty(), String.join("\n", errors));
    }

    /**
     * The matches() pre-filter must never reject a string that the JDK matches, and it must not fabricate matches
     * either. Exhaustively compare against the JDK for short strings over a small alphabet.
     */
    @Test
    void generatedMatchesAgreesWithJdk() {
        String[] patterns = {
                "(ab){2,3}",
                "(abc)+",
                "(ab)*",
                "a{0,3}",
                "(a{2,3}){2}",
                "(ab){2,3}c",
                "a{2,}",
                "(ab|a)+",
                "a?b?c?",
                "(ab){0,2}(cd){0,2}",
                "(ab|cd)+",
                "(ab|cde)+",
                "(a|bb)c",
                "(ab|cd){2,3}",
                "(abc)+|(def)+",
                "(ab){1,2}|(cde){1,2}",
        };
        int counter = 0;
        for (String regex : patterns) {
            var pattern = DFACompiler.compile(regex, "PolynomialJdkComparison" + counter++);
            var jdk = java.util.regex.Pattern.compile(regex);
            for (String s : stringsUpToLength(6)) {
                assertEquals(jdk.matcher(s).matches(), pattern.matcher(s).matches(),
                        "Mismatch for regex=" + regex + " on string=\"" + s + "\"");
            }
        }
    }

    /**
     * A polynomial with two unbounded generators produces a numerical semigroup rather than a simple residue class;
     * make sure the generated filter does not reject any string built from legal repetitions.
     */
    @Test
    void generatedMatchesHandlesMultipleGenerators() {
        int counter = 0;
        for (var regex : new String[] {"(ABC)+(DEFGHIJ)+", "(ABC){1,8}(DEFGHIJ)+"}) {
            var pattern = DFACompiler.compile(regex, "PolynomialJdkComparisonMultiGenerator" + counter++);
            var jdk = java.util.regex.Pattern.compile(regex);
            for (int abc = 1; abc <= 8; abc++) {
                for (int def = 1; def <= 5; def++) {
                    var s = "ABC".repeat(abc) + "DEFGHIJ".repeat(def);
                    assertTrue(jdk.matcher(s).matches(), regex + " on " + s);
                    assertTrue(pattern.matcher(s).matches(), regex + " on " + s);
                }
            }
        }
    }

    /**
     * A union produces a disjunction of length polynomials rather than a single one; make sure the generated filter
     * does not reject any string the branch could produce.
     */
    @Test
    void generatedMatchesHandlesUnionGenerators() {
        var regex = "(ABC)+|(DEFGHIJ)+";
        var pattern = DFACompiler.compile(regex, "PolynomialJdkComparisonUnion");
        var jdk = java.util.regex.Pattern.compile(regex);
        for (int abc = 1; abc <= 4; abc++) {
            var s = "ABC".repeat(abc);
            assertTrue(jdk.matcher(s).matches(), regex + " on " + s);
            assertTrue(pattern.matcher(s).matches(), regex + " on " + s);
        }
        for (int def = 1; def <= 3; def++) {
            var s = "DEFGHIJ".repeat(def);
            assertTrue(jdk.matcher(s).matches(), regex + " on " + s);
            assertTrue(pattern.matcher(s).matches(), regex + " on " + s);
        }
        var fixed = DFACompiler.compile("(a|bb)c", "PolynomialJdkComparisonUnionFixed");
        assertTrue(fixed.matcher("ac").matches());
        assertTrue(fixed.matcher("bbc").matches());
        assertFalse(fixed.matcher("aac").matches());
        assertFalse(fixed.matcher("c").matches());
    }

    /**
     * A union whose minimum lengths already cover every residue produces a table that accepts every length at or above
     * the minimum. It should not be embedded, though the minimum length check is still worthwhile.
     */
    @Test
    void lengthArrayOnlyEmittedWhenItAddsResidueInformation() {
        var uninformative = DFACompiler.compileToBytes("(ab)+|(cde)+", "LengthFilterUninformativeUnion", 0);
        assertFalse(hasLengthFilterField(uninformative));

        var informative = DFACompiler.compileToBytes("(ABC)+|(DEFGHIJ)+", "LengthFilterInformativeUnion", 0);
        assertTrue(hasLengthFilterField(informative));

        // The minimum length is still enforced without the array.
        var pattern = DFACompiler.compile("(ab)+|(cde)+", "LengthFilterUninformativeUnionMatcher");
        assertFalse(pattern.matcher("").matches());
        assertTrue(pattern.matcher("ab").matches());
        assertTrue(pattern.matcher("cde").matches());
    }

    private static boolean hasLengthFilterField(byte[] classBytes) {
        var found = new boolean[1];
        new org.objectweb.asm.ClassReader(classBytes).accept(
                new org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9) {
                    @Override
                    public org.objectweb.asm.FieldVisitor visitField(int access, String name, String descriptor,
                            String signature, Object value) {
                        if (name.equals("LENGTH_FILTER")) {
                            found[0] = true;
                        }
                        return null;
                    }
                }, 0);
        return found[0];
    }

    private static List<String> stringsUpToLength(int maxLength) {
        var strings = new ArrayList<String>();
        strings.add("");
        var previous = List.of("");
        for (int length = 1; length <= maxLength; length++) {
            var next = new ArrayList<String>();
            for (var prefix : previous) {
                for (char c : new char[] {'a', 'b', 'c', 'd'}) {
                    next.add(prefix + c);
                }
            }
            strings.addAll(next);
            previous = next;
        }
        return strings;
    }
}
