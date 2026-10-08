package com.justinblank.strings;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LengthPolynomialTest {

    @Test
    void concatenationPolynomial() {
        var node = RegexParser.parse("\\d{3}-\\d{4}");
        var optPolynomial = node.lengthPolynomial();
        assertThat(optPolynomial).isPresent();
        var polynomial = optPolynomial.get();
        assertThat(polynomial.getConstant()).isEqualTo(8);
        assertThat(polynomial.minLength()).isEqualTo(8);
        assertThat(polynomial.maxLength()).isEqualTo(8);
    }

    @Test
    void optional() {
        var node = RegexParser.parse("(\\d{3}-)?");
        var optPolynomial = node.lengthPolynomial();
        assertThat(optPolynomial).isPresent();
        var polynomial = optPolynomial.get();
        assertThat(polynomial.getConstant()).isEqualTo(0);
        assertThat(polynomial.minLength()).isEqualTo(0);
        assertThat(polynomial.maxLength()).isEqualTo(4);
        assertThat(polynomial.canMatch(0)).isTrue();
        assertThat(polynomial.canMatch(4)).isTrue();
        assertThat(polynomial.canMatch(2)).isFalse();
    }

    @Test
    void countedRepetition() {
        var node = RegexParser.parse("(b{3,4})");
        var polynomial = node.lengthPolynomial();
        assertThat(polynomial).isPresent();
        assertThat(polynomial.get().canMatch(3)).isTrue();
        assertThat(polynomial.get().canMatch(4)).isTrue();
        assertThat(polynomial.get().canMatch(2)).isFalse();
        assertThat(polynomial.get().canMatch(5)).isFalse();
    }

    @Test
    void optional_ofCountedRepetition() {
        var node = RegexParser.parse("(b{3,4})?");
        var polynomial = node.lengthPolynomial();
        assertThat(polynomial).isPresent();
        assertThat(polynomial.get().minLength()).isEqualTo(0);
        assertThat(polynomial.get().maxLength()).isEqualTo(4);
        assertThat(polynomial.get().canMatch(0)).isTrue();
        assertThat(polynomial.get().canMatch(1)).isTrue();
        assertThat(polynomial.get().canMatch(2)).isTrue();
        assertThat(polynomial.get().canMatch(3)).isTrue();
        assertThat(polynomial.get().canMatch(4)).isTrue();
        assertThat(polynomial.get().canMatch(5)).isFalse();
    }

    @Test
    void polynomial_withOptional() {
        var node = RegexParser.parse("(\\d{3}-)?\\d{3}-\\d{4}");
        var polynomial = node.lengthPolynomial();
        assertThat(polynomial).isPresent();
        assertThat(polynomial.get().minLength()).isEqualTo(8);
        assertThat(polynomial.get().maxLength()).isEqualTo(12);
        assertThat(polynomial.get().canMatch(8)).isTrue();
        assertThat(polynomial.get().canMatch(12)).isTrue();
        assertThat(polynomial.get().canMatch(9)).isFalse();
    }

    @Test
    void plusRepetitionIsUnbounded() {
        var node = RegexParser.parse("(ab)+");
        var polynomial = node.lengthPolynomial();
        assertThat(polynomial).isPresent();
        assertThat(polynomial.get().minLength()).isEqualTo(2);
        assertThat(polynomial.get().maxLength()).isEqualTo(-1);
        assertThat(polynomial.get().canMatch(3)).isFalse();
        assertThat(polynomial.get().canMatch(4)).isTrue();
        assertThat(polynomial.get().canMatch(100)).isTrue();
    }

    @Test
    void starRepetitionIsUnbounded() {
        var node = RegexParser.parse("(ab)*");
        var polynomial = node.lengthPolynomial();
        assertThat(polynomial).isPresent();
        assertThat(polynomial.get().minLength()).isEqualTo(0);
        assertThat(polynomial.get().maxLength()).isEqualTo(-1);
        assertThat(polynomial.get().canMatch(1)).isFalse();
        assertThat(polynomial.get().canMatch(2)).isTrue();
        assertThat(polynomial.get().canMatch(101)).isFalse();
    }

    @Test
    void openEndedRepetitionIsUnbounded() {
        var node = RegexParser.parse("a{2,}");
        var polynomial = node.lengthPolynomial();
        assertThat(polynomial).isPresent();
        assertThat(polynomial.get().minLength()).isEqualTo(2);
        assertThat(polynomial.get().maxLength()).isEqualTo(-1);
        assertThat(polynomial.get().canMatch(1)).isFalse();
        assertThat(polynomial.get().canMatch(2)).isTrue();
        assertThat(polynomial.get().canMatch(1000)).isTrue();
    }
}
