package com.justinblank.strings;

import org.junit.jupiter.api.Test;

import java.util.*;

import static com.justinblank.strings.Factorization.best;
import static org.junit.jupiter.api.Assertions.*;

class FactorizationTest {

    @Test
    void bestDifferentLengths() {
        var set1 = Set.of("abc", "def", "ghi");
        var set2 = Set.of("abc", "def");
        assertEquals(set2, best(set1, set2));
    }

    @Test
    void bestNull() {
        Set<String> set1 = null;
        Set<String> set2 = null;
        assertNull(best(set1, set2));
    }

    @Test
    void bestEmpty() {
        Set<String> set1 = Collections.emptySet();
        Set<String> set2 = Collections.emptySet();
        assertEquals(set2, best(set1, set2));
    }

    @Test
    void best1Empty() {
        Set<String> set1 = Collections.emptySet();
        Set<String> set2 = Set.of("A");
        assertEquals(set2, best(set1, set2));
    }

    @Test
    void bestDifferentDominated() {
        var set1 = Set.of("abc12", "def12", "ghi12");
        var set2 = Set.of("abc", "def");
        assertEquals(set1, best(set1, set2));
    }

    @Test
    void bestSameLength() {
        var set1 = Set.of("abc", "def", "ghi");
        var set2 = Set.of("abc", "def", "ghi12");
        assertEquals(set2, best(set1, set2));
    }

    @Test
    void factorizationSimpleConcatentation() {
        var node = RegexParser.parse("AB");
        Set<String> expected = Set.of("AB");
        Factorization factorization = node.bestFactors();

        assertEquals(expected, factorization.getFactors());
        assertEquals(expected, factorization.getPrefixes());
        assertEquals(expected, factorization.getSuffixes());
        assertEquals(expected, factorization.getRequiredFactors());
        assertEquals(expected, factorization.getAll());
    }

    @Test
    void factorizationRepetitionAndConcatenation() {
        var node = RegexParser.parse("A*B");
        Factorization factorization = node.bestFactors();
        assertNull(factorization.getAll());
        assertEquals(Set.of("B"), factorization.getSuffixes());
        assertTrue(factorization.getPrefixes().isEmpty());
        assertEquals(Set.of("B"), factorization.getFactors());
        assertEquals(Set.of("B"), factorization.getRequiredFactors());
    }

    @Test
    void factorization() {
        var node = RegexParser.parse("((GA|AAA)*)(TA|AG)");
        Factorization factorization = node.bestFactors();
        assertEquals(Set.of("TA", "AG"), factorization.getFactors());
        assertEquals(Set.of(), factorization.getRequiredFactors());
    }

    @Test
    void factorizationCharRangeUnion() {
        var node = RegexParser.parse("[AB][CD]");
        var factorization = node.bestFactors();
        var factorSet = Set.of("BC", "AC", "BD", "AD");
        assertEquals(factorSet, factorization.getPrefixes());
        assertEquals(factorSet, factorization.getSuffixes());
        assertEquals(factorSet, factorization.getFactors());
        assertEquals(factorSet, factorization.getAll());
        assertEquals(new HashSet<>(), factorization.getRequiredFactors());
    }

    @Test
    void factorizationDNAExample() {
        var node = RegexParser.parse("(ABC(GA|AAA)*)(TA|AG)");
        Factorization factorization = node.bestFactors();
        assertEquals(Set.of("ABC"), factorization.getFactors());
    }

    @Test
    void factorizationUnionOfLiterals() {
        var node = RegexParser.parse("((ABC)|(DEF))");
        var factorization = node.bestFactors();
        assertEquals(Set.of("ABC", "DEF"), factorization.getFactors());
        assertEquals(Set.of("ABC", "DEF"), factorization.getPrefixes());
        assertEquals(Set.of("ABC", "DEF"), factorization.getSuffixes());
        assertEquals(Set.of("ABC", "DEF"), factorization.getAll());
    }

    @Test
    void oneOrMoreRepetition() {
        var node = RegexParser.parse("A?");
        var factorization = node.bestFactors();
        var factorSet = Set.of("", "A");
        assertEquals(factorSet, factorization.getPrefixes());
        assertEquals(factorSet, factorization.getSuffixes());
        assertEquals(factorSet, factorization.getFactors());
        assertEquals(factorSet, factorization.getAll());
    }

    @Test
    void oneOrMoreRepetitionCharRange() {
        var node = RegexParser.parse("[A-Z]*");
        var factorization = node.bestFactors();
        assertNull(factorization.getPrefixes());
        assertNull(factorization.getSuffixes());
        assertNull(factorization.getFactors());
        assertNull(factorization.getAll());
    }

    @Test
    void potentiallyEmptyCountedRepetition() {
        var node = RegexParser.parse("(AB){0,2}");
        var factorization = node.bestFactors();
        var expectedFactors = Set.of("", "AB", "ABAB");
        assertEquals(expectedFactors, factorization.getFactors());
        assertEquals(expectedFactors, factorization.getPrefixes());
        assertEquals(expectedFactors, factorization.getSuffixes());
        assertEquals(expectedFactors, factorization.getAll());

        assertEquals(Optional.empty(), factorization.getSharedPrefix());
    }

    @Test
    void potentiallyEmptyCountedRepetitionOfRange() {
        var node = RegexParser.parse("[B-i]{0,2}");
        var factorization = node.bestFactors();
        var expectedFactors = Set.of("");
        assertEquals(expectedFactors, factorization.getFactors());
        assertEquals(expectedFactors, factorization.getPrefixes());
        assertEquals(expectedFactors, factorization.getSuffixes());
        assertNull(factorization.getAll());

        assertEquals(Optional.empty(), factorization.getSharedPrefix());
    }

    @Test
    void countedRepetition() {
        var node = RegexParser.parse("(AB){1,2}");
        var factorization = node.bestFactors();
        Set<String> expectedFactors = Set.of("AB", "ABAB");
        assertEquals(expectedFactors, factorization.getFactors());
        assertEquals(expectedFactors, factorization.getPrefixes());
        assertEquals(expectedFactors, factorization.getSuffixes());
        assertEquals(expectedFactors, factorization.getAll());
    }

    @Test
    void potentiallyEmptyCountedRepetitionWithLargeRange() {
        var node = RegexParser.parse("[A-Z]{0,2}");
        var factorization = node.bestFactors();
        assertEquals(Set.of(""), factorization.getFactors());
        assertEquals(Set.of(""), factorization.getPrefixes());
        assertEquals(Set.of(""), factorization.getSuffixes());
        assertNull(factorization.getAll());
    }

    @Test
    void sherlock() {
        var node = RegexParser.parse("([Ss]herlock)");
        var factors = node.bestFactors();
        assertEquals(Optional.of(List.of('S', 's')), factors.getInitialChars());
    }

    @Test
    void holmesWithin25CharactersOfWatson() {
        var node = RegexParser.parse("Holmes.{0,25}Watson|Watson.{0,25}Holmes");
        var factors = node.bestFactors();
        var requiredFactors = factors.getRequiredFactors();
        assertTrue(requiredFactors.contains("Holmes"));
        assertTrue(requiredFactors.contains("Watson"));
    }
}
