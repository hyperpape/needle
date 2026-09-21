package com.justinblank.strings;

import java.util.List;
import java.util.Optional;

public class FactorizationTestSpec {

    final String pattern;
    final List<String> expectedPrefixes;
    final List<String> expectedSuffixes;
    final List<String> expectedFactors;
    final List<String> expectedAll;
    final List<String> expectedRequiredFactors;
    final Optional<String> expectedSharedPrefix;
    final Optional<String> expectedSharedSuffix;

    public FactorizationTestSpec(String pattern, 
                                  List<String> expectedPrefixes,
                                  List<String> expectedSuffixes,
                                  List<String> expectedFactors,
                                  List<String> expectedAll,
                                  List<String> expectedRequiredFactors,
                                  Optional<String> expectedSharedPrefix,
                                  Optional<String> expectedSharedSuffix) {
        this.pattern = pattern;
        this.expectedPrefixes = expectedPrefixes;
        this.expectedSuffixes = expectedSuffixes;
        this.expectedFactors = expectedFactors;
        this.expectedAll = expectedAll;
        this.expectedRequiredFactors = expectedRequiredFactors;
        this.expectedSharedPrefix = expectedSharedPrefix;
        this.expectedSharedSuffix = expectedSharedSuffix;
    }
}
