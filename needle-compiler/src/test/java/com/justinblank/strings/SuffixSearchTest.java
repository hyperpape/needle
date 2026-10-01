package com.justinblank.strings;

import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the suffix-driven backwards search used for unbounded-length regexes with a shared suffix. Correctness is
 * checked against java.util.regex; scaling checks guard against quadratic behavior when the suffix occurs many times
 * (each occurrence must not trigger a fresh scan of the whole haystack).
 */
@Timeout(300)
class SuffixSearchTest {

    private static final java.util.concurrent.atomic.AtomicInteger CLASS_COUNTER =
            new java.util.concurrent.atomic.AtomicInteger();

    private static void assertSameSpansAsJdk(String regex, String input, int flags) {
        int jdkFlags = flags & ~Pattern.LEFTMOST_LONGEST;
        Pattern pattern = DFACompiler.compile(regex, "SuffixSearch" + CLASS_COUNTER.incrementAndGet(), flags);
        java.util.regex.Pattern jdkPattern = java.util.regex.Pattern.compile(regex, jdkFlags);
        Matcher matcher = pattern.matcher(input);
        java.util.regex.Matcher jdkMatcher = jdkPattern.matcher(input);
        int i = 0;
        while (jdkMatcher.find()) {
            assertTrue(matcher.find(), regex + " vs \"" + input + "\": needle stopped early at JDK match #" + (i + 1)
                    + " " + jdkMatcher.start() + "," + jdkMatcher.end());
            assertEquals(jdkMatcher.start(), matcher.start(), regex + " match #" + (i + 1) + " start");
            assertEquals(jdkMatcher.end(), matcher.end(), regex + " match #" + (i + 1) + " end");
            i++;
        }
        assertTrue(!matcher.find(), regex + " vs \"" + input + "\": extra match " + matcher.start() + "," + matcher.end());
    }

    @ParameterizedTest
    @CsvSource({
            ".*x, axbx",
            "b.*x, bxabxabx",
            "ab.*y, abzzzy",
            "ab.*y, abyqyqy",
            "a*bc, bc",
            "[^x]*x, aabbxaaxx",
            "[A-Za-z]+ing, testing",
            "(t*acg*)*(cg), acgggactcgcc",
            // Suffix search alone determines the span: no match contains the suffix strictly inside, and no match
            // is a proper prefix of another
            "a[^a]*a, aba",
            "a[^a]*a, aaba",
            "a[^a]*a, xaaa",
            "x[^x]*x, axbxc",
            "a[^a]*aa, xaaa",
            "[^x]*x, xaax",
            // Suffix occurs strictly inside matches, or matches are prefixes of one another: the forwards and
            // backwards scans still compute the span
            "aa[^a]*a, xaaa",
            "aa[^a]*a, xaabaax"
    })
    void matchesJdk(String regex, String input) {
        assertSameSpansAsJdk(regex, input, 0);
    }

    /**
     * Ordered alternation where the earlier alternative can continue past the point where the later alternative
     * completes: the forwards scan must keep scanning to the earlier alternative's end. This regressed when the
     * subset construction pruned the continuing thread because the accepting thread carried the shared Match
     * instruction's priority instead of the precedence of the path that reached it.
     */
    @ParameterizedTest
    @CsvSource({
            "(a[^a]*a|a), aba",
            "(a[^a]*a|a), xbaba",
            "(a[^a]*a|a), abababa"
    })
    void suffixSearchOrderedAlternationSpans(String regex, String input) {
        assertSameSpansAsJdk(regex, input, 0);
    }

    /**
     * Suffix-dense haystacks where a naive implementation rescans from every occurrence. Correctness at a size where
     * quadratic behavior would still finish, then timing growth between two larger sizes.
     */
    @ParameterizedTest
    @CsvSource({
            "'.*x', x",
            "'[^x]*x', xa",
            "'ab.*y', aby",
            "'a[^a]*a', ab"
    })
    void suffixDenseHaystacksStayLinear(String regex, String unit) throws Exception {
        // correctness at a size where even quadratic work completes quickly
        int checkSize = 2_000;
        String checkInput = buildInput(unit, checkSize);
        assertSameSpansAsJdk(regex, checkInput, 0);

        long t1 = timeFind(regex, buildInput(unit, 20_000));
        long t2 = timeFind(regex, buildInput(unit, 40_000));
        // doubling the input should not quadruple the time; generous multipliers absorb JIT and GC noise
        assertTrue(t2 < t1 * 4 + 250_000_000L,
                regex + ": time grew super-linearly: t(20k)=" + t1 + "ns, t(40k)=" + t2 + "ns");
        assertTrue(t2 < 3_000_000_000L, regex + ": 40k input took " + t2 + "ns");
    }

    private static String buildInput(String unit, int repetitions) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < repetitions; i++) {
            sb.append(unit);
        }
        return sb.toString();
    }

    private static final java.util.concurrent.atomic.AtomicInteger BENCH_COUNTER =
            new java.util.concurrent.atomic.AtomicInteger();

    /**
     * Times the compiled class's own search loop, below the Pattern layer.
     */
    private static long timeFind(String regex, String input) throws Exception {
        byte[] bytes = DFACompiler.compileToBytes(regex, "SuffixSearchBench" + BENCH_COUNTER.incrementAndGet(), 0);
        Class<?> cls = com.justinblank.classloader.MyClassLoader.getInstance()
                .loadClass("SuffixSearchBench" + BENCH_COUNTER.get(), bytes);
        // run twice, keep the second measurement to skip warmup
        long elapsed = 0;
        for (int round = 0; round < 2; round++) {
            Object matcher = cls.getDeclaredConstructor(String.class).newInstance(input);
            long start = System.nanoTime();
            var find = cls.getMethod("find");
            int count = 0;
            while ((Boolean) find.invoke(matcher)) {
                count++;
            }
            elapsed = System.nanoTime() - start;
            if (round == 1) {
                assertTrue(count > 0, regex + " should match");
            }
        }
        return elapsed;
    }
}
