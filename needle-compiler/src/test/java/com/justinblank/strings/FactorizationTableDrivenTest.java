package com.justinblank.strings;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactorizationTableDrivenTest {

    @ParameterizedTest
    @MethodSource("provideTestSpecs")
    void factorizationTests(FactorizationTestSpec spec) {
        try {
            var node = RegexParser.parse(spec.pattern);
            var factorization = node.bestFactors();

            // Check prefixes - handle both null and empty set
            Set<String> prefixSet = factorization.getPrefixes();
            boolean expectPrefixEmptyOrNull = spec.expectedPrefixes == null || spec.expectedPrefixes.isEmpty();
            if (expectPrefixEmptyOrNull) {
                if (prefixSet != null && !prefixSet.isEmpty()) {
                    fail("Expected empty or null prefixes for pattern: " + spec.pattern);
                }
            } else {
                assertNotNull(prefixSet, "Expected non-null prefixes for pattern: " + spec.pattern);
                assertNotNull(spec.expectedPrefixes, "Expected non-null prefixes list for pattern: " + spec.pattern);
                assertEquals(spec.expectedPrefixes.size(), prefixSet.size(),
                        "Prefix size mismatch for pattern: " + spec.pattern);
                for (String expected : spec.expectedPrefixes) {
                    assertTrue(prefixSet.contains(expected),
                            "Expected prefix '" + expected + "' not found in pattern: " + spec.pattern);
                }
            }

            // Check suffixes
            Set<String> suffixSet = factorization.getSuffixes();
            boolean expectSuffixEmptyOrNull = spec.expectedSuffixes == null || spec.expectedSuffixes.isEmpty();
            if (expectSuffixEmptyOrNull) {
                if (suffixSet != null && !suffixSet.isEmpty()) {
                    fail("Expected empty or null suffixes for pattern: " + spec.pattern);
                }
            } else {
                assertNotNull(suffixSet, "Expected non-null suffixes for pattern: " + spec.pattern);
                assertNotNull(spec.expectedSuffixes, "Expected non-null suffixes list for pattern: " + spec.pattern);
                assertEquals(spec.expectedSuffixes.size(), suffixSet.size(),
                        "Suffix size mismatch for pattern: " + spec.pattern);
                for (String expected : spec.expectedSuffixes) {
                    assertTrue(suffixSet.contains(expected),
                            "Expected suffix '" + expected + "' not found in pattern: " + spec.pattern);
                }
            }

            // Check factors
            Set<String> factorSet = factorization.getFactors();
            boolean expectFactorEmptyOrNull = spec.expectedFactors == null || spec.expectedFactors.isEmpty();
            if (expectFactorEmptyOrNull) {
                if (factorSet != null && !factorSet.isEmpty()) {
                    fail("Expected empty or null factors for pattern: " + spec.pattern);
                }
            } else {
                assertNotNull(factorSet, "Expected non-null factors for pattern: " + spec.pattern);
                assertNotNull(spec.expectedFactors, "Expected non-null factors list for pattern: " + spec.pattern);
                assertEquals(spec.expectedFactors.size(), factorSet.size(),
                        "Factor size mismatch for pattern: " + spec.pattern);
                for (String expected : spec.expectedFactors) {
                    assertTrue(factorSet.contains(expected),
                            "Expected factor '" + expected + "' not found in pattern: " + spec.pattern);
                }
            }

            // Check 'all' field
            Set<String> allSet = factorization.getAll();
            boolean expectAllEmptyOrNull = spec.expectedAll == null || spec.expectedAll.isEmpty();
            if (expectAllEmptyOrNull) {
                if (allSet != null && !allSet.isEmpty()) {
                    fail("Expected empty or null 'all' for pattern: " + spec.pattern);
                }
            } else {
                assertNotNull(allSet, "Expected non-null 'all' for pattern: " + spec.pattern);
                assertNotNull(spec.expectedAll, "Expected non-null 'all' list for pattern: " + spec.pattern);
                assertEquals(spec.expectedAll.size(), allSet.size(),
                        "'all' size mismatch for pattern: " + spec.pattern);
                for (String expected : spec.expectedAll) {
                    assertTrue(allSet.contains(expected),
                            "Expected '" + expected + "' not found in 'all' for pattern: " + spec.pattern);
                }
            }

            // Check required-factors
            Set<String> requiredSet = factorization.getRequiredFactors();
            boolean expectRequiredEmptyOrNull = spec.expectedRequiredFactors == null || spec.expectedRequiredFactors.isEmpty();
            if (expectRequiredEmptyOrNull) {
                if (requiredSet != null && !requiredSet.isEmpty()) {
                    fail("Expected empty or null required-factors for pattern: " + spec.pattern);
                }
            } else {
                assertNotNull(requiredSet, "Expected non-null required-factors for pattern: " + spec.pattern);
                assertNotNull(spec.expectedRequiredFactors, "Expected non-null required-factors list for pattern: " + spec.pattern);
                assertEquals(spec.expectedRequiredFactors.size(), requiredSet.size(),
                        "Required-factors size mismatch for pattern: " + spec.pattern);
                for (String expected : spec.expectedRequiredFactors) {
                    assertTrue(requiredSet.contains(expected),
                            "Expected required factor '" + expected + "' not found in pattern: " + spec.pattern);
                }
            }

            // Check shared-prefix
            Optional<String> sharedPrefix = factorization.getSharedPrefix();
            if (!spec.expectedSharedPrefix.isPresent() && sharedPrefix.isPresent()) {
                fail("Expected empty shared prefix for pattern: " + spec.pattern);
            } else {
                assertEquals(spec.expectedSharedPrefix, sharedPrefix,
                        "Shared prefix mismatch for pattern: " + spec.pattern);
            }

            // Check shared-suffix
            Optional<String> sharedSuffix = factorization.getSharedSuffix();
            if (!spec.expectedSharedSuffix.isPresent() && sharedSuffix.isPresent()) {
                fail("Expected empty shared suffix for pattern: " + spec.pattern);
            } else {
                assertEquals(spec.expectedSharedSuffix, sharedSuffix,
                        "Shared suffix mismatch for pattern: " + spec.pattern);
            }
        } catch (Exception e) {
            fail("Test failed for pattern: " + spec.pattern, e);
        }
    }

    static List<FactorizationTestSpec> provideTestSpecs() throws Exception {
        return new FactorizationTestSpecParser().readTests();
    }
}
