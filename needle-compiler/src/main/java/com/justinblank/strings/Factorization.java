package com.justinblank.strings;

import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class containing data for the computation of the factors of a regular expression, roughly based on the scheme
 * introduced in Navarro and Rafinot, Flexible Pattern Matching in Strings section 5.5.2.
 *
 * This class is mutated by operations on it.
 */
public class Factorization {

    // TODO: measure effect of this
    public static final int MAX_CHAR_RANGE_FACTORS = 4;
    // TODO: tune this
    public static final int MAX_REPETITION_COUNT = 2;

    // Note: none of these sets are ever internally mutated, outside of the constructors.

    /**
     * The set of all factors that can be matched by the regular expression.
     */
    private Set<String> all;
    /**
     * The set of all suffixes that the regular expression can have.
     */
    private Set<String> suffixes;
    /**
     * The set of all prefixes that the regular expression can have.
     */
    private Set<String> prefixes;
    /**
     * A set of factors such that one of them must be present.
     */
    private Set<String> factors;

    private int minLength = 0;
    private int maxLength = -1;

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

    static Factorization copy(Factorization factorization) {
        return new Factorization(factorization.all, factorization.suffixes, factorization.prefixes, factorization.factors);
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

    /**
     * Union does set union on all the components of a factorization. For any component, if either side is null,
     * then the result is null.
     * @param factorization the other factorization
     */
    public void union(Factorization factorization) {
        if (this.all == null || factorization.all == null) {
            all = null;
        }
        else {
            Set<String> newAll = new HashSet<>(all);
            newAll.addAll(factorization.all);
            all = newAll;
        }
        if (this.prefixes == null || factorization.prefixes == null) {
            prefixes = null;
        }
        else {
            Set<String> newPrefixes = new HashSet<>(prefixes);
            newPrefixes.addAll(factorization.prefixes);
            prefixes = newPrefixes;
        }
        if (this.suffixes == null || factorization.suffixes == null) {
            suffixes = null;
        }
        else {
            Set<String> newSuffixes = new HashSet<>(suffixes);
            newSuffixes.addAll(factorization.suffixes);
            suffixes = newSuffixes;
        }
        if (this.factors == null || factorization.factors == null) {
            factors = null;
        }
        else {
            Set<String> newFactors = new HashSet<>(factors);
            newFactors.addAll(factorization.factors);
            factors = newFactors;
        }
    }

    public void concatenate(Factorization factorization) {
        var localSuffixes = suffixes;
        var localPrefixes = prefixes;
        var localFactors = factors;
        var localAll = all;

        if (localAll == null || factorization.all == null) {
            all = null;
        }
        else {
            all = concatenateStrings(localAll, factorization.all);
        }
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
    
    public boolean isComplete() {
        return this.all != null && !this.all.isEmpty();
    }

    public Factorization countedRepetition(int min, int max) {
        Factorization factorization = this;
        if (max > MAX_REPETITION_COUNT) {
            return empty();
        }
        if (min == 0) {
            factorization = fromString("");
        }
        for (int i = min; i <= max; i++) {
            Factorization newFactors = fromString("");
            for (int j = 0; j < i; j++) {
                newFactors.concatenate(this);
            }
            factorization.union(newFactors);
        }
        return factorization;
    }

    public Optional<List<Character>> getInitialChars() {
        if (prefixes == null || prefixes.isEmpty()) {
            return Optional.empty();
        }
        Set<Character> prefixChars = new HashSet<>();
        for (String prefix : prefixes) {
            prefixChars.add(prefix.charAt(0));
        }
        List<Character> chars = new ArrayList<>(prefixChars);
        Collections.sort(chars);
        return Optional.of(chars);
    }

    public Optional<String> getSharedPrefix() {
        return getSharedPrefix(prefixes);
    }

    protected Optional<String> getSharedPrefix(Set<String> prefixes) {
        if (prefixes == null || prefixes.isEmpty()) {
            return Optional.empty();
        }
        if (prefixes.equals(Set.of(""))) {
            return Optional.empty();
        }
        String sharedPrefix = null;
        for (String prefix : prefixes) {
            if (sharedPrefix == null) {
                sharedPrefix = prefix;
            }
            else {
                int i = 0;
                for (; i < sharedPrefix.length() && i < prefix.length(); i++) {
                    if (sharedPrefix.charAt(i) != prefix.charAt(i)) {
                        break;
                    }
                }
                if (i == 0) {
                    return Optional.empty();
                }
                else {
                    sharedPrefix = sharedPrefix.substring(0, i);
                }
            }
        }
        return Optional.of(sharedPrefix);
    }

    public Optional<String> getSharedSuffix() {
        var reversedSuffixes = getSuffixes().stream().map(StringUtils::reverse).collect(Collectors.toSet());
        return getSharedPrefix(reversedSuffixes).map(StringUtils::reverse);
    }

    public int getMinLength() {
        return minLength;
    }

    public void setMinLength(int minLength) {
        if (minLength < 0) {
            throw new IllegalArgumentException("min length must be >= 0");
        }
        this.minLength = minLength;
    }

    public void setMaxLength(int maxLength) {
        if (maxLength < 0) {
            throw new IllegalArgumentException("max length must be >= 0");
        }
        this.maxLength = maxLength;
    }

    public Optional<Integer> getMaxLength() {
        if (maxLength == -1) {
            return Optional.empty();
        }
        return Optional.of(maxLength);
    }

    @Override
    public String toString() {
        return "Factorization{" +
                "all=" + all +
                ", suffixes=" + suffixes +
                ", prefixes=" + prefixes +
                ", factors=" + factors +
                '}';
    }
}
