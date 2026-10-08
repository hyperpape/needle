package com.justinblank.strings.RegexAST;

import org.junit.jupiter.api.Test;

import static com.justinblank.strings.RegexAST.LengthPolynomial.gcd;
import static org.junit.jupiter.api.Assertions.*;

class LengthPolynomialTest {

    @Test
    void gcd_doesGcdThings() {
        assertEquals(2, gcd(4, 2));
        assertEquals(2, gcd(2, 4));
        assertEquals(1, gcd(3, 5));
        assertEquals(1, gcd(5, 3));
        assertEquals(1, gcd(2, 5));
        assertEquals(1, gcd(5, 2));

        assertEquals(12, gcd(36, 96));
        assertEquals(12, gcd(96, 36));

        assertEquals(2, gcd(0, 2));
        assertEquals(2, gcd(2, 0));
        assertEquals(0, gcd(0, 0));
    }

    @Test
    void constant_onlyMatchesOneLength() {
        var polynomial = LengthPolynomial.constant(3);
        assertTrue(polynomial.canMatch(3));
        assertFalse(polynomial.canMatch(2));
        assertFalse(polynomial.canMatch(4));
        assertEquals(3, polynomial.minLength());
        assertEquals(3, polynomial.maxLength());
        assertEquals(0, polynomial.gcdOfSizes());
    }

    @Test
    void singleComponentChecksBoundsAndResidue() {
        var polynomial = LengthPolynomial.constant(2).plus(LengthPolynomial.constant(1).multi(2, 3).orElseThrow());
        // lengths are 2 + k for k in [2, 3]: {4, 5}
        assertEquals(4, polynomial.minLength());
        assertEquals(5, polynomial.maxLength());
        assertTrue(polynomial.canMatch(4));
        assertTrue(polynomial.canMatch(5));
        assertFalse(polynomial.canMatch(3));
        assertFalse(polynomial.canMatch(6));
    }

    @Test
    void residueIsCheckedForEachComponentSize() {
        // lengths are 2*k for k in [1, 3]: {2, 4, 6}
        var polynomial = LengthPolynomial.constant(2).multi(1, 3).orElseThrow();
        assertEquals(2, polynomial.minLength());
        assertEquals(6, polynomial.maxLength());
        assertEquals(2, polynomial.gcdOfSizes());
        assertTrue(polynomial.canMatch(2));
        assertTrue(polynomial.canMatch(4));
        assertTrue(polynomial.canMatch(6));
        assertFalse(polynomial.canMatch(3));
        assertFalse(polynomial.canMatch(5));
        assertFalse(polynomial.canMatch(1));
    }

    @Test
    void plusMergesLikeSizedComponents() {
        // (b in [2,3]) concatenated with (b in [4,5]): b in [6,8]
        var left = LengthPolynomial.constant(1).multi(2, 3).orElseThrow();
        var right = LengthPolynomial.constant(1).multi(4, 5).orElseThrow();
        var polynomial = left.plus(right);
        assertEquals(6, polynomial.minLength());
        assertEquals(8, polynomial.maxLength());
        assertEquals(1, polynomial.gcdOfSizes());
        assertTrue(polynomial.canMatch(6));
        assertTrue(polynomial.canMatch(7));
        assertTrue(polynomial.canMatch(8));
        assertFalse(polynomial.canMatch(5));
        assertFalse(polynomial.canMatch(9));
    }

    @Test
    void unboundedRepetitionHasNoUpperBoundButStillChecksResidue() {
        // (ab)*: even lengths, unbounded
        var polynomial = LengthPolynomial.constant(2).multi(0, -1).orElseThrow();
        assertEquals(0, polynomial.minLength());
        assertEquals(-1, polynomial.maxLength());
        assertEquals(2, polynomial.gcdOfSizes());
        assertTrue(polynomial.canMatch(0));
        assertTrue(polynomial.canMatch(2));
        assertTrue(polynomial.canMatch(100));
        assertFalse(polynomial.canMatch(3));
    }

    @Test
    void timesZeroIsConstantZero() {
        var polynomial = LengthPolynomial.constant(5).times(0);
        assertTrue(polynomial.canMatch(0));
        assertFalse(polynomial.canMatch(1));
        assertFalse(polynomial.canMatch(5));
    }
}
