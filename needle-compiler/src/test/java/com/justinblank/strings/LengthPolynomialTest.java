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
    void optional() {
        var node = RegexParser.parse("(\\d{3}-)?");
        var polynomial = node.lengthPolynomial();
        assertThat(polynomial).isPresent();
    }

    @Test
    void countedRepetition() {
        var node = RegexParser.parse("(b{3,4})");
        var polynomial = node.lengthPolynomial();
        assertThat(polynomial).isPresent();
    }

    @Test
    void polynomial_withOptional() {
        var node = RegexParser.parse("(\\d{3}-)?\\d{3}-\\d{4}");
        var polynomial = node.lengthPolynomial();
        assertThat(polynomial).isPresent();
    }

}
