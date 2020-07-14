package com.justinblank.strings;

import com.justinblank.strings.RegexAST.Node;
import com.justinblank.strings.Search.SearchMethod;
import org.junit.Test;
import org.quicktheories.QuickTheory;
import org.quicktheories.generators.StringsDSL;

import static com.justinblank.strings.SearchMethodTestUtil.*;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

public class IntegrationTest {

    static final String MANY_STATE_REGEX_STRING = "((123)|(234)|(345)|(456)){1,24}";

    @Test
    public void testEmptyString() {
        SearchMethod searchMethod = NFA.createNFA("");
        find(searchMethod, "");
        find(searchMethod, "abc");
        DFA dfa = DFA.createDFA("");
        assertTrue(dfa.matches(""));
    }

    @Test
    public void testEmptyStringAlternation() {
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
    public void testConcatenatedRangesWithRepetition() {
        DFA dfa = DFA.createDFA("[A-Za-z][A-Za-z0-9]*");
        assertTrue(dfa.matches("ABC0123"));
    }

    @Test
    public void testConcatenatedRangesWithRepetitionSearch() {
        DFA dfa = DFA.createDFA("[A-Za-z][A-Za-z0-9]*");
        MatchResult result = dfa.search("ABC0123");
        assertEquals(0, result.start);
        assertEquals(7, result.end);

        dfa = DFA.createDFA("[^A-Za-z][A-Za-z0-9]*");
        result = dfa.search("%ABC0123");
        assertEquals(MatchResult.success(0, 8), result);
    }

    @Test
    public void testNonContiguousRanges() {
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
    public void testAlternationWithRepetitionDFAMatchesOneRepetition() {
        DFA dfa = DFA.createDFA("((123)|(234)){1,24}");
        assertTrue(dfa.matches("123"));
    }

    @Test
    public void testUpperAndLowerCaseRange() {
        var method = NFA.createNFA("[S-l]");
        match(method, "g");
    }

    @Test
    public void testUpperAndLowerCaseRangeWithCountedRepetition() {
        var method = NFA.createNFA("[S-l]{0,2}");
        match(method, "g");
    }

    @Test
    public void testTriplyRepeatedConcatenatedCharPlusCharRange() {
        var method = NFA.createNFA("(((((`)([m-o]))*)*)*)(a)");
        match(method, "`ma");
    }

    @Test
    public void testBackslashAsPartOfCharRange() {
        SearchMethod method = NFA.createNFA("[\\-r]");
        assertTrue(method.matches("n"));
    }

    @Test
    public void testAlternationWithRepetitionDFAMatchesMultipleRepetitions() {
        DFA dfa = DFA.createDFA("((123)|(234)){1,24}");
        assertTrue(dfa.matches("123234234123"));
    }

    @Test
    public void testRanges() {
        DFA dfa = DFA.createDFA("[A-Za-z]");
        assertTrue(dfa.matches("C"));
        assertTrue(dfa.matches("c"));
        assertFalse(dfa.matches("5"));
    }

    @Test
    public void testRangesSearch() {
        DFA dfa = DFA.createDFA("[A-Za-z]");
        assertEquals(0, dfa.search("c").start);
        assertEquals(1, dfa.search("c").end);
        assertFalse(dfa.search("5").matched);
    }

    @Test
    public void testNFARepetition() {
        SearchMethod method = NFA.createNFA("C*");
        match(method, "");
        match(method, "C");
        match(method, "CCCCCC");
        find(method, "ACC", 2, 3);
    }

    @Test
    public void testDFARepetition() {
        DFA dfa = DFA.createDFA("C*");
        assertTrue(dfa.matches(""));
        assertTrue(dfa.matches("C"));
        assertTrue(dfa.matches("CCCCCC"));
    }

    @Test
    public void testDFARepetitionSearch() {
        DFA dfa = DFA.createDFA("C*");
        assertTrue(dfa.search("").matched);
        assertTrue(dfa.search("C").matched);
        assertTrue(dfa.search("CCCCCC").matched);
        assertTrue(dfa.search("DD").matched);
        assertTrue(dfa.search("DDC").matched);
        assertTrue(dfa.search("DDCCCCDD").matched);
    }

    @Test
    public void testDFACountedRepetitionSearch() {
        DFA dfa = DFA.createDFA("C+");
        assertTrue(dfa.search("C").matched);
        assertTrue(dfa.search("CCCCCC").matched);
        assertTrue(dfa.search("DDC").matched);
        assertTrue(dfa.search("DDCCCCDD").matched);
        assertFalse(dfa.search("DD").matched);
    }

    @Test
    public void testDFAAlternation() {
        DFA dfa = DFA.createDFA("A|B");
        assertTrue(dfa.matches("A"));
        assertTrue(dfa.matches("B"));
        assertFalse(dfa.matches("AB"));
    }

    @Test
    public void testDFAAlternationSearch() {
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
    public void testDFAAlternationThreeOptions() {
        DFA dfa = DFA.createDFA("A|B|C");
        assertTrue(dfa.matches("B"));
    }

    @Test
    public void testGroupedDFAAlternation() {
        DFA dfa = DFA.createDFA("(AB)|(BA)");
        assertTrue(dfa.matches("AB"));
        assertTrue(dfa.matches("BA"));
        assertFalse(dfa.matches("ABBA"));
    }

    @Test
    public void testDFAAlternationThreeGroupsSameSize() {
        DFA dfa = DFA.createDFA("(AB)|(BC)|(CD)");
        assertTrue(dfa.matches("BC"));
    }

    @Test
    public void testDFAAlternationThreeGroupsDifferentSizes() {
        DFA dfa = DFA.createDFA("(AB)|(BC)|(CDE)");
        assertTrue(dfa.matches("BC"));
    }

    @Test
    public void testDFAAlternationGroupsWithRepetition() {
        DFA dfa = DFA.createDFA("((AB)|(BC)){1,2}");
        assertFalse(dfa.matches(""));
        assertTrue(dfa.matches("AB"));
        assertFalse(dfa.matches("DE"));
        assertTrue(dfa.matches("ABBC"));
        assertFalse(dfa.matches("ABBCAB"));
    }

    @Test
    public void testGroupedDFAAlternationWithDifferentSizedGroupsAndRepetitions() {
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
    public void testGroupedDFAAlternationSearch() {
        DFA dfa = DFA.createDFA("(AB)|(BA)");
        assertTrue(dfa.search("AB").matched);
        assertTrue(dfa.search("BA").matched);

        MatchResult result = dfa.search("ABBA");
        assertEquals(0, result.start);
        assertEquals(2, result.end);
    }

    @Test
    public void testManyStateRegex() {
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
    public void testManyStateRegexWithLiteralSuffix() {
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
    public void testRepetitionWithLiteralSuffix() {
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
    public void testManyStateDFASearch() {
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
    public void testCountedRepetitionOneChar() {
        String regexString = "1{1,1}";
        Node node = RegexParser.parse(regexString);
        SearchMethod searchMethod = NFA.createNFA(regexString);
        match(searchMethod, "1");
        fail(searchMethod, "");
        assertFalse(searchMethod.matches("11"));
    }

    @Test
    public void testZeroBoundCountedRepetition() {
        String regexString = "1{0,2}";
        SearchMethod method = NFA.createNFA(regexString); // TODO: fix AsciiAhoCorasick
        match(method, "");
        match(method, "1");
        match(method, "11");
        find(method, "111");
        assertFalse(method.matches("111"));
    }

    @Test
    public void testCountedRepetition() {
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
    public void test_ab_PLUS() {
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
    public void test_a_PLUS() {
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
    public void test_aORbORc_PLUS() {
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
    public void testConcatenatedMultiRegex() {
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
    public void testPeriod() {
        SearchMethod method = NFA.createNFA(".");
        match(method, "a");
        match(method, "%");
        assertFalse(method.matches("abcd"));
        find(method, "abcdef");
        find(method, "%#$(");
        QuickTheory.qt().forAll(new StringsDSL().allPossible().ofLengthBetween(1, 1)).check(method::matches);
    }

    @Test
    public void testGreedySearchOfOverlappingStrings() {
        SearchMethod nfa = NFA.createNFA("(EAD)|(DEAD)");
        find(nfa, "DEAD");
        assertEquals(MatchResult.success(0, 4), nfa.find("DEAD"));
    }
}
