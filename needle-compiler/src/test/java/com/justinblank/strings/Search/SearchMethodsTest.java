package com.justinblank.strings.Search;

import com.justinblank.strings.MatchResult;
import org.junit.jupiter.api.Test;
import org.quicktheories.QuickTheory;
import org.quicktheories.core.Gen;
import org.quicktheories.generators.IntegersDSL;
import org.quicktheories.generators.ListsDSL;
import org.quicktheories.generators.StringsDSL;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;


class SearchMethodsTest {

    /**
     * Using a small alphabet seems to be more effective at finding bugs, but also provides more readable output and
     * better shrinking. So in many tests, I use the small alphabet first, then run with a large alphabet later. I don't
     * hide any bugs that might somehow happen with the large alphabet, but in general, I'll see the nicer output from
     * the small alphabet if need be.
     */
    private static final StringsDSL.StringGeneratorBuilder SMALL_ALPHABET = new StringsDSL().betweenCodePoints(97, 101);
    private static final StringsDSL.StringGeneratorBuilder BMP = new StringsDSL().betweenCodePoints(1, 256 * 256);
    private static final StringsDSL.StringGeneratorBuilder SMALL_BMP = new StringsDSL().betweenCodePoints(0x00c5, 0x00c9);// 0x05D0, 0x05D4);
    private static final int SMALL_DATA_SIZE = 10;
    private static final int LARGE_DATA_SIZE = 64;

    @Test
    void asciiSingleStringMatching() {
        Gen<String> strings = SMALL_ALPHABET.ofLengthBetween(1, SMALL_DATA_SIZE);
        QuickTheory.qt().forAll(strings).check((s1) ->
                SearchMethods.makeSearchMethod(Collections.singletonList(s1)).matches(s1));
        strings = new StringsDSL().ascii().ofLengthBetween(1, LARGE_DATA_SIZE);
        QuickTheory.qt().forAll(strings).check((s1) ->
                SearchMethods.makeSearchMethod(Collections.singletonList(s1)).matches(s1));
    }

    @Test
    void singleStringMatching() {
        Gen<String> strings = SMALL_BMP.ofLengthBetween(1, SMALL_DATA_SIZE);
        QuickTheory.qt().forAll(strings).check((s1) ->
                SearchMethods.makeSearchMethod(Collections.singletonList(s1)).matches(s1));
        strings = BMP.ofLengthBetween(1, LARGE_DATA_SIZE);
        QuickTheory.qt().forAll(strings).check((s1) ->
                SearchMethods.makeSearchMethod(Collections.singletonList(s1)).matches(s1));
    }

    @Test
    void asciiSingleStringContainedIn() {
        Gen<String> strings = new StringsDSL().ascii().ofLengthBetween(1, LARGE_DATA_SIZE);
        QuickTheory.qt().forAll(strings).check((s1) ->
                SearchMethods.makeSearchMethod(Collections.singletonList(s1)).containedIn(s1));
    }

    @Test
    void singleStringContainedIn() {
        Gen<String> strings = BMP.ofLengthBetween(1, LARGE_DATA_SIZE);
        QuickTheory.qt().forAll(strings).check((s1) ->
                SearchMethods.makeSearchMethod(Collections.singletonList(s1)).containedIn(s1));
    }

    @Test
    void asciiMatching() {
        Gen<String> strings = SMALL_ALPHABET.ofLengthBetween(1, SMALL_DATA_SIZE);
        Gen<List<String>> manyStrings = new ListsDSL().of(strings).ofSizeBetween(1, SMALL_DATA_SIZE);
        QuickTheory.qt().forAll(manyStrings, new IntegersDSL().allPositive()).check((l, i) ->
                SearchMethods.makeSearchMethod(l).matches(l.get(i % l.size())));

        strings = new StringsDSL().ascii().ofLengthBetween(1, LARGE_DATA_SIZE);
        manyStrings = new ListsDSL().of(strings).ofSizeBetween(1, LARGE_DATA_SIZE);
        QuickTheory.qt().forAll(manyStrings, new IntegersDSL().allPositive()).check((l, i) ->
                SearchMethods.makeSearchMethod(l).matches(l.get(i % l.size())));
    }

    @Test
    void matching() {
        Gen<String> strings = SMALL_BMP.ofLengthBetween(1, SMALL_DATA_SIZE);
        Gen<List<String>> manyStrings = new ListsDSL().of(strings).ofSizeBetween(1, SMALL_DATA_SIZE);
        QuickTheory.qt().forAll(manyStrings, new IntegersDSL().allPositive()).check((l, i) ->
                SearchMethods.makeSearchMethod(l).matches(l.get(i % l.size())));

        strings = BMP.ofLengthBetween(1, LARGE_DATA_SIZE);
        manyStrings = new ListsDSL().of(strings).ofSizeBetween(1, LARGE_DATA_SIZE);
        QuickTheory.qt().forAll(manyStrings, new IntegersDSL().allPositive()).check((l, i) ->
                SearchMethods.makeSearchMethod(l).matches(l.get(i % l.size())));
    }

    @Test
    void asciiSingleStringOverlappingFalseStart() {
        String needle = "aab";
        String haystack = "aaab";
        SearchMethod searchMethod = SearchMethods.makeSearchMethod(Collections.singletonList(needle));
        assertTrue(searchMethod.containedIn(haystack));
        assertFalse(searchMethod.matches(haystack));
        assertEquals(MatchResult.success(1, 4), searchMethod.find(haystack));
    }

    @Test
    void asciiContainedIn() {
        Gen<String> strings = SMALL_ALPHABET.ofLengthBetween(1, SMALL_DATA_SIZE);
        Gen<List<String>> manyStrings = new ListsDSL().of(strings).ofSizeBetween(2, SMALL_DATA_SIZE);
        QuickTheory.qt().forAll(manyStrings, strings, new IntegersDSL().between(0, SMALL_DATA_SIZE)).check((l, s, i) -> {
            String targetString = l.get(i % l.size());
            targetString = s + targetString + s;
            return SearchMethods.makeSearchMethod(l).containedIn(targetString);
        });

        strings = new StringsDSL().ascii().ofLengthBetween(1, LARGE_DATA_SIZE);
        manyStrings = new ListsDSL().of(strings).ofSizeBetween(2, LARGE_DATA_SIZE);
        QuickTheory.qt().forAll(manyStrings, strings, new IntegersDSL().between(0, LARGE_DATA_SIZE)).check((l, s, i) -> {
            String targetString = l.get(i % l.size());
            targetString = s + targetString + s;
            return SearchMethods.makeSearchMethod(l).containedIn(targetString);
        });
    }

    @Test
    void asciiContainedInWithPrefix() {
        Gen<String> strings = new StringsDSL().ascii().ofLengthBetween(1, LARGE_DATA_SIZE);
        Gen<List<String>> manyStrings = new ListsDSL().of(strings).ofSizeBetween(1, LARGE_DATA_SIZE);
        QuickTheory.qt().withFixedSeed(1119727800924974L).forAll(manyStrings, strings, new IntegersDSL().allPositive()).check((l, s, i) -> {
            String targetString = l.get(i % l.size());
            targetString = s + targetString;
            return SearchMethods.makeSearchMethod(l).containedIn(targetString);
        });
    }

    @Test
    void asciiContainedInWithSuffix() {
        Gen<String> strings = SMALL_ALPHABET.ofLengthBetween(1, SMALL_DATA_SIZE);
        Gen<List<String>> manyStrings = new ListsDSL().of(strings).ofSizeBetween(1, SMALL_DATA_SIZE);
        QuickTheory.qt().forAll(manyStrings, strings, new IntegersDSL().between(0, SMALL_DATA_SIZE)).check((l, s, i) -> {
            String targetString = l.get(i % l.size());
            targetString = targetString + s;
            return SearchMethods.makeSearchMethod(l).containedIn(targetString);
        });

        strings = new StringsDSL().ascii().ofLengthBetween(1, LARGE_DATA_SIZE);
        manyStrings = new ListsDSL().of(strings).ofSizeBetween(1, LARGE_DATA_SIZE);
        QuickTheory.qt().forAll(manyStrings, strings, new IntegersDSL().between(0, LARGE_DATA_SIZE)).check((l, s, i) -> {
            String targetString = l.get(i % l.size());
            targetString = targetString + s;
            return SearchMethods.makeSearchMethod(l).containedIn(targetString);
        });
    }

    @Test
    void asciiContainedInWithPrefixAndSuffix() {
        Gen<String> strings = SMALL_ALPHABET.ofLengthBetween(1, SMALL_DATA_SIZE);
        Gen<List<String>> manyStrings = new ListsDSL().of(strings).ofSizeBetween(1, SMALL_DATA_SIZE);
        QuickTheory.qt().withFixedSeed(1119727800924974L).forAll(manyStrings, strings, new IntegersDSL().allPositive()).check((l, s, i) -> {
            String targetString = l.get(i % l.size());
            targetString = s + targetString + s;
            return SearchMethods.makeSearchMethod(l).containedIn(targetString);
        });

        strings = new StringsDSL().ascii().ofLengthBetween(1, LARGE_DATA_SIZE);
        manyStrings = new ListsDSL().of(strings).ofSizeBetween(1, LARGE_DATA_SIZE);
        QuickTheory.qt().withFixedSeed(1119727800924974L).forAll(manyStrings, strings, new IntegersDSL().allPositive()).check((l, s, i) -> {
            String targetString = l.get(i % l.size());
            targetString = s + targetString + s;
            return SearchMethods.makeSearchMethod(l).containedIn(targetString);
        });
    }

    @Test
    void asciiFindArbitrary() {
        Gen<String> strings = SMALL_ALPHABET.ofLengthBetween(1, SMALL_DATA_SIZE);
        Gen<List<String>> manyStrings = new ListsDSL().of(strings).ofSizeBetween(2, SMALL_DATA_SIZE);
        Gen<String> haystackStrings = SMALL_ALPHABET.ofLengthBetween(1, LARGE_DATA_SIZE);
        QuickTheory.qt().forAll(manyStrings, strings, haystackStrings).check((l, s, h) -> {
            SearchMethod method = SearchMethods.makeSearchMethod(l);
            MatchResult result = method.find(h);
            if (result.matched) {
                String needle = h.substring(result.start, result.end);
                return l.contains(needle);
            }
            return true;
        });

        strings = new StringsDSL().ascii().ofLengthBetween(1, LARGE_DATA_SIZE);
        manyStrings = new ListsDSL().of(strings).ofSizeBetween(2, LARGE_DATA_SIZE);
        haystackStrings = new StringsDSL().ascii().ofLengthBetween(1, LARGE_DATA_SIZE);
        QuickTheory.qt().forAll(manyStrings, strings, haystackStrings).check((l, s, h) -> {
            SearchMethod method = SearchMethods.makeSearchMethod(l);
            MatchResult result = method.find(h);
            if (result.matched) {
                String needle = h.substring(result.start, result.end);
                return l.contains(needle);
            }
            return true;
        });
    }

    @Test
    void findArbitrary() {
        Gen<String> strings = SMALL_BMP.ofLengthBetween(1, SMALL_DATA_SIZE);
        Gen<List<String>> manyStrings = new ListsDSL().of(strings).ofSizeBetween(2, SMALL_DATA_SIZE);
        Gen<String> haystackStrings = SMALL_BMP.ofLengthBetween(1, LARGE_DATA_SIZE);
        QuickTheory.qt().forAll(manyStrings, strings, haystackStrings).check((l, s, h) -> {
            SearchMethod method = SearchMethods.makeSearchMethod(l);
            MatchResult result = method.find(h);
            if (result.matched) {
                String needle = h.substring(result.start, result.end);
                return l.contains(needle);
            }
            return true;
        });

        strings = BMP.ofLengthBetween(1, LARGE_DATA_SIZE);
        manyStrings = new ListsDSL().of(strings).ofSizeBetween(2, LARGE_DATA_SIZE);
        haystackStrings = BMP.ofLengthBetween(1, LARGE_DATA_SIZE);
        QuickTheory.qt().forAll(manyStrings, strings, haystackStrings).check((l, s, h) -> {
            SearchMethod method = SearchMethods.makeSearchMethod(l);
            MatchResult result = method.find(h);
            if (result.matched) {
                String needle = h.substring(result.start, result.end);
                return l.contains(needle);
            }
            return true;
        });
    }

    @Test
    void asciiCollidingNeedlesInMiddleOfHaystack() {
        List<String> strings = Arrays.asList("aa", "baaa");
        SearchMethod method = SearchMethods.makeSearchMethod(strings);
        assertTrue(method.containedIn("abaab"));
    }

    @Test
    void otherHaystackCollision() {
        List<String> strings = Arrays.asList("aabaa", "acaaaa", "aaaa");
        SearchMethod method = SearchMethods.makeSearchMethod(strings);
        assertTrue(method.containedIn("aabaa"));
        assertTrue(method.containedIn("aca" + "aabaa" + "aca"));
    }

    @Test
    void containedIn() {
        Gen<String> strings = new StringsDSL().betweenCodePoints(1, 256 * 256).ofLengthBetween(1, LARGE_DATA_SIZE);
        Gen<List<String>> manyStrings = new ListsDSL().of(strings).ofSizeBetween(1, LARGE_DATA_SIZE);
        QuickTheory.qt().forAll(manyStrings, strings, new IntegersDSL().allPositive()).check((l, s, i) -> {
            String targetString = l.get(i % l.size());
            targetString = s + targetString + s;
            return SearchMethods.makeSearchMethod(l).containedIn(targetString);
        });
    }

    @Test
    void asciiFind() {
        Gen<String> strings = SMALL_ALPHABET.ofLengthBetween(1, SMALL_DATA_SIZE);
        Gen<List<String>> manyStrings = new ListsDSL().of(strings).ofSizeBetween(2, SMALL_DATA_SIZE);
        QuickTheory.qt().forAll(manyStrings, strings, new IntegersDSL().between(0, SMALL_DATA_SIZE)).check((l, s, i) -> {
            String targetString = l.get(i % l.size());
            String newTargetString = s + targetString + s;
            SearchMethod method = SearchMethods.makeSearchMethod(l);
            MatchResult result = method.find(newTargetString);
            if (!result.matched) {
                return false;
            }
            String needle = newTargetString.substring(result.start, result.end);
            return l.contains(needle);
        });


        strings = new StringsDSL().ascii().ofLengthBetween(1, LARGE_DATA_SIZE);
        manyStrings = new ListsDSL().of(strings).ofSizeBetween(2, LARGE_DATA_SIZE);
        QuickTheory.qt().forAll(manyStrings, strings, new IntegersDSL().between(0, LARGE_DATA_SIZE)).check((l, s, i) -> {
            String targetString = l.get(i % l.size());
            String newTargetString = s + targetString + s;
            SearchMethod method =  SearchMethods.makeSearchMethod(l);
            MatchResult result = method.find(newTargetString);
            if (!result.matched) {
                return false;
            }
            String needle = newTargetString.substring(result.start, result.end);
            return l.contains(needle);
        });
    }

    @Test
    void asciiFindInBoundedSubstring() {
        Gen<String> strings = SMALL_ALPHABET.ofLengthBetween(1, SMALL_DATA_SIZE);
        Gen<List<String>> manyStrings = new ListsDSL().of(strings).ofSizeBetween(2, SMALL_DATA_SIZE);
        QuickTheory.qt().forAll(manyStrings, strings, new IntegersDSL().between(0, SMALL_DATA_SIZE)).check((l, s, i) -> {
            String targetString = l.get(i % l.size());
            String newTargetString = s + targetString + s;
            SearchMethod method =  SearchMethods.makeSearchMethod(l);
            MatchResult result = method.find(newTargetString, s.length(), newTargetString.length());
            if (!result.matched) {
                return false;
            }
            String needle = newTargetString.substring(result.start, result.end);
            return l.contains(needle);
        });

        strings = new StringsDSL().ascii().ofLengthBetween(1, LARGE_DATA_SIZE);
        manyStrings = new ListsDSL().of(strings).ofSizeBetween(2, LARGE_DATA_SIZE);
        QuickTheory.qt().forAll(manyStrings, strings, new IntegersDSL().between(0, LARGE_DATA_SIZE)).check((l, s, i) -> {
            String targetString = l.get(i % l.size());
            String newTargetString = s + targetString + s;
            SearchMethod method =  SearchMethods.makeSearchMethod(l);
            MatchResult result = method.find(newTargetString, s.length(), newTargetString.length());
            if (!result.matched) {
                return false;
            }
            String needle = newTargetString.substring(result.start, result.end);
            return l.contains(needle);
        });
    }

    @Test
    void find() {
        Gen<String> strings = SMALL_BMP.ofLengthBetween(1, SMALL_DATA_SIZE);
        Gen<List<String>> manyStrings = new ListsDSL().of(strings).ofSizeBetween(2, SMALL_DATA_SIZE);
        QuickTheory.qt().forAll(manyStrings, strings, new IntegersDSL().between(0, SMALL_DATA_SIZE)).check((l, s, i) -> {
            String targetString = l.get(i % l.size());
            String newTargetString = s + targetString + s;
            SearchMethod method =  SearchMethods.makeSearchMethod(l);
            MatchResult result = method.find(newTargetString);
            if (!result.matched) {
                return false;
            }
            String needle = newTargetString.substring(result.start, result.end);
            return l.contains(needle);
        });

        strings = BMP.ofLengthBetween(1, LARGE_DATA_SIZE);
        manyStrings = new ListsDSL().of(strings).ofSizeBetween(2, LARGE_DATA_SIZE);
        QuickTheory.qt().forAll(manyStrings, strings, new IntegersDSL().between(0, LARGE_DATA_SIZE)).check((l, s, i) -> {
            String targetString = l.get(i % l.size());
            String newTargetString = s + targetString + s;
            SearchMethod method =  SearchMethods.makeSearchMethod(l);
            MatchResult result = method.find(newTargetString);
            if (!result.matched) {
                return false;
            }
            String needle = newTargetString.substring(result.start, result.end);
            return l.contains(needle);
        });
    }

    @Test
    void findInBoundedSubstring() {
        Gen<String> strings = SMALL_BMP.ofLengthBetween(1, SMALL_DATA_SIZE);
        Gen<List<String>> manyStrings = new ListsDSL().of(strings).ofSizeBetween(2, SMALL_DATA_SIZE);
        QuickTheory.qt().forAll(manyStrings, strings, new IntegersDSL().between(0, SMALL_DATA_SIZE)).check((l, s, i) -> {
            String targetString = l.get(i % l.size());
            String newTargetString = s + targetString + s;
            SearchMethod method =  SearchMethods.makeSearchMethod(l);
            MatchResult result = method.find(newTargetString, s.length(), newTargetString.length());
            if (!result.matched) {
                return false;
            }
            String needle = newTargetString.substring(result.start, result.end);
            return l.contains(needle);
        });

        strings = BMP.ofLengthBetween(1, LARGE_DATA_SIZE);
        manyStrings = new ListsDSL().of(strings).ofSizeBetween(2, LARGE_DATA_SIZE);
        QuickTheory.qt().forAll(manyStrings, strings, new IntegersDSL().between(0, LARGE_DATA_SIZE)).check((l, s, i) -> {
            String targetString = l.get(i % l.size());
            String newTargetString = s + targetString + s;
            SearchMethod method =  SearchMethods.makeSearchMethod(l);
            MatchResult result = method.find(newTargetString, s.length(), newTargetString.length());
            if (!result.matched) {
                return false;
            }
            String needle = newTargetString.substring(result.start, result.end);
            return l.contains(needle);
        });
    }

    @Test
    void searchMethodMatchesStringContainingNonBMPCharacters() {
        String s = "\uD83D\uDE02abcdef";
        SearchMethod method = SearchMethods.makeSearchMethod(Set.of("abc"));
        assertTrue(method.containedIn(s));
    }

    @Test
    void illegalIndexStartAscii() {
        assertThrows(IndexOutOfBoundsException.class, () ->
            SearchMethods.makeSearchMethod(Collections.singletonList("a")).find("a", -1, 1));
    }

    @Test
    void illegalIndexEndNegativeAscii() {
        assertThrows(IndexOutOfBoundsException.class, () ->
            SearchMethods.makeSearchMethod(Collections.singletonList("a")).find("a", 0, -1));
    }

    @Test
    void illegalIndexEndAscii() {
        assertThrows(IndexOutOfBoundsException.class, () ->
            SearchMethods.makeSearchMethod(Collections.singletonList("a")).find("a", 0, 3));
    }

    @Test
    void startGreaterThanEndAscii() {
        assertThrows(IllegalArgumentException.class, () ->
            SearchMethods.makeSearchMethod(Collections.singletonList("a")).find("abc", 2, 1));
    }

    @Test
    void illegalIndexStart() {
        assertThrows(IndexOutOfBoundsException.class, () ->
            SearchMethods.makeSearchMethod(Collections.singletonList("א")).find("a", -1, 1));
    }

    @Test
    void illegalIndexEndNegative() {
        assertThrows(IndexOutOfBoundsException.class, () ->
            SearchMethods.makeSearchMethod(Collections.singletonList("א")).find("a", 0, -1));
    }

    @Test
    void illegalIndexEnd() {
        assertThrows(IndexOutOfBoundsException.class, () ->
            SearchMethods.makeSearchMethod(Collections.singletonList("א")).find("a", 0, 3));
    }

    @Test
    void startGreaterThanEnd() {
        assertThrows(IllegalArgumentException.class, () ->
            SearchMethods.makeSearchMethod(Collections.singletonList("א")).find("abc", 2, 1));
    }
}
