package com.justinblank.strings;

import com.justinblank.strings.Search.SearchMethod;
import com.justinblank.strings.Search.SearchMethods;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.quicktheories.QuickTheory;
import org.quicktheories.core.Gen;
import org.quicktheories.generators.Generate;
import org.quicktheories.generators.IntegersDSL;
import org.quicktheories.generators.ListsDSL;
import org.quicktheories.generators.StringsDSL;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.justinblank.strings.SearchMethodTestUtil.*;
import static com.justinblank.strings.SearchMethodTestUtil.match;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

public class NFATest {


    @Test
    public void testAsciiSingleStringMatching() {
        Gen<String> strings = SMALL_ALPHABET.ofLengthBetween(1, SMALL_DATA_SIZE);
        QuickTheory.qt().forAll(strings).check((s1) ->
                NFA.createNFA(s1).matches(s1));
        strings = A_THROUGH_Z.ofLengthBetween(1, LARGE_DATA_SIZE);
        QuickTheory.qt().forAll(strings).check((s1) ->
                NFA.createNFA(s1).matches(s1));
    }

    @Test
    public void testRepetition() {
        Gen<String> s = A_THROUGH_Z.ofLength(1);
        Gen<Integer> l = new IntegersDSL().between(2, 100);
        QuickTheory.qt().forAll(s, l).check((singleChar, length) -> {
            String regexString = singleChar + "+";
            String hayStack = StringUtils.repeat(singleChar , length);
            SearchMethod method = NFA.createNFA(regexString);
            MatchResult result = method.find(hayStack);
            if (!(MatchResult.success(0, hayStack.length()).equals(result))) {
                return false;
            }
            result = method.find(hayStack, 1, hayStack.length());
            return MatchResult.success(1, hayStack.length()).equals(result);
        });
    }

    @Test
    public void testCountedRepetitions() {
        Gen<String> s = A_THROUGH_Z.ofLength(1);
        Gen<Integer> l = new IntegersDSL().between(2, 100);
        QuickTheory.qt().forAll(s, l, l, l).check((singleChar, length, min, max) -> {
            if (min > max) {
                int temp = max;
                max = min;
                min = temp;
            }
            String regexString = singleChar + "{" + min + "," + max + "}";
            String hayStack = StringUtils.repeat(singleChar, length);
            SearchMethod method = NFA.createNFA(regexString);
            MatchResult result = method.find(hayStack);
            if (length >= min) {
                int matchLength = Math.min(hayStack.length(), max);
                if (!(MatchResult.success(0, matchLength).equals(result))) {
                    return false;
                }
            }
            if (length > min) {
                result = method.find(hayStack, 1, Math.min(hayStack.length(), 1 + max));
                int matchLength = Math.min(hayStack.length(), 1 + max);
                return MatchResult.success(1, matchLength).equals(result);
            }
            return true;

        });
    }

    @Test
    public void testSingleStringMatching() {
        Gen<String> strings = SMALL_BMP.ofLengthBetween(1, SMALL_DATA_SIZE);
        QuickTheory.qt().forAll(strings).check((s1) ->
                NFA.createNFA(s1).matches(s1));
        strings = A_THROUGH_Z.ofLengthBetween(1, LARGE_DATA_SIZE);
        QuickTheory.qt().forAll(strings).check((s1) ->
                NFA.createNFA(s1).matches(s1));
    }

    @Test
    public void testAsciiSingleStringContainedIn() {
        Gen<String> strings = A_THROUGH_Z.ofLengthBetween(1, LARGE_DATA_SIZE);
        QuickTheory.qt().forAll(strings).check((s1) ->
                NFA.createNFA(s1).containedIn(s1));
    }

    @Test
    public void testSingleStringContainedIn() {
        Gen<String> strings = NON_METACHAR_BMP.ofLengthBetween(1, LARGE_DATA_SIZE);
        QuickTheory.qt().forAll(strings).check((s1) ->
                NFA.createNFA(s1).containedIn(s1));
    }

    @Test
    public void testMatching() {
        Gen<String> strings = SMALL_BMP.ofLengthBetween(1, SMALL_DATA_SIZE);
        Gen<List<String>> manyStrings = new ListsDSL().of(strings).ofSizeBetween(1, SMALL_DATA_SIZE);
        QuickTheory.qt().forAll(manyStrings, new IntegersDSL().allPositive()).check((l, i) ->
                createNFA(l).matches(l.get(i % l.size())));

        strings = NON_METACHAR_BMP.ofLengthBetween(1, LARGE_DATA_SIZE);
        manyStrings = new ListsDSL().of(strings).ofSizeBetween(1, LARGE_DATA_SIZE);
        QuickTheory.qt().forAll(manyStrings, new IntegersDSL().allPositive()).check((l, i) ->
                createNFA(l).matches(l.get(i % l.size())));
    }

    @Test
    public void testAsciiSingleStringOverlappingFalseStart() {
        String needle = "aab";
        String haystack = "aaab";
        SearchMethod nfa = NFA.createNFA(needle);
        assertTrue(nfa.containedIn(haystack));
        assertFalse(nfa.matches(haystack));
        assertEquals(MatchResult.success(1, 4), nfa.find(haystack));
    }

    @Test
    public void testAsciiContainedIn() {
        Gen<String> strings = SMALL_ALPHABET.ofLengthBetween(1, SMALL_DATA_SIZE);
        Gen<List<String>> manyStrings = new ListsDSL().of(strings).ofSizeBetween(2, SMALL_DATA_SIZE);
        QuickTheory.qt().forAll(manyStrings, strings, new IntegersDSL().between(0, SMALL_DATA_SIZE)).check((l, s, i) -> {
            String targetString = l.get(i % l.size());
            targetString = s + targetString + s;
            return NFA.createNFA("(" + String.join(")|(", l) + ")").containedIn(targetString);
        });

        strings = A_THROUGH_Z.ofLengthBetween(1, LARGE_DATA_SIZE);
        manyStrings = new ListsDSL().of(strings).ofSizeBetween(2, LARGE_DATA_SIZE);
        QuickTheory.qt().forAll(manyStrings, strings, new IntegersDSL().between(0, LARGE_DATA_SIZE)).check((l, s, i) -> {
            String targetString = l.get(i % l.size());
            targetString = s + targetString + s;
            return NFA.createNFA(joinLiterals(l)).containedIn(targetString);
        });
    }

    @Test
    public void testAsciiContainedInWithAffixes() {
        Gen<String> strings = SMALL_ALPHABET.ofLengthBetween(1, SMALL_DATA_SIZE);
        Gen<String> maybeEmpty = strings.mix(Generate.constant(() -> ""));
        Gen<List<String>> manyStrings = new ListsDSL().of(strings).ofSizeBetween(1, SMALL_DATA_SIZE);
        QuickTheory.qt().withFixedSeed(1119727800924974L).forAll(manyStrings, maybeEmpty, maybeEmpty, new IntegersDSL().allPositive()).check((l, p, s, i) -> {
            String targetString = l.get(i % l.size());
            targetString = p + targetString + s;
            return createNFA(l).containedIn(targetString);
        });

        strings = A_THROUGH_Z.ofLengthBetween(1, LARGE_DATA_SIZE);
        maybeEmpty = strings.mix(Generate.constant(() -> ""));
        manyStrings = new ListsDSL().of(strings).ofSizeBetween(1, LARGE_DATA_SIZE);
        QuickTheory.qt().withFixedSeed(1119727800924974L).forAll(manyStrings, maybeEmpty, maybeEmpty, new IntegersDSL().allPositive()).check((l, p, s, i) -> {
            String targetString = l.get(i % l.size());
            targetString = p + targetString + s;
            return createNFA(l).containedIn(targetString);
        });
    }

    @Test
    public void testAsciiFindArbitrary() {
        Gen<String> strings = SMALL_ALPHABET.ofLengthBetween(1, SMALL_DATA_SIZE);
        Gen<List<String>> manyStrings = new ListsDSL().of(strings).ofSizeBetween(2, SMALL_DATA_SIZE);
        Gen<String> haystackStrings = SMALL_ALPHABET.ofLengthBetween(1, LARGE_DATA_SIZE);
        QuickTheory.qt().forAll(manyStrings, strings, haystackStrings).check((l, s, h) -> {
            SearchMethod method = createNFA(l);
            MatchResult result = method.find(h);
            boolean shouldMatch = false;
            for (String needle : l) {
                if (h.contains(needle)) {
                    shouldMatch = true;
                    break;
                }
            }
            if (shouldMatch && !result.matched) {
                return false;
            }
            if (result.matched) {
                String needle = h.substring(result.start, result.end);
                return l.contains(needle);
            }
            return true;
        });

        strings = A_THROUGH_Z.ofLengthBetween(1, LARGE_DATA_SIZE);
        manyStrings = new ListsDSL().of(strings).ofSizeBetween(2, LARGE_DATA_SIZE);
        haystackStrings = new StringsDSL().ascii().ofLengthBetween(1, LARGE_DATA_SIZE);
        QuickTheory.qt().forAll(manyStrings, strings, haystackStrings).check((l, s, h) -> {
            SearchMethod method = createNFA(l);
            MatchResult result = method.find(h);
            if (result.matched) {
                String needle = h.substring(result.start, result.end);
                return l.contains(needle);
            }
            return true;
        });
    }

    @Test
    public void testFindArbitrary() {
        Gen<String> strings = SMALL_BMP.ofLengthBetween(1, SMALL_DATA_SIZE);
        Gen<List<String>> manyStrings = new ListsDSL().of(strings).ofSizeBetween(2, SMALL_DATA_SIZE);
        Gen<String> haystackStrings = SMALL_BMP.ofLengthBetween(1, LARGE_DATA_SIZE);
        QuickTheory.qt().forAll(manyStrings, strings, haystackStrings).check((l, s, h) -> {
            SearchMethod method = createNFA(l);
            MatchResult result = method.find(h);
            if (result.matched) {
                String needle = h.substring(result.start, result.end);
                return l.contains(needle);
            }
            return true;
        });

        strings = NON_METACHAR_BMP.ofLengthBetween(1, LARGE_DATA_SIZE);
        manyStrings = new ListsDSL().of(strings).ofSizeBetween(2, LARGE_DATA_SIZE);
        haystackStrings = NON_METACHAR_BMP.ofLengthBetween(1, LARGE_DATA_SIZE);
        QuickTheory.qt().forAll(manyStrings, strings, haystackStrings).check((l, s, h) -> {
            SearchMethod method = createNFA(l);
            MatchResult result = method.find(h);
            if (result.matched) {
                String needle = h.substring(result.start, result.end);
                return l.contains(needle);
            }
            return true;
        });
    }

    @Test
    public void testAsciiCollidingNeedlesInMiddleOfHaystack() {
        List<String> strings = Arrays.asList("aa", "baaa");
        SearchMethod nfa = createNFA(strings);
        assertTrue(nfa.containedIn("abaab"));
    }

    @Test
    public void testOtherHaystackCollision() {
        List<String> strings = Arrays.asList("aabaa", "acaaaa", "aaaa");
        SearchMethod nfa = NFA.createNFA("(" + String.join(")|(", strings) + ")");
        assertTrue(nfa.containedIn("aabaa"));
        assertTrue(nfa.containedIn("aca" + "aabaa" + "aca"));
    }

    @Test
    public void testUnionOfLiterals() {
        SearchMethod method = NFA.createNFA("A|BCD|E");
        match(method, "A");
        match(method, "BCD");
        match(method, "E");
        find(method, "ZZAZZ");
        assertFalse(method.matcher("F").matches());
        assertFalse(method.matcher("F").containedIn());
    }

    @Test
    public void testContainedIn() {
        Gen<String> strings = NON_METACHAR_BMP.ofLengthBetween(1, LARGE_DATA_SIZE);
        Gen<List<String>> manyStrings = new ListsDSL().of(strings).ofSizeBetween(1, LARGE_DATA_SIZE);
        QuickTheory.qt().forAll(manyStrings, strings, new IntegersDSL().allPositive()).check((l, s, i) -> {
            String targetString = l.get(i % l.size());
            targetString = s + targetString + s;
            return createNFA(l).containedIn(targetString);
        });
    }

    @Test
    public void testAsciiFind() {
        Gen<String> strings = SMALL_ALPHABET.ofLengthBetween(1, SMALL_DATA_SIZE);
        Gen<List<String>> manyStrings = new ListsDSL().of(strings).ofSizeBetween(2, SMALL_DATA_SIZE);
        QuickTheory.qt().forAll(manyStrings, strings, new IntegersDSL().between(0, SMALL_DATA_SIZE)).check((l, s, i) -> {
            String targetString = l.get(i % l.size());
            String newTargetString = s + targetString + s;
            SearchMethod method = createNFA(l);
            MatchResult result = method.find(newTargetString);
            if (!result.matched) {
                return false;
            }
            String needle = newTargetString.substring(result.start, result.end);
            return l.contains(needle);
        });

        strings = A_THROUGH_Z.ofLengthBetween(1, LARGE_DATA_SIZE);
        manyStrings = new ListsDSL().of(strings).ofSizeBetween(2, LARGE_DATA_SIZE);
        QuickTheory.qt().forAll(manyStrings, strings, new IntegersDSL().between(0, LARGE_DATA_SIZE)).check((l, s, i) -> {
            String targetString = l.get(i % l.size());
            String newTargetString = s + targetString + s;
            SearchMethod method =  createNFA(l);
            MatchResult result = method.find(newTargetString);
            if (!result.matched) {
                return false;
            }
            String needle = newTargetString.substring(result.start, result.end);
            return l.contains(needle);
        });
    }

    @Test
    public void testAsciiFindInBoundedSubstring() {
        Gen<String> strings = SMALL_ALPHABET.ofLengthBetween(1, SMALL_DATA_SIZE);
        Gen<List<String>> manyStrings = new ListsDSL().of(strings).ofSizeBetween(2, SMALL_DATA_SIZE);
        QuickTheory.qt().forAll(manyStrings, strings, new IntegersDSL().between(0, SMALL_DATA_SIZE)).check((l, s, i) -> {
            String targetString = l.get(i % l.size());
            String newTargetString = s + targetString + s;
            SearchMethod method = createNFA(l);
            MatchResult result = method.find(newTargetString, s.length(), newTargetString.length());
            if (!result.matched) {
                return false;
            }
            String needle = newTargetString.substring(result.start, result.end);
            return l.contains(needle);
        });

        strings = A_THROUGH_Z.ofLengthBetween(1, LARGE_DATA_SIZE);
        manyStrings = new ListsDSL().of(strings).ofSizeBetween(2, LARGE_DATA_SIZE);
        QuickTheory.qt().forAll(manyStrings, strings, new IntegersDSL().between(0, LARGE_DATA_SIZE)).check((l, s, i) -> {
            String targetString = l.get(i % l.size());
            String newTargetString = s + targetString + s;
            SearchMethod method =  createNFA(l);
            MatchResult result = method.find(newTargetString, s.length(), newTargetString.length());
            if (!result.matched) {
                return false;
            }
            String needle = newTargetString.substring(result.start, result.end);
            return l.contains(needle);
        });
    }

    @Test
    public void testFind() {
        Gen<String> strings = SMALL_BMP.ofLengthBetween(1, SMALL_DATA_SIZE);
        Gen<List<String>> manyStrings = new ListsDSL().of(strings).ofSizeBetween(2, SMALL_DATA_SIZE);
        QuickTheory.qt().forAll(manyStrings, strings, new IntegersDSL().between(0, SMALL_DATA_SIZE)).check((l, s, i) -> {
            String targetString = l.get(i % l.size());
            String newTargetString = s + targetString + s;
            SearchMethod method = createNFA(l);
            MatchResult result = method.find(newTargetString);
            if (!result.matched) {
                return false;
            }
            String needle = newTargetString.substring(result.start, result.end);
            return l.contains(needle);
        });

        strings = NON_METACHAR_BMP.ofLengthBetween(1, LARGE_DATA_SIZE);
        manyStrings = new ListsDSL().of(strings).ofSizeBetween(2, LARGE_DATA_SIZE);
        QuickTheory.qt().forAll(manyStrings, strings, new IntegersDSL().between(0, LARGE_DATA_SIZE)).check((l, s, i) -> {
            String targetString = l.get(i % l.size());
            String newTargetString = s + targetString + s;
            SearchMethod method = createNFA(l);
            MatchResult result = method.find(newTargetString);
            if (!result.matched) {
                return false;
            }
            String needle = newTargetString.substring(result.start, result.end);
            return l.contains(needle);
        });
    }

    @Test
    public void testFindInBoundedSubstring() {
        Gen<String> strings = SMALL_BMP.ofLengthBetween(1, SMALL_DATA_SIZE);
        Gen<List<String>> manyStrings = new ListsDSL().of(strings).ofSizeBetween(2, SMALL_DATA_SIZE);
        QuickTheory.qt().forAll(manyStrings, strings, new IntegersDSL().between(0, SMALL_DATA_SIZE)).check((l, s, i) -> {
            String targetString = l.get(i % l.size());
            String newTargetString = s + targetString + s;
            SearchMethod method = createNFA(l);
            MatchResult result = method.find(newTargetString, s.length(), newTargetString.length());
            if (!result.matched) {
                return false;
            }
            String needle = newTargetString.substring(result.start, result.end);
            return l.contains(needle);
        });

        strings = NON_METACHAR_BMP.ofLengthBetween(1, LARGE_DATA_SIZE);
        manyStrings = new ListsDSL().of(strings).ofSizeBetween(2, LARGE_DATA_SIZE);
        QuickTheory.qt().forAll(manyStrings, strings, new IntegersDSL().between(0, LARGE_DATA_SIZE)).check((l, s, i) -> {
            String targetString = l.get(i % l.size());
            String newTargetString = s + targetString + s;
            SearchMethod method = createNFA(l);
            MatchResult result = method.find(newTargetString, s.length(), newTargetString.length());
            if (!result.matched) {
                return false;
            }
            String needle = newTargetString.substring(result.start, result.end);
            return l.contains(needle);
        });
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testIllegalIndexStart() {
        NFA.createNFA("a*").find("a", -1, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testIllegalIndexEndNegative() {
        NFA.createNFA("a*").find("a", 0, -1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testIllegalIndexEnd() {
        NFA.createNFA("a*").find("a", 0, 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStartGreaterThanEnd() {
        NFA.createNFA("a*").find("abc", 2, 1);
    }

    private static String joinLiterals(List<String> strings) {
        return "(" + String.join(")|(", strings) + ")";
    }

    private static SearchMethod createNFA(List<String> strings) {
        return NFA.createNFANoAhoCorasick(joinLiterals(strings));
    }

    @Test
    public void testEpsilonClosure_isNoOpOnCharacter() {
        NFA nfa = NFA.createNFANoAhoCorasick("gc*g");
        var closure = nfa.epsilonClosure(0);
        assertEquals(Set.of(0), closure);
    }

    @Test
    public void testEpsilonClosure_omitsSkipInstructions() {
        NFA nfa = NFA.createNFANoAhoCorasick("gc*g");
        var closure = nfa.epsilonClosure(1);
        assertEquals(Set.of(2, 4), closure);
    }
}
