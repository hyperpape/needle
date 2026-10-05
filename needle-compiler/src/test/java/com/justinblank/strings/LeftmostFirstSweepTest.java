package com.justinblank.strings;

import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exhaustive check of leftmost-first find() spans against java.util.regex: for each pattern, every string of length
 * at most 6 over a small alphabet, comparing every match span. Ordered alternations whose precedence decides the
 * span, matches that begin with a shared suffix, and suffixes that are not matches themselves are all included.
 */
@Timeout(300)
class LeftmostFirstSweepTest {

    private static final java.util.concurrent.atomic.AtomicInteger CLASS_COUNTER =
            new java.util.concurrent.atomic.AtomicInteger();

    @ParameterizedTest
    @CsvSource({
            "a[^a]*a",
            "a*baa",
            "[^x]*x",
            "ax|a[^a]*a",
            "a[^a]*a|aa",
            "x[^x]*x|ax",
            "ab.ab",
            "ab",
            "ax|abx",
            "(a|ab)x",
            ".*x",
            "(a[^a]*a|a)",
            "ab(abxq)*x",
            "abc"
    })
    void exhaustiveSweep(String regex) {
        Pattern pattern = DFACompiler.compile(regex, "LeftmostFirstSweep" + CLASS_COUNTER.incrementAndGet(), 0);
        java.util.regex.Pattern jdkPattern = java.util.regex.Pattern.compile(regex);
        String alphabet = "abxz";
        int total = 1;
        for (int len = 0; len <= 6; len++) {
            for (int i = 0; i < total; i++) {
                StringBuilder sb = new StringBuilder();
                int v = i;
                for (int k = 0; k < len; k++) {
                    sb.append(alphabet.charAt(v % alphabet.length()));
                    v /= alphabet.length();
                }
                String input = sb.toString();
                Matcher matcher = pattern.matcher(input);
                java.util.regex.Matcher jdkMatcher = jdkPattern.matcher(input);
                int count = 0;
                while (jdkMatcher.find()) {
                    count++;
                    assertTrue(matcher.find(), regex + " vs \"" + input + "\": needle stopped early at JDK match #"
                            + count + " " + jdkMatcher.start() + "," + jdkMatcher.end());
                    assertEquals(jdkMatcher.start(), matcher.start(),
                            regex + " match #" + count + " on \"" + input + "\" start");
                    assertEquals(jdkMatcher.end(), matcher.end(),
                            regex + " match #" + count + " on \"" + input + "\" end");
                }
                assertFalse(matcher.find(), regex + " on \"" + input + "\": extra match "
                        + matcher.start() + "," + matcher.end());
            }
            total *= alphabet.length();
        }
    }
}
