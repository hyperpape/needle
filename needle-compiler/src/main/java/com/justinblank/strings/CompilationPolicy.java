package com.justinblank.strings;

import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

class CompilationPolicy {

    // TODO: These parameters are all set haphazardly. In the future, it will probably be worthwhile to systematically
    // investigate the ideal values. Any similar numeric cutoffs cutoffs should be set here so that we can easily modify
    // them for performance testing.
    public static final int MIN_OFFSET_LENGTH = 3;
    public static final int WAS_ACCEPTED_HASHSET_SIZE_THRESHOLD = 5;
    public static final int PREDICATE_RANGE_SIZE_CUTOFF = 6;
    public static final int FACTORIZATION_MAX_CHAR_RANGE_SIZE = 4;
    public static final int FACTORIZATION_MAX_REPETITION_COUNT = 2;

    // Whether any of the states for this regex use byte classes. For DFAs that do not treat all non-ascii characters
    // identically, some states may still only transition on an ascii character. For those states, we can potentially
    // use byteclasses, so we need to separately track whether any state uses byteclasses, and whether all states use
    // byteclasses.
    boolean usedByteClasses = false;
    boolean useByteClassesForAllStates = false;

    String suffix;
    String infix;

    boolean usePrefix;
    boolean useSuffix;
    // the variable name is based on the idea we may use multiple infixes in the future. For now, we aren't.
    boolean useInfixes;
    // Whether to check that we have enough distance to fully match our string on each outer loop--if our regex is short
    // This almost certainly won't be worthwhile. If our regex is long, then we can avoid looking at many characters
    boolean useMaxStart;

    public CompilationPolicy() {
    }

    public static CompilationPolicy create(Factorization factorization) {
        var compilationPolicy = new CompilationPolicy();
        compilationPolicy.usePrefix = factorization.getSharedPrefix().map(StringUtils::isNotEmpty).orElse(false);
        compilationPolicy.useSuffix = factorization.getSharedSuffix().map(suffix ->
                factorization.getMaxLength().isPresent()
        ).orElse(false) && !factorization.getSharedSuffix().equals(factorization.getSharedPrefix());;
        compilationPolicy.useInfixes = !factorization.getRequiredInfixes().isEmpty() && factorization.getMaxLength().isPresent();
        compilationPolicy.useMaxStart = factorization.getMinLength() > 4;

        if (compilationPolicy.useSuffix) {
            compilationPolicy.suffix = factorization.getSharedSuffix().orElse(null);
        }
        if (compilationPolicy.useInfixes) {
            compilationPolicy.infix = chooseInfix(factorization);
        }
        return compilationPolicy;
    }

    private static String chooseInfix(Factorization factorization) {
        var infixes = factorization.getRequiredInfixes();
        String bestInfix = null;
        for (var infix : infixes) {
            if (bestInfix == null) {
                bestInfix = infix;
            }
            else if (infix.length() > bestInfix.length()) {
                bestInfix = infix;
            }
        }
        return bestInfix;
    }

    public Optional<String> getSuffix() {
        return Optional.ofNullable(suffix);
    }

    public Optional<String> getInfix() {
        return Optional.of(infix);
    }

}
