package com.justinblank.strings.RegexAST;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
        var variables = new ArrayList<LengthComponent>();
        for (LengthComponent c : this.components) {
            variables.add(c.times(n));
        }
        return new LengthPolynomial(variables, n * constant);
    }

    public LengthPolynomial plus(LengthPolynomial other) {
        var variables = new ArrayList<LengthComponent>();
        int thisIdx = 0;
        int otherIdx = 0;
        while (thisIdx < this.components.size()) {
            var thisVariable = this.components.get(thisIdx);
            var toAdd = thisVariable;
            while (otherIdx < other.components.size()) {
                var otherVariable = other.components.get(otherIdx);
                if (otherVariable.size < thisVariable.size) {
                    variables.add(otherVariable);
                    otherIdx++;
                    continue;
                }
                else if (thisVariable.size == otherVariable.size) {
                    toAdd = new LengthComponent(thisVariable.min + otherVariable.min, thisVariable.max + otherVariable.max, thisVariable.size);
                }
                break;
            }
            variables.add(toAdd);
            thisIdx++;
        }
        while (otherIdx < other.components.size()) {
            variables.add(other.components.get(otherIdx));
            otherIdx++;
        }
        return new LengthPolynomial(variables, constant + other.constant);
    }

    public Optional<LengthPolynomial> multi(int min, int max) {
        var variables = new ArrayList<LengthComponent>();
        variables.add(new LengthComponent(min, max, constant));
        for (var component : components) {
            variables.add(new LengthComponent(component.min * min, component.max * max, component.size));
        }
        var polynomial = new LengthPolynomial(variables, 0);
        return Optional.of(polynomial);
    }

    public int getConstant() {
        return constant;
    }

    /**
     * Return whether it's possible this polynomial blurp
     * @param i
     * @return
     */
    public boolean canMatch(int i) {
        var remaining = i - constant;
        if (remaining < 0) {
            return false;
        }
        if (components.size() > 1) {
            return true;
        }
        else if (components.size() == 1) {
            return remaining % components.get(0).size == 0;
        }
        return true;
    }

    public static int gcd(int x, int y) {
        while (x != y) {
            if (x > y) {
                int tmp = x % y;
                if (tmp == 0) {
                    return y;
                }
                x = y;
                y = tmp;
            }
            else {
                int tmp = y % x;
                if (tmp == 0) {
                    return x;
                }
                y = x;
                x = tmp;
            }
        }
        return x;
    }
}
