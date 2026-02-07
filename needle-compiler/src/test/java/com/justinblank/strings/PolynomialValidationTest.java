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
