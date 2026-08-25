package com.justinblank.strings;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.tuple.Pair;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that repeated calls to Matcher#find() return the same sequence of (start, end) pairs as the JDK, using
 * src/test/resources/iteratedMatches.txt as the source of patterns and expected match sequences.
 */
public class IteratedMatchTests {

    private static final AtomicInteger CLASS_NAME_COUNTER = new AtomicInteger();

    private static final Map<Pair<String, Integer>, Pattern> PATTERNS = new HashMap<>();

    private static Pattern patternFor(IteratedMatchTestSpec spec) {
        int flags = spec.flags != null ? spec.flags.flags : 0;
        return PATTERNS.computeIfAbsent(Pair.of(spec.pattern, flags),
                (p) -> DFACompiler.compile(spec.pattern, "iteratedMatchTests" + CLASS_NAME_COUNTER.incrementAndGet(), flags));
    }

    @Test
    void canReadIteratedMatchTests() throws Exception {
        var specs = new RegexTestSpecParser().readIteratedMatchTests();
        assertFalse(specs.isEmpty());
    }

    @Test
    void jdkIteratedMatchFileBasedTests() throws Exception {
        var errors = new ArrayList<String>();
        for (var spec : new RegexTestSpecParser().readIteratedMatchTests()) {
            int flags = spec.flags != null ? spec.flags.flags : 0;
            var javaMatcher = java.util.regex.Pattern.compile(spec.pattern, flags).matcher(spec.target);
            var index = 0;
            for (var expected : spec.matches) {
                if (!javaMatcher.find()) {
                    errors.add("JDK failed to match pattern='" + spec.pattern + "' with flags=" + flags + " against haystack='" + spec.target +
                            "': expected match " + index + " at " + expected + ", but find() returned false");
                    break;
                }
                if (javaMatcher.start() != expected.start || javaMatcher.end() != expected.end) {
                    errors.add("JDK match for pattern='" + spec.pattern + "' with flags=" + flags + " against haystack='" + spec.target +
                            "': expected " + expected + ", actual=(" + javaMatcher.start() + "," + javaMatcher.end() + ")");
                }
                index++;
            }
            if (javaMatcher.find()) {
                errors.add("JDK found extra matches for pattern='" + spec.pattern + "' with flags=" + flags + " against haystack='" + spec.target +
                        "': expected " + spec.matches.size() + " matches, but found one more at (" + javaMatcher.start() + "," + javaMatcher.end() + ")");
            }
        }
        if (!errors.isEmpty()) {
            fail(String.join("\n", errors));
        }
    }

    @Test
    void needleIteratedMatchFileBasedTests() throws Exception {
        var errors = new ArrayList<String>();
        for (var spec : new RegexTestSpecParser().readIteratedMatchTests()) {
            int flags = spec.flags != null ? spec.flags.flags : 0;
            try {
                var matcher = patternFor(spec).matcher(spec.target);
                var actual = new ArrayList<IteratedMatchTestSpec.Match>();
                var terminated = true;
                while (true) {
                    var found = matcher.find();
                    if (!found) {
                        break;
                    }
                    actual.add(new IteratedMatchTestSpec.Match(matcher.start(), matcher.end()));
                    // Each match must advance the search position, so a pattern can produce at most one match per
                    // position of the haystack, plus one trailing empty match
                    if (actual.size() > spec.target.length() + 1) {
                        terminated = false;
                        break;
                    }
                }
                if (!terminated) {
                    errors.add("needle iteration failed to terminate for pattern='" + spec.pattern + "' with flags=" + flags + " against haystack='" + spec.target +
                            "': matches so far were " + actual);
                } else if (!actual.equals(spec.matches)) {
                    errors.add("needle iterated matching of pattern='" + spec.pattern + "' with flags=" + flags + " against haystack='" + spec.target +
                            "' had incorrect matches: expected " + spec.matches + ", actual " + actual);
                }
            }
            // Throwable to catch ExceptionInInitializerError
            catch (Throwable t) {
                errors.add("needle matching spec='" + spec.pattern + "' with flags=" + flags + " against string=" + spec.target + " threw " + t);
            }
        }
        if (!errors.isEmpty()) {
            fail(String.join("\n", errors));
        }
    }

}
