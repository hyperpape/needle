package com.justinblank.strings;

import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

class CompilationPolicy {

    boolean usedByteClasses = false;
    boolean useByteClassesForAllStates = false;

    // 6 is a wild guess
    final int predicateRangeSizeCutoff = 6;

    String suffix;
    String infix;

    boolean usePrefix;
    boolean useSuffix;
    // TODO: the variable name is based on the idea we may use multiple infixes in the future. For now, we aren't.
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
