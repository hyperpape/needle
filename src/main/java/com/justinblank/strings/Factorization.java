package com.justinblank.strings;

import java.util.HashSet;
import java.util.Set;

/**
 * Class containing data for the computation of the factors of a regular expression.
 *
 * This class is mutated by operations on it.
 */
public class Factorization {

    // TODO: measure effect of this
    public static final int MAX_CHAR_RANGE_FACTORS = 4;
    private Set<String> all;
    private Set<String> suffixes;
    private Set<String> prefixes;
    private Set<String> factors;

    private Factorization() {
        all = new HashSet<>();
        suffixes = new HashSet<>();
        prefixes = new HashSet<>();
        factors = new HashSet<>();
    }

    private Factorization(Set<String> all, Set<String> suffixes, Set<String> prefixes, Set<String> factors) {
        this.all = all;
        this.suffixes = suffixes;
        this.prefixes = prefixes;
        this.factors = factors;
    }

    private Factorization(char c) {
        this(String.valueOf(c));
    }

    private Factorization(String s) {
        this();
        all.add(s);
        suffixes.add(s);
        prefixes.add(s);
        factors.add(s);
    }

    public static Factorization fromChar(char c) {
        return new Factorization(c);
    }

    public static Factorization fromRange(char start, char end) {
        if ((int) end - (int) start > MAX_CHAR_RANGE_FACTORS) {
            return Factorization.empty();
        }
        else if (start == end) {
            return fromChar(start);
        }
        Set<String> strings = new HashSet<>();
        for (int i = start; i <= (int) end; i++) {
            char c = (char) i;
            strings.add(String.valueOf(c));
        }
        return new Factorization(strings, strings, strings, strings);
    }

    public static Factorization fromString(String string) {
        return new Factorization(string);
    }

    public static Factorization empty() {
        return new Factorization(null, null, null, null);
    }

    Set<String> getAll() {
        return all;
    }

    Set<String> getSuffixes() {
        return suffixes;
    }

    Set<String> getPrefixes() {
        return prefixes;
    }

    Set<String> getFactors() {
        return factors;
    }

    public void alternate(Factorization factorization) {
        if (this.all == null || factorization.all == null) {
            all = null;
        }
        else {
            all.addAll(factorization.all);
        }
        if (this.prefixes == null || factorization.prefixes == null) {
            prefixes = null;
        }
        else {
            prefixes.addAll(factorization.prefixes);
        }
        if (this.suffixes == null || factorization.suffixes == null) {
            suffixes = null;
        }
        else {
            suffixes.addAll(factorization.suffixes);
        }
        if (this.factors == null || factorization.factors == null) {
            factors = null;
        }
        else {
            factors.addAll(factorization.factors);
        }
    }

    public void concatenate(Factorization factorization) {
        var localSuffixes = suffixes;
        var localPrefixes = prefixes;
        var localFactors = factors;
        var localAll = all;

        all = concatenateStrings(localAll, factorization.all);
        prefixes = best(localPrefixes, concatenateStrings(localAll, factorization.prefixes));
        suffixes = best(factorization.suffixes, concatenateStrings(localSuffixes, factorization.all));
        var firstFactors = best(localFactors, factorization.factors);
        factors = best(firstFactors, concatenateStrings(localSuffixes, factorization.prefixes));
    }

    /**
     * Take two sets of strings, returning a new set of strings consisting of s1 + s2 where s1 is in the first set, and
     * s2 is in the second.
     *
     * If either set is null, the other set is returned. The result may be null.
     *
     * @param set1 a set of strings
     * @param set2 a set of strings
     * @return the set of concatenated strings
     */
    private static Set<String> concatenateStrings(Set<String> set1, Set<String> set2) {
        if (null == set1 || null == set2) {
            return new HashSet<>();
        }
        else if (set1.isEmpty()) {
            return set1;
        }
        else if (set2.isEmpty()) {
            return set2;
        }
        Set<String> newFactors = new HashSet<>();
        for (String factor : set1) {
            for (String rightFactor : set2) {
                newFactors.add(factor + rightFactor);
            }
        }
        return newFactors;
    }

    /**
     * Given two sets of factors, uses heuristics to determine which is better. In general, a smaller set of
     * longer factors is preferred.
     *
     * @param set1 the first set of factors
     * @param set2 the second set of factors
     * @return the better set of factors
     */
    static Set<String> best(Set<String> set1, Set<String> set2) {
        if (set1 == null || set1.isEmpty()) {
            return set2;
        }
        else if (set2 == null || set2.isEmpty()) {
            return set1;
        }
        // TODO: this can be better
        if (set1.size() > set2.size()) {
            int minSize1 = set1.stream().mapToInt(String::length).min().orElse(Integer.MAX_VALUE);
            int maxSize2 = set2.stream().mapToInt(String::length).max().orElse(Integer.MAX_VALUE);
            if (minSize1 <= maxSize2) {
                return set2;
            }
        }
        else if (set2.size() > set1.size()) {
            int maxSize1 = set1.stream().mapToInt(String::length).max().orElse(Integer.MAX_VALUE);
            int minSize2 = set2.stream().mapToInt(String::length).min().orElse(Integer.MAX_VALUE);
            if (minSize2 <= maxSize1) {
                return set1;
            }
        }
        int count1 = set1.stream().mapToInt(String::length).sum();
        int count2 = set2.stream().mapToInt(String::length).sum();
        if (count2 > count1) {
            return set2;
        }
        return set1;
    }

    public boolean hasFactors() {
        return this.factors != null && !this.factors.isEmpty();
    }

    public boolean isComplete() {
        return this.all != null && !this.all.isEmpty();
    }
}
