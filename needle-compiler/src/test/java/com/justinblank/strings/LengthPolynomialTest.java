package com.justinblank.strings;

import com.justinblank.strings.RegexAST.LengthPolynomial;
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

    @Test
    void membershipForMultipleUnboundedGenerators() {
        var node = RegexParser.parse("(ABC)+(DEFGHIJ)+");
        var membership = node.lengthPolynomial().orElseThrow().membership(CompilationPolicy.MAX_LENGTH_FILTER_MODULUS)
                .orElseThrow();
        assertThat(membership.base()).isEqualTo(10);
        assertThat(membership.modulus()).isEqualTo(3);
        for (int length : new int[] {10, 13, 16, 17, 19, 20, 22, 23, 24, 25, 26}) {
            assertThat(accepts(membership, length)).as("length " + length).isTrue();
        }
        for (int length : new int[] {0, 9, 11, 12, 14, 15, 18, 21}) {
            assertThat(accepts(membership, length)).as("length " + length).isFalse();
        }
    }

    @Test
    void membershipFallsBackWhenABoundIsTight() {
        var node = RegexParser.parse("(ABC){1,2}(DEFGHIJ)+");
        assertThat(node.lengthPolynomial().orElseThrow().membership(CompilationPolicy.MAX_LENGTH_FILTER_MODULUS))
                .isEmpty();
    }

    @Test
    void membershipHandlesBoundsWideEnoughNotToExcludeTheMinimum() {
        var node = RegexParser.parse("(ABC){1,8}(DEFGHIJ)+");
        var membership = node.lengthPolynomial().orElseThrow().membership(CompilationPolicy.MAX_LENGTH_FILTER_MODULUS)
                .orElseThrow();
        assertThat(membership.base()).isEqualTo(10);
        assertThat(accepts(membership, 16)).isTrue();
        assertThat(accepts(membership, 11)).isFalse();
        assertThat(accepts(membership, 14)).isFalse();
    }

    @Test
    void membershipFallsBackWhenModulusIsTooLarge() {
        var node = RegexParser.parse("(a{300})+(b{301})+");
        assertThat(node.lengthPolynomial().orElseThrow().membership(CompilationPolicy.MAX_LENGTH_FILTER_MODULUS))
                .isEmpty();
    }

    private static boolean accepts(LengthPolynomial.Membership membership, int length) {
        if (length < membership.base()) {
            return false;
        }
        int remaining = length - membership.base();
        return membership.minRepresentable()[remaining % membership.modulus()] <= remaining;
    }
}
