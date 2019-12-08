package com.justinblank.strings;

import com.justinblank.strings.RegexAST.Node;
import com.justinblank.strings.Search.SearchMethod;
import org.junit.Test;

import static com.justinblank.strings.SearchMethodTestUtil.*;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class IntegrationTest {

    // TODO: Parser change fixed a bug. Previous version had fewer states than it should've, larger state space causes
    //  massive slowdown, see if this can be fixed
    public static final String MANY_STATE_REGEX_STRING = "((123)|(234)|(345)|(456)){1,24}";
    // public static final String MANY_STATE_REGEX_STRING = "((123)|(234)|(345)|(456)|(567)|(678)|(789)|(0987)|(9876)|(8765)|(7654)|(6543)|(5432)|(4321)|(3210)){1,24}";

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
        Node node = RegexParser.parse("[A-Za-z][A-Za-z0-9]*");
        DFA dfa = NFAToDFACompiler.compile(ThompsonNFABuilder.createNFA(node));
        assertTrue(dfa.matches("ABC0123"));
    }

    @Test
    public void testConcatenatedRangesWithRepetitionSearch() {
        Node node = RegexParser.parse("[A-Za-z][A-Za-z0-9]*");
        DFA dfa = NFAToDFACompiler.compile(ThompsonNFABuilder.createNFA(node));
        MatchResult result = dfa.search("ABC0123");
        assertEquals(0, result.start);
        assertEquals(7, result.end);
    }

    @Test
    public void testSillyNonContiguousRanges() {
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
    public void testAlternationWithRepetitionDFAMatchesMultipleRepetitions() {
        DFA dfa = DFA.createDFA("((123)|(234)){1,24}");
        assertTrue(dfa.matches("123234234123"));
    }

    @Test
    public void testRanges() {
        Node node = RegexParser.parse("[A-Za-z]");
        DFA dfa = NFAToDFACompiler.compile(ThompsonNFABuilder.createNFA(node));
        assertTrue(dfa.matches("C"));
        assertTrue(dfa.matches("c"));
        assertFalse(dfa.matches("5"));
    }

    @Test
    public void testRangesSearch() {
        Node node = RegexParser.parse("[A-Za-z]");
        DFA dfa = NFAToDFACompiler.compile(ThompsonNFABuilder.createNFA(node));
        assertEquals(0, dfa.search("c").start);
        assertEquals(1, dfa.search("c").end);
        assertFalse(dfa.search("5").matched);
    }

    @Test
    public void testNFARepetition() {
        Node node = RegexParser.parse("C*");
        NFA nfa = ThompsonNFABuilder.createNFA(node);
        match(nfa, "");
        match(nfa, "C");
        match(nfa, "CCCCCC");
        find(nfa, "ACC", 2, 3);
    }

    @Test
    public void testDFARepetition() {
        Node node = RegexParser.parse("C*");
        DFA dfa = NFAToDFACompiler.compile(ThompsonNFABuilder.createNFA(node));
        assertTrue(dfa.matches(""));
        assertTrue(dfa.matches("C"));
        assertTrue(dfa.matches("CCCCCC"));
    }

    @Test
    public void testDFARepetitionSearch() {
        Node node = RegexParser.parse("C*");
        DFA dfa = NFAToDFACompiler.compile(ThompsonNFABuilder.createNFA(node));
        assertTrue(dfa.search("").matched);
        assertTrue(dfa.search("C").matched);
        assertTrue(dfa.search("CCCCCC").matched);
        assertTrue(dfa.search("DD").matched);
        assertTrue(dfa.search("DDC").matched);
        assertTrue(dfa.search("DDCCCCDD").matched);
    }

    @Test
    public void testDFACountedRepetitionSearch() {
        Node node = RegexParser.parse("C+");
        DFA dfa = NFAToDFACompiler.compile(ThompsonNFABuilder.createNFA(node));
        assertTrue(dfa.search("C").matched);
        assertTrue(dfa.search("CCCCCC").matched);
        assertTrue(dfa.search("DDC").matched);
        assertTrue(dfa.search("DDCCCCDD").matched);
        assertFalse(dfa.search("DD").matched);
    }

    @Test
    public void testDFAAlternation() {
        Node node = RegexParser.parse("A|B");
        DFA dfa = NFAToDFACompiler.compile(ThompsonNFABuilder.createNFA(node));
        assertTrue(dfa.matches("A"));
        assertTrue(dfa.matches("B"));
        assertFalse(dfa.matches("AB"));
    }

    @Test
    public void testDFAAlternationSearch() {
        Node node = RegexParser.parse("A|B");
        DFA dfa = NFAToDFACompiler.compile(ThompsonNFABuilder.createNFA(node));
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
        Node node = RegexParser.parse("(AB)|(BA)");
        DFA dfa = NFAToDFACompiler.compile(ThompsonNFABuilder.createNFA(node));
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
        NFA nfa = ThompsonNFABuilder.createNFA(RegexParser.parse(regex));
        find(nfa, "AB", 0, 1);
        find(nfa, "AB", 0, 2);
        DFA dfa = DFA.createDFA(regex);
        assertFalse(dfa.matches(""));
        assertTrue(dfa.matches("A"));
        assertTrue(dfa.matches("B"));
        assertTrue(dfa.matches("CD"));

        assertTrue(dfa.matches("AB"));
        assertTrue(dfa.matches("ACD"));
    }

    @Test
    public void testGroupedDFAAlternationSearch() {
        Node node = RegexParser.parse("(AB)|(BA)");
        DFA dfa = NFAToDFACompiler.compile(ThompsonNFABuilder.createNFA(node));
        assertTrue(dfa.search("AB").matched);
        assertTrue(dfa.search("BA").matched);

        MatchResult result = dfa.search("ABBA");
        assertEquals(0, result.start);
        assertEquals(2, result.end);
    }

    @Test
    public void testManyStateRegex() {
        Node node = RegexParser.parse(MANY_STATE_REGEX_STRING);
        NFA nfa = ThompsonNFABuilder.createNFA(node);
        find(nfa, "456", 0, 3);
        find(nfa,"234234", 0, 6);
        fail(nfa, "");
        DFA dfa = NFAToDFACompiler.compile(nfa);
        assertTrue(dfa.matches("456"));
        assertFalse(dfa.matches(""));
        assertTrue(dfa.matches("234234"));
    }

    @Test
    public void testManyStateRegexWithLiteralSuffix() {
        String regexString = MANY_STATE_REGEX_STRING + "ab";
        Node node = RegexParser.parse(regexString);
        NFA nfa = ThompsonNFABuilder.createNFA(node);
        match(nfa,"456ab");
        match(nfa, "234234ab");
        fail(nfa, "");
        DFA dfa = NFAToDFACompiler.compile(nfa);
        assertTrue(dfa.matches("456ab"));
        assertFalse(dfa.matches(""));
        assertTrue(dfa.matches("234234ab"));
    }

    @Test
    public void testRepetitionWithLiteralSuffix() {
        String regexString = "((12)|(23)){1,2}" + "ab";
        Node node = RegexParser.parse(regexString);
        NFA nfa = ThompsonNFABuilder.createNFA(node);
        match(nfa, "12ab");
        match(nfa, "23ab");
        fail(nfa, "");
        fail(nfa, "ghij");
        fail(nfa, "1aghij");
        DFA dfa = NFAToDFACompiler.compile(nfa);
        assertTrue(dfa.matches("12ab"));
        assertFalse(dfa.matches(""));
        assertTrue(dfa.matches("23ab"));
    }

    @Test
    public void testManyStateDFASearch() {
        String regexString = "(123)|(234)|(345)|(456)|(567)|(678)|(789)|(0987)|(9876)|(8765)|(7654)|(6543)|(5432)|(4321)|(3210){1,24}";
        Node node = RegexParser.parse(regexString);
        NFA nfa = ThompsonNFABuilder.createNFA(node);
        match(nfa, "567");
        fail(nfa, "");
        match(nfa, "32103210");
        DFA dfa = NFAToDFACompiler.compile(nfa);
        assertTrue(dfa.search("567").matched);
        assertFalse(dfa.search("").matched);
        assertTrue(dfa.search("32103210").matched);
    }

    @Test
    public void testCountedRepetitionOneChar() {
        String regexString = "1{1,1}";
        Node node = RegexParser.parse(regexString);
        NFA nfa = ThompsonNFABuilder.createNFA(node);
        match(nfa, "1");
        fail(nfa, "");
        assertFalse(nfa.matches( "11"));
    }

    @Test
    public void testZeroBoundCountedRepetition() {
        String regexString = "1{0,2}";
        Node node = RegexParser.parse(regexString);
        NFA nfa = ThompsonNFABuilder.createNFA(node);
        match(nfa, "");
        match(nfa, "1");
        match(nfa, "11");
        find(nfa, "111");
        assertFalse(nfa.matches("111"));
    }

    @Test
    public void testCountedRepetition() {
        String regexString = "(1|2){2,3}abc";
        Node node = RegexParser.parse(regexString);
        NFA nfa = ThompsonNFABuilder.createNFA(node);
        match(nfa, "12abc");
        match(nfa, "121abc");
        fail(nfa, "");
        fail(nfa, "1abc");
        assertFalse(nfa.matches("1221abc"));
        fail(nfa, "12def");
        DFA dfa = NFAToDFACompiler.compile(nfa);
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
        NFA nfa = ThompsonNFABuilder.createNFA(node);
        match(nfa, "ab");
        match(nfa, "ababab");
        fail(nfa, "");
        DFA dfa = NFAToDFACompiler.compile(nfa);
        assertTrue(dfa.matches("ab"));
        assertTrue(dfa.matches("abab"));
        assertFalse(dfa.matches(""));
    }

    @Test
    public void test_a_PLUS() {
        String regexString = "a+";
        Node node = RegexParser.parse(regexString);
        NFA nfa = ThompsonNFABuilder.createNFA(node);
        match(nfa, "a");
        match(nfa, "aaaaaaa");
        fail(nfa, "");
        DFA dfa = NFAToDFACompiler.compile(nfa);
        assertTrue(dfa.matches("a"));
        assertTrue(dfa.matches("aaaaaaa"));
        assertFalse(dfa.matches(""));
    }

    @Test
    public void test_aORb_PLUS() {
        String regexString = "[a-c]+";
        Node node = RegexParser.parse(regexString);
        NFA nfa = ThompsonNFABuilder.createNFA(node);
        match(nfa, "a");
        match(nfa, "c");
        match(nfa, "abacbabababbbabababa");
        fail(nfa, "");
    }

    @Test
    public void testConcatenatedMultiRegex() {
        String regexString = "[a-zA-Z@#]";
        Node node = RegexParser.parse(regexString);
        NFA nfa = ThompsonNFABuilder.createNFA(node);
        match(nfa, "a");
        match(nfa, "@");
        match(nfa, "#");
        fail(nfa, "");
        assertFalse(nfa.matches( "ab"));
        assertFalse(nfa.matches( "a#"));
    }

    @Test
    public void testGreedySearchOfOverlappingStrings() {
        SearchMethod nfa = NFA.createNFA("(EAD)|(DEAD)");
        find(nfa, "DEAD");
        assertEquals(MatchResult.success(0, 4), nfa.find("DEAD"));
    }
}
