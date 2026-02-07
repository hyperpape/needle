package com.justinblank.strings.RegexAST;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.TreeMap;

/**
 * An over-approximation of the set of lengths that a node can match, of the form
 * {@code constant + sum(size_i * x_i)} where each {@code x_i} is an integer in {@code [min_i, max_i]}.
 *
 * <p>A component's {@code max} of -1 means the term is unbounded, and a component's {@code size} of 0 is meaningless
 * and is discarded. Components are kept sorted by size with a single entry per size, so that concatenation can merge
 * like terms.
 *
 * <p>This is used as a pre-filter: it is always sound to reject a length that {@link #canMatch(int)} rejects, but it
 * may accept lengths that the expression cannot actually match. The abstraction is widened only by
 * {@link #multi(int, int)}; {@link #plus(LengthPolynomial)} and {@link #times(int)} describe their operands exactly.
 */
public class LengthPolynomial {

    private final List<LengthComponent> components;
    private final int constant;

    private LengthPolynomial(List<LengthComponent> components, int constant) {
        this.components = components;
        this.constant = constant;
    }

    public static LengthPolynomial constant(int n) {
        return new LengthPolynomial(Collections.emptyList(), n);
    }

    public LengthPolynomial times(int n) {
        if (n == 0) {
            return constant(0);
        }
        var variables = new ArrayList<LengthComponent>(components.size());
        for (LengthComponent c : components) {
            variables.add(c.times(n));
        }
        return new LengthPolynomial(normalize(variables), n * constant);
    }

    public LengthPolynomial plus(LengthPolynomial other) {
        var variables = new ArrayList<LengthComponent>(components.size() + other.components.size());
        variables.addAll(components);
        variables.addAll(other.components);
        return new LengthPolynomial(normalize(variables), constant + other.constant);
    }

    /**
     * Build a polynomial describing this polynomial repeated between min and max times. A max of -1 means the
     * repetition is unbounded above.
     *
     * <p>This is the only operation that over-approximates. The real length of {@code P{min,max}} is
     * {@code sum over k in [min,max] of (c*k + sum_i s_i * (x_i,1 + ... + x_i,k))}, where the same k applies to every
     * term. The returned polynomial instead lets the constant and each component scale by an independent repetition
     * count, and replaces the (possibly non-contiguous) set of sums for a component with the interval
     * {@code [min * component.min, max * component.max]}. For example, {@code (b{3,4})?} can match lengths {0,3,4},
     * but this produces a component of {@code [0 * 3, 1 * 4] = [0,4]}. The result is always a superset, so it can only
     * cause false accepts.
     */
    public Optional<LengthPolynomial> multi(int min, int max) {
        var variables = new ArrayList<LengthComponent>();
        if (constant != 0) {
            variables.add(new LengthComponent(min, max, constant));
        }
        for (var component : components) {
            variables.add(new LengthComponent(component.min * min,
                    LengthComponent.multiplyMax(component.max, max), component.size));
        }
        return Optional.of(new LengthPolynomial(normalize(variables), 0));
    }

    private static List<LengthComponent> normalize(List<LengthComponent> raw) {
        var bySize = new TreeMap<Integer, LengthComponent>();
        for (var component : raw) {
            if (component.size <= 0) {
                continue;
            }
            bySize.merge(component.size, component,
                    (left, right) -> new LengthComponent(left.min + right.min, mergeMax(left.max, right.max), left.size));
        }
        return new ArrayList<>(bySize.values());
    }

    private static int mergeMax(int left, int right) {
        if (left == -1 || right == -1) {
            return -1;
        }
        return left + right;
    }

    public int getConstant() {
        return constant;
    }

    /**
     * @return the smallest length that can satisfy this polynomial
     */
    public int minLength() {
        long min = constant;
        for (var component : components) {
            min += (long) component.size * component.min;
        }
        return clamp(min);
    }

    /**
     * @return the largest length that can satisfy this polynomial, or -1 if unbounded
     */
    public int maxLength() {
        long max = constant;
        for (var component : components) {
            if (component.max == -1) {
                return -1;
            }
            max += (long) component.size * component.max;
        }
        return clamp(max);
    }

    /**
     * @return the greatest common divisor of the sizes of the components, or 0 if there are no components
     */
    public int gcdOfSizes() {
        int g = 0;
        for (var component : components) {
            g = gcd(g, component.size);
        }
        return g;
    }

    /**
     * Return whether it's possible for a string of length {@code length} to satisfy this polynomial. This is a
     * necessary but not sufficient condition for a match.
     */
    public boolean canMatch(int length) {
        long remaining = (long) length - constant;
        if (remaining < 0) {
            return false;
        }
        long min = 0;
        long max = 0;
        boolean bounded = true;
        int g = 0;
        for (var component : components) {
            min += (long) component.size * component.min;
            if (component.max == -1) {
                bounded = false;
            }
            else {
                max += (long) component.size * component.max;
            }
            g = gcd(g, component.size);
        }
        if (remaining < min) {
            return false;
        }
        if (bounded && remaining > max) {
            return false;
        }
        return g <= 1 || remaining % g == 0;
    }

    private static int clamp(long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) value;
    }

    public static int gcd(int x, int y) {
        x = Math.abs(x);
        y = Math.abs(y);
        while (y != 0) {
            int tmp = x % y;
            x = y;
            y = tmp;
        }
        return x;
    }

    /**
     * An exact description of the lengths that satisfy this polynomial, in a form that can answer membership for a
     * single length with one array lookup.
     *
     * <p>A length {@code n} satisfies the polynomial iff {@code n >= base} and
     * {@code minRepresentable[(n - base) % modulus] <= n - base}. This relies on the fact that the components generate
     * a numerical semigroup, and it only holds when there is an unbounded component whose size equals the modulus
     * (guaranteed by {@link #membership(int)}).
     */
    public static final class Membership {

        private final int base;
        private final int modulus;
        private final int[] minRepresentable;

        Membership(int base, int modulus, int[] minRepresentable) {
            this.base = base;
            this.modulus = modulus;
            this.minRepresentable = minRepresentable;
        }

        public int base() {
            return base;
        }

        public int modulus() {
            return modulus;
        }

        public int[] minRepresentable() {
            return minRepresentable;
        }
    }

    /**
     * Compute an exact membership test for the lengths described by this polynomial, if one is small enough to embed.
     *
     * <p>The components contribute {@code size * x} terms. Shifting each term by its minimum leaves a numerical
     * semigroup generated by the component sizes. The Apéry set of that semigroup (the minimum representable value in
     * each residue class modulo the smallest generator) makes membership exact. Bounded components are only folded into
     * the semigroup when their range is large enough that the bound can never exclude the minimal representation; a
     * shortest path to a residue uses fewer than {@code modulus} generators, so a range of at least {@code modulus}
     * suffices.
     *
     * @param maxModulus the largest modulus for which to build a table
     */
    public Optional<Membership> membership(int maxModulus) {
        if (components.size() < 2) {
            return Optional.empty();
        }
        int modulus = Integer.MAX_VALUE;
        for (var component : components) {
            if (component.max == -1) {
                modulus = Math.min(modulus, component.size);
            }
        }
        if (modulus == Integer.MAX_VALUE || modulus > maxModulus) {
            return Optional.empty();
        }
        for (var component : components) {
            if (component.max != -1 && (long) component.max - component.min < modulus) {
                return Optional.empty();
            }
        }

        int[] minRepresentable = new int[modulus];
        Arrays.fill(minRepresentable, Integer.MAX_VALUE);
        minRepresentable[0] = 0;
        var queue = new PriorityQueue<long[]>(Comparator.comparingLong(entry -> entry[0]));
        queue.add(new long[] {0, 0});
        while (!queue.isEmpty()) {
            var current = queue.poll();
            long value = current[0];
            int residue = (int) current[1];
            if (value > minRepresentable[residue]) {
                continue;
            }
            for (var component : components) {
                long next = value + component.size;
                int nextResidue = (int) (next % modulus);
                long clamped = Math.min(next, Integer.MAX_VALUE);
                if (clamped < minRepresentable[nextResidue]) {
                    minRepresentable[nextResidue] = (int) clamped;
                    queue.add(new long[] {next, nextResidue});
                }
            }
        }
        return Optional.of(new Membership(minLength(), modulus, minRepresentable));
    }

    @Override
    public String toString() {
        return constant + " + " + components;
    }
}
