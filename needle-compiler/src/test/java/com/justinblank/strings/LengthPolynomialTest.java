package com.justinblank.strings;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LengthPolynomialTest {

    @Test
    void concatenationPolynomial() {
        var node = RegexParser.parse("\\d{3}-\\d{4}");
        var polynomial = node.lengthPolynomial();
        assertThat(polynomial).isPresent();
    }


    @Test
    void polynomial_withZeroCountedRepetition() {
        var node = RegexParser.parse("(\\d{3}-)?\\d{3}-\\d{4}");
        var polynomial = node.lengthPolynomial();
        assertThat(polynomial).isPresent();
    }

}
