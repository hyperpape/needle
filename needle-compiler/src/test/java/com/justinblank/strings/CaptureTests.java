package com.justinblank.strings;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class CaptureTests {

    @Test
    void canReadCaptureTests() throws Exception {
        List<CaptureTestSpec> specs = new RegexTestSpecParser().readCaptureTests();
        assertFalse(specs.isEmpty());
    }

    @Test
    void fileBasedTests() throws Exception {
        List<CaptureTestSpec> specs = new RegexTestSpecParser().readCaptureTests();
        List<String> failures = new ArrayList<>();
        for (var spec : specs) {
            var javaPattern = java.util.regex.Pattern.compile(spec.pattern, spec.flags != null ? spec.flags.flags : 0);
            var javaMatcher = javaPattern.matcher(spec.target);
            assertTrue(javaMatcher.find());

            if (spec.start != javaMatcher.start() || spec.end != javaMatcher.end()) {
                String failureMessage = "Incorrect match for pattern='" + spec.pattern + "' with haystack='" + spec.target + "', expected=(" + spec.start + "," + spec.end + "'), actual='(" + javaMatcher.start() + "," + javaMatcher.end() + ")";
                failures.add(failureMessage);
            }
            assertEquals(spec.captures.size(), javaMatcher.groupCount());
            for (int i = 0; i < spec.captures.size(); i++) {
                String capture = spec.captures.get(i);
                if (!Objects.equals(capture, javaMatcher.group(i + 1))) {
                    String failureMessage = "Incorrect capture " + (i + 1) + " for pattern='" + spec.pattern + "' with haystack='" + spec.target + "', expected='" + javaMatcher.group(i + 1) + "', actual='" + capture + "'";
                    failures.add(failureMessage);
                }
            }
        }
        if (!failures.isEmpty()) {
            for (var failure : failures) {
                System.err.println(failure);
            }
            fail("Failures in file based capture tests");
        }
    }

    @Test
    void testRepetitionWithCapture() {
        String regex = "(AB)+A";
        java.util.regex.Pattern javaPattern = java.util.regex.Pattern.compile(regex);
        String target = "ABA";
        java.util.regex.Matcher javaMatcher = javaPattern.matcher(target);
        javaMatcher.find();
        assertEquals("ABA", javaMatcher.group(0));
        assertEquals("AB", javaMatcher.group(1));

        target = "ABABABA";
        javaMatcher = javaPattern.matcher(target);
        javaMatcher.find();
        assertEquals("ABABABA", javaMatcher.group(0));
        assertEquals("AB", javaMatcher.group(1));
        assertEquals(1, javaMatcher.groupCount());
    }

    @Test
    void testNestedCaptureWithRepetition() {
        String regex = "((AB)+)A";
        java.util.regex.Pattern javaPattern = java.util.regex.Pattern.compile(regex);
        String target = "ABA";
        java.util.regex.Matcher javaMatcher = javaPattern.matcher(target);
        javaMatcher.find();
        assertEquals("ABA", javaMatcher.group(0));
        assertEquals("AB", javaMatcher.group(1));

        target = "ABABABA";
        javaMatcher = javaPattern.matcher(target);
        javaMatcher.find();
        assertEquals("ABABABA", javaMatcher.group(0));
        assertEquals("ABABAB", javaMatcher.group(1));
        assertEquals("AB", javaMatcher.group(2));
        assertEquals(2, javaMatcher.groupCount());
    }
}
