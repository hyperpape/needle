package com.justinblank.strings;

import com.justinblank.strings.RegexAST.Node;
import com.justinblank.strings.RegexAST.NodePrinter;
import com.justinblank.strings.Search.SearchMethod;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.quicktheories.QuickTheory;
import org.quicktheories.generators.StringsDSL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import static com.justinblank.strings.SearchMethodTestUtil.*;
import static org.junit.jupiter.api.Assertions.*;

public class IntegrationTest {

    static final String MANY_STATE_REGEX_STRING = "((123)|(234)|(345)|(456)){1,24}";

    @Test
    void emptyString() {
        SearchMethod searchMethod = NFA.createNFA("");
        find(searchMethod, "");
        find(searchMethod, "abc");
        DFA dfa = DFA.createDFA("");
        assertTrue(dfa.matches(""));
    }

    @Test
    void emptyStringUnion() {
        SearchMethod searchMethod = NFA.createNFA("()|(abc)");
        find(searchMethod, "", 0, 0);
        find(searchMethod, "def", 0, 0);
        find(searchMethod, "abc", 0, 3);
        find(searchMethod, "abcT", 0, 3);
        find(searchMethod, "Tabc", 0, 0);
        DFA dfa = DFA.createDFA("");
        assertTrue(dfa.matches(""));
    }

    @Test
    void concatenatedRangesWithRepetition() {
        DFA dfa = DFA.createDFA("[A-Za-z][A-Za-z0-9]*");
        assertTrue(dfa.matches("ABC0123"));
    }

    @Test
    void concatenatedRangesWithRepetitionSearch() {
        DFA dfa = DFA.createDFA("[A-Za-z][A-Za-z0-9]*");
        MatchResult result = dfa.search("ABC0123");
        assertEquals(0, result.start);
        assertEquals(7, result.end);

        dfa = DFA.createDFA("[^A-Za-z][A-Za-z0-9]*");
        result = dfa.search("%ABC0123");
        assertEquals(MatchResult.success(0, 8), result);
    }

    @Test
    void nonContiguousRanges() {
        SearchMethod method = NFA.createNFA("[0-13-46-7]");
        assertTrue(method.matches("0"));
        assertTrue(method.matches("1"));
        assertTrue(method.matches("3"));
        assertTrue(method.matches("4"));
        assertTrue(method.matches("6"));
        assertTrue(method.matches("7"));

        assertFalse(method.matches("2"));
        assertFalse(method.matches("5"));
        assertFalse(method.matches("8"));
    }

    @Test
    void unionWithRepetitionDFAMatchesOneRepetition() {
        DFA dfa = DFA.createDFA("((123)|(234)){1,24}");
        assertTrue(dfa.matches("123"));
    }

    @Test
    void upperAndLowerCaseRange() {
        var method = NFA.createNFA("[S-l]");
        match(method, "g");
    }

    @Test
    void upperAndLowerCaseRangeWithCountedRepetition() {
        var method = NFA.createNFA("[S-l]{0,2}");
        match(method, "g");
    }

    @Test
    void triplyRepeatedConcatenatedCharPlusCharRange() {
        var method = NFA.createNFA("(((((`)([m-o]))*)*)*)(a)");
        match(method, "`ma");
    }

    @Test
    void backslashAsPartOfCharRange() {
        SearchMethod method = NFA.createNFA("[\\\\-r]");
        match(method, "\\");
        match(method, "n");
        match(method, "r");
    }

    @Test
    void unionWithRepetitionDFAMatchesMultipleRepetitions() {
        DFA dfa = DFA.createDFA("((123)|(234)){1,24}");
        assertTrue(dfa.matches("123234234123"));
    }

    @Test
    void ranges() {
        DFA dfa = DFA.createDFA("[A-Za-z]");
        assertTrue(dfa.matches("C"));
        assertTrue(dfa.matches("c"));
        assertFalse(dfa.matches("5"));
    }

    @Test
    void rangesSearch() {
        DFA dfa = DFA.createDFA("[A-Za-z]");
        assertEquals(0, dfa.search("c").start);
        assertEquals(1, dfa.search("c").end);
        assertFalse(dfa.search("5").matched);
    }

    @Test
    void nfaRepetition() {
        SearchMethod method = NFA.createNFA("C*");
        match(method, "");
        match(method, "C");
        match(method, "CCCCCC");
        find(method, "ACC", 2, 3);
    }

    @Test
    void dfaRepetition() {
        DFA dfa = DFA.createDFA("C*");
        assertTrue(dfa.matches(""));
        assertTrue(dfa.matches("C"));
        assertTrue(dfa.matches("CCCCCC"));
    }

    @Test
    void dfaRepetitionSearch() {
        DFA dfa = DFA.createDFA("C*");
        assertTrue(dfa.search("").matched);
        assertTrue(dfa.search("C").matched);
        assertTrue(dfa.search("CCCCCC").matched);
        assertTrue(dfa.search("DD").matched);
        assertTrue(dfa.search("DDC").matched);
        assertTrue(dfa.search("DDCCCCDD").matched);
    }

    @Test
    void dfaCountedRepetitionSearch() {
        DFA dfa = DFA.createDFA("C+");
        assertTrue(dfa.search("C").matched);
        assertTrue(dfa.search("CCCCCC").matched);
        assertTrue(dfa.search("DDC").matched);
        assertTrue(dfa.search("DDCCCCDD").matched);
        assertFalse(dfa.search("DD").matched);
    }

    @Test
    void dfaUnion() {
        DFA dfa = DFA.createDFA("A|B");
        assertTrue(dfa.matches("A"));
        assertTrue(dfa.matches("B"));
        assertFalse(dfa.matches("AB"));
    }

    @Test
    void dfaUnionSearch() {
        DFA dfa = DFA.createDFA("A|B");
        assertTrue(dfa.search("A").matched);
        assertTrue(dfa.search("B").matched);
        MatchResult matchResult = dfa.search("AB");
        assertEquals(0, matchResult.start);
        assertEquals(1, matchResult.end);

        matchResult = dfa.search("CDAB");
        assertEquals(2, matchResult.start);
        assertEquals(3, matchResult.end);
    }

    @Test
    void dfaUnionThreeOptions() {
        DFA dfa = DFA.createDFA("A|B|C");
        assertTrue(dfa.matches("B"));
    }

    @Test
    void groupedDFAUnion() {
        DFA dfa = DFA.createDFA("(AB)|(BA)");
        assertTrue(dfa.matches("AB"));
        assertTrue(dfa.matches("BA"));
        assertFalse(dfa.matches("ABBA"));
    }

    @Test
    void dfaUnionThreeGroupsSameSize() {
        DFA dfa = DFA.createDFA("(AB)|(BC)|(CD)");
        assertTrue(dfa.matches("BC"));
    }

    @Test
    void dfaUnionThreeGroupsDifferentSizes() {
        DFA dfa = DFA.createDFA("(AB)|(BC)|(CDE)");
        assertTrue(dfa.matches("BC"));
    }

    @Test
    void dfaUnionGroupsWithRepetition() {
        DFA dfa = DFA.createDFA("((AB)|(BC)){1,2}");
        assertFalse(dfa.matches(""));
        assertTrue(dfa.matches("AB"));
        assertFalse(dfa.matches("DE"));
        assertTrue(dfa.matches("ABBC"));
        assertFalse(dfa.matches("ABBCAB"));
    }

    @Test
    void groupedDFAUnionWithDifferentSizedGroupsAndRepetitions() {
        String regex = "((A)|(B)|(CD)){1,2}";
        SearchMethod method = NFA.createNFA(regex);
        find(method, "AB", 0, 1);
        find(method, "AB", 0, 2);
        DFA dfa = DFA.createDFA(regex);
        assertFalse(dfa.matches(""));
        assertTrue(dfa.matches("A"));
        assertTrue(dfa.matches("B"));
        assertTrue(dfa.matches("CD"));

        assertTrue(dfa.matches("AB"));
        assertTrue(dfa.matches("ACD"));

        method = NFA.createReversedNFANoAhoCorasick(regex);
        find(method, "BA", 0, 1);
        find(method, "BA", 0, 2);
    }

    @Test
    void groupedDFAUnionSearch() {
        DFA dfa = DFA.createDFA("(AB)|(BA)");
        assertTrue(dfa.search("AB").matched);
        assertTrue(dfa.search("BA").matched);

        MatchResult result = dfa.search("ABBA");
        assertEquals(0, result.start);
        assertEquals(2, result.end);
    }

    @Test
    void unionOfUnionFollowedByLiteralWithLiteral() {
        String test = "(a|a)a|ab";
        check(test);
        var dfa = DFA.createDFA(test);
        assertTrue(dfa.matches("aa"));
        assertTrue(dfa.matches("ab"));

        assertFalse(dfa.matches("b"));
        assertFalse(dfa.matches("aab"));
    }

    @Test
    void manyStateRegex() {
        SearchMethod nfa = NFA.createNFA(MANY_STATE_REGEX_STRING);
        find(nfa, "456", 0, 3);
        find(nfa, "234234", 0, 6);
        fail(nfa, "");
        DFA dfa = DFA.createDFA(MANY_STATE_REGEX_STRING);
        assertTrue(dfa.matches("456"));
        assertFalse(dfa.matches(""));
        assertTrue(dfa.matches("234234"));

        nfa = NFA.createReversedNFANoAhoCorasick(MANY_STATE_REGEX_STRING);
        assertTrue(nfa.matches("432432"));
    }

    @Test
    void manyStateRegexWithLiteralSuffix() {
        String regexString = MANY_STATE_REGEX_STRING + "ab";
        SearchMethod method = NFA.createNFA(regexString);
        match(method, "456ab");
        match(method, "234234ab");
        fail(method, "");
        DFA dfa = DFA.createDFA(regexString);
        assertTrue(dfa.matches("456ab"));
        assertFalse(dfa.matches(""));
        assertTrue(dfa.matches("234234ab"));
    }

    @Test
    void repetitionWithLiteralSuffix() {
        String regexString = "((12)|(23)){1,2}" + "ab";
        SearchMethod method = NFA.createNFA(regexString);
        match(method, "12ab");
        match(method, "23ab");
        fail(method, "");
        fail(method, "ghij");
        fail(method, "1aghij");
        DFA dfa = DFA.createDFA(regexString);
        assertTrue(dfa.matches("12ab"));
        assertFalse(dfa.matches(""));
        assertTrue(dfa.matches("23ab"));
    }

    @Test
    void manyStateDFASearch() {
        String regexString = "(123)|(234)|(345)|(456)|(567)|(678)|(789)|(0987)|(9876)|(8765)|(7654)|(6543)|(5432)|(4321)|(3210){1,24}";
        SearchMethod method = NFA.createNFA(regexString);
        match(method, "567");
        fail(method, "");
        match(method, "32103210");
        DFA dfa = DFA.createDFA(regexString);
        assertTrue(dfa.search("567").matched);
        assertFalse(dfa.search("").matched);
        assertTrue(dfa.search("32103210").matched);
    }

    @Test
    void countedRepetitionOneChar() {
        String regexString = "1{1,1}";
        Node node = RegexParser.parse(regexString);
        SearchMethod searchMethod = NFA.createNFA(regexString);
        match(searchMethod, "1");
        fail(searchMethod, "");
        assertFalse(searchMethod.matches("11"));
    }

    @Test
    void zeroBoundCountedRepetition() {
        String regexString = "1{0,2}";
        SearchMethod method = NFA.createNFA(regexString); // TODO: fix AsciiAhoCorasick
        match(method, "");
        match(method, "1");
        match(method, "11");
        find(method, "111");
        assertFalse(method.matches("111"));
    }

    @Test
    void countedRepetition() {
        String regexString = "(1|2){2,3}abc";
        SearchMethod nfa = NFA.createNFA(regexString);
        match(nfa, "12abc");
        match(nfa, "121abc");
        fail(nfa, "");
        fail(nfa, "1abc");
        assertFalse(nfa.matches("1221abc"));
        fail(nfa, "12def");
        DFA dfa = DFA.createDFA(regexString);
        assertTrue(dfa.matches("12abc"));
        assertTrue(dfa.matches("121abc"));
        assertFalse(dfa.matches(""));
        assertFalse(dfa.matches("1abc"));
        assertFalse(dfa.matches("1211abc"));
        assertFalse(dfa.matches("12def"));
    }

    @Test
    void ab_plus() {
        String regexString = "(ab)+";
        Node node = RegexParser.parse(regexString);
        SearchMethod nfa = NFA.createNFA(regexString);
        match(nfa, "ab");
        match(nfa, "ababab");
        fail(nfa, "");
        DFA dfa = DFA.createDFA(regexString);
        assertTrue(dfa.matches("ab"));
        assertTrue(dfa.matches("abab"));
        assertFalse(dfa.matches(""));
    }

    @Test
    void a_plus() {
        String regexString = "a+";
        SearchMethod nfa = NFA.createNFA(regexString);
        match(nfa, "a");
        match(nfa, "aaaaaaa");
        fail(nfa, "");
        DFA dfa = DFA.createDFA(regexString);
        assertTrue(dfa.matches("a"));
        assertTrue(dfa.matches("aaaaaaa"));
        assertFalse(dfa.matches(""));
    }

    @Test
    void a_orb_orc_plus() {
        String regexString = "[a-c]+";
        SearchMethod searchMethod = NFA.createNFA(regexString);
        match(searchMethod, "a");
        match(searchMethod, "c");
        match(searchMethod, "abacbabababbbabababa");
        fail(searchMethod, "");

        regexString = "[^a-c]+";
        searchMethod = NFA.createNFA(regexString);
        match(searchMethod, "d");
        match(searchMethod, "A");
        fail(searchMethod, "a");
        fail(searchMethod, "c");
        match(searchMethod, "defghijklmnopqrst");
        fail(searchMethod, "");
    }

    @Test
    void concatenatedMultiRegex() {
        String regexString = "[a-zA-Z@#]";
        SearchMethod searchMethod = NFA.createNFA(regexString);
        match(searchMethod, "a");
        match(searchMethod, "@");
        match(searchMethod, "#");
        fail(searchMethod, "");
        assertFalse(searchMethod.matches("ab"));
        assertFalse(searchMethod.matches("a#"));

        regexString = "[^a-zA-Z@#]";
        searchMethod = NFA.createNFA(regexString);
        match(searchMethod, "%");
        fail(searchMethod, "a");
        fail(searchMethod, "@");
        fail(searchMethod, "#");
        fail(searchMethod, "");
        assertFalse(searchMethod.matches("%^"));
        assertFalse(searchMethod.matches("{}"));
    }

    @Test
    void unionOfRepeatedRangeWithOverlappingRangeFollowedByLiteral() {
        check("([BQ]*|[Q-x])l");
    }

    @Test
    void period() {
        SearchMethod method = NFA.createNFA(".");
        match(method, "a");
        match(method, "%");
        assertFalse(method.matches("abcd"));
        find(method, "abcdef");
        find(method, "%#$(");
        QuickTheory.qt().forAll(new StringsDSL().allPossible().ofLengthBetween(1, 1)).check(method::matches);
    }

    @Test
    void greedySearchOfOverlappingStrings() {
        SearchMethod nfa = NFA.createNFA("(EAD)|(DEAD)");
        find(nfa, "DEAD");
        assertEquals(MatchResult.success(0, 4), nfa.find("DEAD"));
    }

    @Test
    void theSpaceWord() {
        SearchMethod pattern = NFA.createNFA("the\\s+\\w+");
        find(pattern, "the a");
        find(pattern, "the art");
        find(pattern, "the   art");
        find(pattern, "the   art ");

        find(pattern, " the a");
        find(pattern, " the art");
        find(pattern, " the   art");
        find(pattern, " the   art ");
        find(pattern, "a the u");

        fail(pattern, "the");
        fail(pattern, "the ");
        fail(pattern, "the    ");
        fail(pattern, "theart");
    }

    @Test
    void dfaFailure() {
        String regex = "(([b-e]|[T-_])*)e";
        var dfa = DFA.createDFA(regex);
        assertTrue(dfa.matches("e"));
    }

    @Test
    // TODO: make NFAs support same behavior as DFAs
    @Disabled
    void fileBasedTests() throws Exception {
        var patterns = new HashMap<String, SearchMethod>();
        var testSpecs = new RegexTestSpecParser().readTests();
        var correctMatches = 0;
        var nonMatches = 0;
        var errors = new ArrayList<String>();
        for (var spec : testSpecs) {
            var pattern = patterns.computeIfAbsent(spec.pattern, (p) -> NFA.createNFA(spec.pattern));
            if (spec.successful) {
                // TODO: record failures, rather than dying immediately,
                var result = pattern.matcher(spec.target).find();
                if (!result.matched) {
                    errors.add("Matching spec=" + spec.pattern + " against needle=" + spec.target + " failed: expected start=" + spec.start + ", expected end=" + spec.end);
                }
                else {
                    if (result.start != spec.start || result.end != spec.end) {
                        errors.add("Matching spec=" + spec.pattern + " against needle=" + spec.target + " had incorrect indexes, expected start=" + spec.start + ", expected end=" + spec.end + ", actualStart=" + result.start + ", actualEnd=" + result.end);
                        nonMatches++;
                    } else {
                        correctMatches++;
                    }
                }
            }
            else {
                nonMatches++;
            }
        }
        if (errors.size() > 0) {
            for (var error : errors) {
                System.out.println(error);
            }
            fail("Errors in file based tests");
        }
        // If these fail, then the test parsing is broken
        assertNotEquals(0, correctMatches);
        assertNotEquals(0, nonMatches);
    }

    public void check(String regex) {
        RegexGenerator regexGenerator = new RegexGenerator(new Random(), 1);
        Node node = RegexParser.parse(regex);
        SearchMethod method = NFA.createNFA(regex);
        var javaPattern = java.util.regex.Pattern.compile(regex);
        String shortestFailure = null;
        for (int i = 0; i < 10000; i++) {
            String s = regexGenerator.generateString(node);
            if (shortestFailure == null || s.length() < shortestFailure.length()) {
                MatchResult result = method.find(s);
                if (result.start != 0 || result.end != s.length()) {
                    shortestFailure = s;
                }
                else if (!javaPattern.matcher(s).matches()) {
                    fail("Regex=" + regex + " matched hayStack that java regex didn't, hayStack=" + s);
                }
            }
        }
        assertNull(shortestFailure);
    }

    @Test
    void generativeNFAMatchingTest() {
        Random random = new Random();
        for (int maxSize = 1; maxSize < 24; maxSize++) {
            for (int i = 0; i < 20; i++) {
                RegexGenerator regexGenerator = new RegexGenerator(random, maxSize);
                Node node = regexGenerator.generate();
                String regex = NodePrinter.print(node);
                String hayStack = regexGenerator.generateString(node);
                try {
                    match(NFA.createNFA(regex), hayStack);
                } catch (Throwable t) {
                    System.out.println("failed to match regex='" + regex + "' against hayStack='" + hayStack + "'");
                    throw t;
                }
            }
        }
    }

    @Test
    void generativeDFAMatchingTest() {
        Random random = new Random();
        for (int maxSize = 1; maxSize < 8; maxSize++) {
            for (int i = 0; i < 20; i++) {
                RegexGenerator regexGenerator = new RegexGenerator(random, maxSize);
                Node node = regexGenerator.generate();
                String regex = NodePrinter.print(node);
                String hayStack = regexGenerator.generateString(node);
                try {
                    assertTrue(DFA.createDFA(regex).matches(hayStack));
                    assertTrue(java.util.regex.Pattern.compile(regex).matcher(hayStack).matches());
                } catch (Throwable t) {
                    System.out.println("failed to match regex='" + regex + "' against hayStack='" + hayStack + "'");
                    throw t;
                }
            }
        }
    }
}
