package com.justinblank.strings;

import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

class CompilationPolicy {

    boolean usedByteClasses = false;
    boolean useByteClassesForAllStates = false;

    // 6 is a wild guess
    final int predicateRangeSizeCutoff = 6;

    String suffix;

    boolean usePrefix;
    boolean useSuffix;
    boolean useInfixes;
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
        return compilationPolicy;
    }

    public Optional<String> getSuffix() {
        return Optional.ofNullable(suffix);
    }

}
