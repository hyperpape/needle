package com.justinblank.strings;

import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

class CompilationPolicy {

    boolean usedByteClasses = false;
    boolean useByteClassesForAllStates = false;

    // 6 is a wild guess
    final int predicateRangeSizeCutoff = 6;

    String suffix;

    boolean shouldSeekForward;
    boolean useSuffix;
    boolean useMaxStart;

    public CompilationPolicy() {
    }

    public static CompilationPolicy create(Factorization factorization) {
        var compilationPolicy = new CompilationPolicy();
        compilationPolicy.shouldSeekForward = factorization.getSharedPrefix().map(StringUtils::isNotEmpty).orElse(false);
        boolean useSuffix = factorization.getSharedSuffix().map(suffix ->
                factorization.getMaxLength().isPresent()
        ).orElse(false) && !factorization.getSharedSuffix().equals(factorization.getSharedPrefix());
        compilationPolicy.useSuffix = useSuffix;
        compilationPolicy.useMaxStart = factorization.getMinLength() > 4;
        return compilationPolicy;
    }

    public Optional<String> getSuffix() {
        return Optional.ofNullable(suffix);
    }

}
