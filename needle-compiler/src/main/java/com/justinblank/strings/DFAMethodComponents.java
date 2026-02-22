package com.justinblank.strings;

import com.justinblank.classcompiler.Method;
import com.justinblank.classcompiler.lang.Builtin;
import com.justinblank.classcompiler.lang.Conditional;
import com.justinblank.classcompiler.lang.Expression;
import com.justinblank.classcompiler.lang.Statement;

import static com.justinblank.classcompiler.lang.BinaryOperator.*;
import static com.justinblank.classcompiler.lang.BinaryOperator.gt;
import static com.justinblank.classcompiler.lang.CodeElement.*;
import static com.justinblank.classcompiler.lang.CodeElement.set;
import static com.justinblank.classcompiler.lang.Literal.literal;

public class DFAMethodComponents {
    static void returnWasAccepted(FindMethodSpec spec, Method method) {
        method.returnValue(
                call(spec.wasAcceptedName(), Builtin.BOOL, thisRef(), read(MatchingVars.STATE)));
    }

    static void setLengthLocalVariable(Method method) {
        method.set(MatchingVars.LENGTH, get(MatchingVars.LENGTH, Builtin.I, thisRef()));
    }

    static Expression inBounds() {
        return lt(read(MatchingVars.INDEX), read(MatchingVars.LENGTH));
    }

    static Expression inBoundsForStart() {
        return lte(read(MatchingVars.INDEX), read(MatchingVars.MAX_START));
    }

    protected static Expression readChar() {
        return call("charAt", Builtin.C,
                read(MatchingVars.STRING),
                read(MatchingVars.INDEX));
    }

    protected static Statement incrementIndex() {
        return set(MatchingVars.INDEX, plus(read(MatchingVars.INDEX), 1));
    }

    static Expression hasLastMatch() {
        return gt(read(MatchingVars.LAST_MATCH), literal(-1));
    }

    static Conditional returnLastMatchIfDeadState() {
        return cond(eq(-1, read(MatchingVars.STATE))).withBody(returnValue(read(MatchingVars.LAST_MATCH)));
    }

    static Conditional setLastMatchIfAccepted(String wasAcceptedMethod) {
        return cond(call(wasAcceptedMethod, Builtin.BOOL, thisRef(), read(MatchingVars.STATE))).withBody(
                set(MatchingVars.LAST_MATCH, read(MatchingVars.INDEX))
        );
    }

    static void checkMinMaxLengthForMatch(Method method, Factorization factorization) {
        if (factorization.getMinLength() > CompilationPolicy.THRESHOLD_FOR_CALCULATING_MAX_START || factorization.getMaxLength().isPresent()) {
            Expression expression = null;
            if (factorization.getMinLength() > CompilationPolicy.THRESHOLD_FOR_CALCULATING_MAX_START) {
                expression = gt(factorization.getMinLength(), read(MatchingVars.LENGTH));
            }
            if (factorization.getMaxLength().isPresent()) {
                var secondExpression = gt(read(DFAClassBuilder.LENGTH_FIELD), factorization.getMaxLength().get());

                if (expression == null) {
                    expression = secondExpression;
                }
                else {
                    expression = or(expression, secondExpression);
                }
            }
            method.cond(expression).withBody(returnValue(0));
        }
    }
}
