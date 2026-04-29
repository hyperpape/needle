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
    }
}