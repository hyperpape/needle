package com.justinblank.strings;

import com.justinblank.strings.RegexAST.Node;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.quicktheories.QuickTheory;
import org.quicktheories.core.Gen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static com.justinblank.strings.SearchMethodTestUtil.*;
import static org.junit.jupiter.api.Assertions.*;

public class DFACompilerTest {

    private static final AtomicInteger CLASS_NAME_COUNTER = new AtomicInteger();

    static final String CORE_LARGE_REGEX_STRING = "((123)|(234)|(345)|(456)|(567)|(678)|(789)|(0987)|(9876)|(8765)|(7654)|(6543)|(5432)|(4321)|(3210)){1,";

    // TODO: This is suboptimal, as it doesn't mix unicode and A-Z.
    static final Gen<String> ALPHABET = A_THROUGH_Z.ofLengthBetween(0, SMALL_DATA_SIZE)
            .mix(SMALL_BMP.ofLengthBetween(0, SMALL_DATA_SIZE));

    public static Pattern anonymousPattern(String regex) {
        return anonymousPattern(regex, false);
    }

    public static Pattern anonymousPattern(String regex, boolean debug) {
        var compilerOptions = new CompilerOptions(0,  CharacterDistribution.DEFAULT, debug ? DebugOptions.all() : DebugOptions.none());
        return DFACompiler.compile(regex, "Pattern" + CLASS_NAME_COUNTER.incrementAndGet(), compilerOptions);
    }

    // There are lots of painful little fencepost type errors possible as we start to experiment with inlining and
    // handling prefixes, so we'll explicitly test sizes 1-4
    @Test
    void singleCharLiteralRegex() {
        Pattern pattern = DFACompiler.compile("a", "SingleCharRegex");
        match(pattern, "a");
        fail(pattern, "b");

        assertFalse(pattern.matcher("ab").matches());
        find(pattern, "ab");
        find(pattern, "aba");

        assertFalse(pattern.matcher("ba").matches());
        find(pattern, "ba");
        find(pattern, "bad");

        fail(pattern, "AB{");

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "a", prefix, suffix);
            return true;
        });
    }

    @Test
    void multipleFindSingleCharLiteralRegex() {
        Pattern pattern = DFACompiler.compile("a", "SingleCharRegexMultipleFind");
        var matcher = pattern.matcher("aba");
        find(pattern, "ab");
        assertEquals(MatchResult.success(0, 1), pattern.matcher("ab").find());
        find(pattern, "aba");
        assertEquals(MatchResult.success(0, 1), matcher.find());
        assertEquals(MatchResult.success(2, 3), matcher.find());
        assertFalse(matcher.find().matched);
    }


    @Test
    void twoCharLiteralRegex() {
        Pattern pattern = DFACompiler.compile("xy", "TwoCharLiteralRegex");
        match(pattern, "xy");

        fail(pattern, "z");
        assertFalse(pattern.matcher("xyz").matches());
        assertTrue(pattern.matcher("xyz").containedIn());
        assertFalse(pattern.matcher("zxy").matches());
        assertTrue(pattern.matcher("zxy").containedIn());
        assertFalse(pattern.matcher("xzxy").matches());
        assertTrue(pattern.matcher("xzxy").containedIn());
        fail(pattern, "XY{");

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "xy", prefix, suffix);
            return true;
        });
    }

    @Test
    void threeCharLiteralRegex() {
        Pattern pattern = DFACompiler.compile("abc", "ThreeCharLiteralRegex");
        match(pattern, "abc");

        new ArrayList<>().addAll(new ArrayList<>());
        fail(pattern, "d");
        assertFalse(pattern.matcher("abcd").matches());
        assertTrue(pattern.matcher("abcd").containedIn());
        assertFalse(pattern.matcher("dabc").matches());
        assertTrue(pattern.matcher("dabc").containedIn());
        assertTrue(pattern.matcher("abdabc").containedIn());
        fail(pattern, "AB{");
        fail(pattern, "abd");

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "abc", prefix, suffix);
            return true;
        });
    }

    @Test
    void fourCharLiteralRegex() {
        Pattern pattern = DFACompiler.compile("abcd", "FourCharLiteralRegex");
        match(pattern, "abcd");

        fail(pattern, "e");
        assertFalse(pattern.matcher("abcde").matches());
        assertTrue(pattern.matcher("abcde").containedIn());
        assertFalse(pattern.matcher("eabcd").matches());
        assertTrue(pattern.matcher("eabcd").containedIn());
        fail(pattern, "ABC{");
        fail(pattern, "abc");
        fail(pattern, "abce");

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "abcd", prefix, suffix);
            return true;
        });
    }

    @Test
    void nineCharLiteralRegex() {
        Pattern pattern = DFACompiler.compile("abcdefghi", "NineCharLiteralRegex");
        match(pattern, "abcdefghi");

        fail(pattern, "abcd");
        fail(pattern, "abcdefgh");

        // Match initial state, pass by offset, but then hit zero state
        find(pattern, "a0cdefghi" + "abcdefghi");

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "abcdefghi", prefix, suffix);
            return true;
        });
    }

    @Test
    void literalRepetitionRegex() {
        Pattern pattern = DFACompiler.compile("a*", "aStarRegex");
        match(pattern, "");
        match(pattern, "a");
        match(pattern, "aa");
        match(pattern, "aaa");
        match(pattern, "aaaa");

        assertTrue(pattern.matcher("ab").containedIn());
        assertTrue(pattern.matcher("e").containedIn());

        find(pattern, "ba");

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "a", prefix, suffix);
            find(pattern, "aa", prefix, suffix);
            find(pattern, "aaa", prefix, suffix);
            find(pattern, "aaaa", prefix, suffix);
            return true;
        });
    }

    @Test
    void literalRepetitionLiteralRegex() {
        Pattern pattern = DFACompiler.compile("ad*g", "literalRepetitionLiteralRegex");
        match(pattern, "ag");
        match(pattern, "adg");
        match(pattern, "adddg");

        fail(pattern, "adeg");

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "ag", prefix, suffix);
            find(pattern, "adg", prefix, suffix);
            find(pattern, "addg", prefix, suffix);
            find(pattern, "adddg", prefix, suffix);
            return true;
        });
    }

    @Test
    void dfaCompiledSimpleRegex() {
        Pattern pattern = DFACompiler.compile("[0-9A-Za-z]*", "TestName");
        match(pattern, "AB09");

        match(pattern, "ABC09az");
        assertFalse(pattern.matcher("AB{").matches());
        assertTrue(pattern.matcher("AB{").containedIn());

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "AB", prefix, suffix);
            find(pattern, "BA", prefix, suffix);
            return true;
        });
    }

    @Test
    void groupedDFAUnion() {
        Pattern pattern = DFACompiler.compile("(AB)|(BA)", "testGroupedDFAUnion");
        match(pattern, "AB");
        match(pattern, "BA");

        assertFalse(pattern.matcher("ABBA").matches());
        assertTrue(pattern.matcher("ABBA").containedIn());

        fail(pattern, "A");
        fail(pattern, "AA");
        fail(pattern, "B");
        fail(pattern, "BB");

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "AB", prefix, suffix);
            find(pattern, "BA", prefix, suffix);
            return true;
        });
    }

    @Test
    void twoAcceptingStateDFA() throws Exception {
        Pattern pattern = DFACompiler.compile("(A+)|(B+)", "testTwoAcceptingStateDFA");
        match(pattern, "A");
        match(pattern, "B");
        match(pattern, "AA");
        match(pattern, "BB");
        fail(pattern, "");
        assertFalse(pattern.matcher("AB").matches());
        assertTrue(pattern.matcher("AB").containedIn());

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "A", prefix, suffix);
            find(pattern, "B", prefix, suffix);
            find(pattern, "AA", prefix, suffix);
            find(pattern, "BB", prefix, suffix);
            return true;
        });
    }

    @Test
    void dfaCompiledManyStateRegex() throws Exception {
        String regexString = IntegrationTest.MANY_STATE_REGEX_STRING;
        Pattern pattern = DFACompiler.compile(regexString, "testDFACompiledManyStateRegex");
        match(pattern, "456");
        match(pattern, "456456");

        fail(pattern, "");
        fail(pattern, "059{");

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "456", prefix, suffix);
            find(pattern, "456456", prefix, suffix);
            find(pattern, "123", prefix, suffix);
            return true;
        });
    }

    @Test
    void countedRepetitionSingleChar() {
        String regexString = "A{1,2}";
        Pattern pattern = DFACompiler.compile(regexString, "CountedRepetitionSingleCharRegex");
        match(pattern, "A");
        match(pattern, "AA");
        fail(pattern, "");
        fail(pattern, "B");
        fail(pattern, "BB");

        find(pattern, "BAB");

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "A", prefix, suffix);
            find(pattern, "AA", prefix, suffix);
            return true;
        });

    }

    @Test
    void countedRepetitionTwoChar() {
        String regexString = "(AB){1,2}";
        Pattern pattern = DFACompiler.compile(regexString, "CountedRepetitionRegexTwoCharRegex");
        match(pattern, "AB");
        match(pattern, "ABAB");
        fail(pattern, "");
        fail(pattern, "BB");
        fail(pattern, "AA");

        find(pattern, "AAB");
        assertFalse(pattern.matcher("ABABAB").matches());

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "AB", prefix, suffix);
            find(pattern, "ABAB", prefix, suffix);
            return true;
        });
    }

    @Test
    void countedRepetitionOfUnion() {
        String regexString = "((AB)|(BA)){1,2}";
        Pattern pattern = DFACompiler.compile(regexString, "CountedRepetitionOfUnion");
        fail(pattern, "");
        match(pattern, "BA");
        match(pattern, "ABBA");
        match(pattern, "BAAB");
        match(pattern, "BABA");
        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "BA", prefix, suffix);
            find(pattern, "ABBA", prefix, suffix);
            find(pattern, "BAAB", prefix, suffix);
            find(pattern, "BABA", prefix, suffix);
            return true;
        });
    }

    @Test
    void countedRepetitionWithLiteralSuffix() throws Exception {
        String regexString = "((AB)|(CD)){1,2}" + "AB";
        Pattern pattern = DFACompiler.compile(regexString, "GroupedRepetitionWithLiteralSuffix");
        var m = pattern.matcher("ABAB");
        match(pattern, "ABAB");
        match(pattern, "ABCDAB");
        match(pattern, "CDAB");
        match(pattern, "CDCDAB");
        fail(pattern, "");
        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "ABAB", prefix, suffix);
            find(pattern, "ABCDAB", prefix, suffix);
            find(pattern, "CDAB", prefix, suffix);
            find(pattern, "CDCDAB", prefix, suffix);
            return true;
        });
    }

    @Test
    void theSpaceWord() {
        Pattern pattern = DFACompiler.compile("the\\s+\\w+", "theSpaceWord");
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
    void manyStateRegexWithLiteralSuffix() {
        String regexString = IntegrationTest.MANY_STATE_REGEX_STRING + "ab";
        Pattern pattern = DFACompiler.compile(regexString, "ManyStateRegexWithLiteralSuffix");
        match(pattern, "123ab");
        match(pattern, "234234ab");
        fail(pattern, "");

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "123ab", prefix, suffix);
            find(pattern, "234234ab", prefix, suffix);
            return true;
        });
    }

    @Test
    void dfaCompiledDigit() throws Exception {
        Pattern pattern = DFACompiler.compile("[0-9]", "testDFACompiledDigit");
        match(pattern, "0");

        fail(pattern, "");
        assertFalse(pattern.matcher("0{").matches());
        assertTrue(pattern.matcher("1{").containedIn());
        assertTrue(pattern.matcher("0{").containedIn());

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "1{", prefix, suffix);
            find(pattern, "0{", prefix, suffix);
            return true;
        });
    }

    @Test
    void dfaCompiledDigitPlus() throws Exception {
        Pattern pattern = DFACompiler.compile("[0-9]+", "testDFACompiledDigitPlus");
        match(pattern, "0");

        fail(pattern, "");
        assertFalse(pattern.matcher("059{").matches());
        assertTrue(pattern.matcher("12{").containedIn());
        assertTrue(pattern.matcher("059{").containedIn());

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "12{", prefix, suffix);
            find(pattern, "059{", prefix, suffix);
            return true;
        });
    }

    @Test
    void dfaCompiledBMP() throws Exception {
        Pattern pattern = DFACompiler.compile("[\u0600-\u06FF]", "testDFACompiledBMP");
        match(pattern, "\u0600");
        fail(pattern, "AB{");
    }

    @Test
    void dfaCompiledUnionOfLiterals() throws Exception {
        Pattern pattern = DFACompiler.compile("A|BCD|E", "union1");
        match(pattern, "A");
        match(pattern, "BCD");
        match(pattern, "E");
        assertFalse(pattern.matcher("F").matches());
        assertFalse(pattern.matcher("F").containedIn());

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "A", prefix, suffix);
            find(pattern, "BCD", prefix, suffix);
            find(pattern, "E", prefix, suffix);
            return true;
        });
    }

    @Test
    void repeatedRangeOverlappingWithSuffix() throws Exception {
        Pattern pattern = DFACompiler.compile("[A-Za-z]+ab", "repeatedRangeOverlappingWithSuffix");
        match(pattern, "Aab");
        match(pattern, "aab");

        match(pattern, "AZDab");
        match(pattern, "AZDab");
        match(pattern, "ZDaab");

        var m = pattern.matcher("AaDab");
        match(pattern, "AaDab");

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern,"Aab", prefix, suffix);
            find(pattern, "aab", prefix, suffix);
            find(pattern, "AZDab", prefix, suffix);
            find(pattern, "AZDab", prefix, suffix);
            find(pattern, "aZDaab", prefix, suffix);
            return true;
        });
    }

    @Test
    void repeatedRangeWithIngSuffix() throws Exception {
        Pattern pattern = DFACompiler.compile("[A-Za-z]+ing", "repeatedRangeWithIngSuffix");
        match(pattern, "bing");
        match(pattern, "Bing");
        match(pattern, "zing");
        match(pattern, "Zing");
    }

    @Test
    void twoLargeRangesPrefixSuffixLiteral() throws Exception {
        Pattern pattern = DFACompiler.compile("[A-Za-z]+abcdef", "TwoLargeRangesPrefixWithSuffixLiteral");
        match(pattern, "Aabcdef");
        match(pattern, "aabcdef");

        match(pattern, "AZDabcdef");
        match(pattern, "AZDabcdef");
        match(pattern, "ZDaabcdef");

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "Aabcdef", prefix, suffix);
            find(pattern, "aabcdef", prefix, suffix);
            find(pattern, "AZDabcdef", prefix, suffix);
            find(pattern, "AZDabcdef", prefix, suffix);
            find(pattern, "aZDaabcdef", prefix, suffix);
            return true;
        });
    }

    @Test
    void prefixLargeRangeWithSuffixLiteral() {
        Pattern pattern = DFACompiler.compile("[A-Z]+abcdef", "PrefixLargeRangeWithSuffixLiteral");
        match(pattern, "Aabcdef");
        match(pattern, "AZDabcdef");

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "Aabcdef", prefix, suffix);
            find(pattern, "AZDabcdef", prefix, suffix);
            return true;
        });
    }

    @Test
    void largeRangePrefixWithInfixLiteralAndLargeRangeSuffix() {
        Pattern pattern = DFACompiler.compile("[A-Z]+abcdef[A-Z]+", "LargeRangePrefixWithInfixLiteralAndLargeRangeSuffix");
        match(pattern, "AabcdefZ");
        match(pattern, "AZDabcdefDZA");

        QuickTheory.qt().forAll(ALPHABET, ALPHABET).check((prefix, suffix) -> {
            find(pattern, "AabcdefZ", prefix, suffix);
            find(pattern, "AZDabcdefDZA", prefix, suffix);
            return true;
        });
    }

    @Test
    void overSimplifiedURLMatcher() {
        final Pattern pattern = DFACompiler.compile("http://.+", "OverSimplifiedURLMatcher");
        Matcher matcher = pattern.matcher("http://www.google.com");
        matcher.matches();
        matcher.containedIn();
        MatchResult matchResult = matcher.find();
        assertTrue(matchResult.matched);
        assertEquals(0, matchResult.start);
        assertEquals(21, matchResult.end);

        matcher = pattern.matcher("http://Γειά σου.com");
        matcher.matches();
        matcher.containedIn();
        matchResult = matcher.find();
        assertTrue(matchResult.matched);
        assertEquals(0, matchResult.start);
        assertEquals(19, matchResult.end);
    }

    @Test
    void holmesWithin25CharactersOfWatson() {
        final var pattern = DFACompiler.compile("Holmes.{0,25}Watson|Watson.{0,25}Holmes", "HolmesWithin25CharactersOfWatson");
        Matcher matcher = pattern.matcher("HolmesThenWatson");
        assertTrue(matcher.matches());
    }

    @Test
    void minimized() {
        final var pattern = DFACompiler.compile("AB.{0,2}12|AB.{0,2}12", "minimized");
        Matcher matcher = pattern.matcher("AB+12");
        assertTrue(matcher.matches());
    }

    // Has prefix with offset
    @Test
    void theCrown() {
        var pattern = DFACompiler.compile("the [Cc]rown", "theCrown");
        find(pattern, "the Crown");
    }

    @Test
    void dfaCompileFailsLargePattern() {
        String manyStateRegexString = "((123)|(234)|(345)|(456)|(567)|(678)|(789)|(0987)|(9876)|(8765)|(7654)|(6543)|(5432)|(4321)|(3210)){1,1000}";
        assertThrows(IllegalArgumentException.class, () ->
            DFACompiler.compile(manyStateRegexString, "tooBig"));
    }

    @Test
    void largeRegex() {
        String largeRegex = CORE_LARGE_REGEX_STRING + "4}";
        var pattern = DFACompiler.compile(largeRegex, "testLargeRegex4");
        var hayStack = "1232343450987";
        assertTrue(pattern.matcher(hayStack).matches());

        pattern = DFACompiler.compile(CORE_LARGE_REGEX_STRING + "8}", "testLargeRegex8");
        hayStack = hayStack + hayStack; // + hayStack + hayStack;
        assertTrue(pattern.matcher(hayStack).matches());
        assertTrue(pattern.matcher(hayStack).matches());
    }

    @Test
    void sameRegexCanGenerateByteBasedDFAClassesAndShortBasedDFAClasses() {
        var regex = ".{0,43}A";
        Node node = RegexParser.parse(regex, 0);

        NFA forwardNFA = new NFA(RegexInstrBuilder.createNFA(node));

        DFA dfa = NFAToDFACompiler.compile(forwardNFA, ConversionMode.BASIC, false);
        DFA dfaSearch = NFAToDFACompiler.compile(forwardNFA, ConversionMode.DFA_SEARCH, false);
        assertTrue(dfa.statesCount() <= Byte.MAX_VALUE);
        assertTrue(dfaSearch.statesCount() > Byte.MAX_VALUE);

        var pattern = anonymousPattern(regex);
        compareResultsToStandardLibrary("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@A", pattern, java.util.regex.Pattern.compile(regex));
    }

    @Test
    void findingIngWords() {
        String regexString = "[a-zA-Z]+ing";
        String hayStack = "the most perfect reasoning and observing machine that the world has seen";
        var pattern = DFACompiler.compile(regexString, "ingregex");
        var count = 0;
        var m = pattern.matcher(hayStack);
        while (m.find().matched) {
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    void fourCharOffsetWithoutPrefix() {
        var pattern = DFACompiler.compile("[a-q][^u-z]{3}x", "testFourCharOffsetWithoutPrefix");
        find(pattern, "aaaax");
    }

    @Test
    void sherlockStreetInFile() throws Exception {
        var path = Paths.get("src", "test", "resources", "sherlockholmes.txt");
        checkMatchesInFileAgainstStandardLibrary("Sherlock|Street", path);
    }

    @Test
    void upperOrLowercaseSherlockInFile() throws Exception {
        var path = Paths.get("src", "test", "resources", "sherlockholmes.txt");
        checkMatchesInFileAgainstStandardLibrary("[Ss]herlock", path);
    }

    @Test
    void handling_long_state_transition_strings() throws Exception {
        var regex = ".{0,47}BCDFHEIJKLAMG";
        String hayStack = "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@BCDFHEIJKLAMG";

        Node node = RegexParser.parse(regex, 0);

        NFA forwardNFA = new NFA(RegexInstrBuilder.createNFA(node));
        DFA dfaSearch = NFAToDFACompiler.compile(forwardNFA, ConversionMode.DFA_SEARCH);
        FindMethodSpec spec = new FindMethodSpec(dfaSearch,FindMethodSpec.FORWARDS, true, Factorization.empty(), CharacterDistribution.DEFAULT);
        DFAStateTransitions stateTransitions = new DFAStateTransitions();
        stateTransitions.byteClasses = dfaSearch.byteClasses();

        for (var state : dfaSearch.allStates()) {
            stateTransitions.addStateTransitionString(spec, state);
        }
        // This represents that the DFA will use multiple strings to encode state transitions for the forward spec
        // This calculation and assert are pretty tied to the details of the string encoding, but copy some of the
        // logic, which is a bit annoying.
        // TODO: try to simplify this and make it less brittle
        Set<String> byteTransitionStrings = stateTransitions.byteClassStringMaps.get(spec.statesConstant());
        int totalLength = byteTransitionStrings.stream().mapToInt(String::length).sum() + byteTransitionStrings.size();
        assertTrue(totalLength > 65535);

        var pattern = anonymousPattern(regex);
        compareResultsToStandardLibrary(hayStack, pattern, java.util.regex.Pattern.compile(regex));
    }

    /**
     * Compare a regex against the JDK standard library implementation, by ensuring that the compiled DFA finds the same
     * set of (non-overlapping) matches as the stdlib. The match is performed against the contents of a file.
     *
     * @param regex the regex
     * @param path  the path of the file
     * @throws IOException    if the file cannot be read
     * @throws AssertionError if the comparison fails
     */
    static void checkMatchesInFileAgainstStandardLibrary(String regex, Path path) throws IOException {
        String hayStack = Files.readAllLines(path).get(0);
        Pattern pattern = DFACompiler.compile(regex, "fileSearchRegex" + CLASS_NAME_COUNTER.incrementAndGet());
        java.util.regex.Pattern jdkPattern = java.util.regex.Pattern.compile(regex);
        compareResultsToStandardLibrary(hayStack, pattern, jdkPattern);
    }

    static void compareResultsToStandardLibrary(String hayStack, Pattern pattern, java.util.regex.Pattern jdkPattern) {
        Matcher matcher = pattern.matcher(hayStack);
        java.util.regex.Matcher jdkMatcher = jdkPattern.matcher(hayStack);
        int index = 0;
        while (true) {
            var matchResult = matcher.find();
            var jdkMatched = jdkMatcher.find();
            var jdkMatchResult = jdkMatcher.toMatchResult();
            if (matchResult.matched && jdkMatched) {
                var comparison = "patternStart=" + matchResult.start + ", patternEnd=" + matchResult.end + ", jdkStart=" + jdkMatchResult.start() + ", jdkEnd=" + jdkMatchResult.end();
                assertEquals(matchResult.start, jdkMatchResult.start(), "Match start was not equal at index=" + index + ", " + comparison);
                assertEquals(matchResult.end, jdkMatchResult.end(), "Match end was not equal at index=" + index + ", " + comparison);
            } else if (matchResult.matched) {
                fail("Pattern matched and standard library didn't at index=" + index + ", patternStart=" + matchResult.start + ", patternEnd=" + matchResult.end);
            } else if (jdkMatched) {
                fail("Standard library matched and pattern didn't at index=" + index + ", stdLibStart=" + jdkMatchResult.start() + ", stdLibEnd=" + jdkMatchResult.end());
            } else {
                return;
            }
            index = matchResult.end + 1;
        }
    }

    @Test
    void fileBasedTests() throws Exception {
        var baseName = "dfaFileBasedTests";
        var counter = new AtomicInteger();
        var patterns = new HashMap<Pair<String, Integer>, Pattern>();
        var testSpecs = new RegexTestSpecParser().readTests();
        var correctMatches = 0;
        var nonMatches = 0;
        var errors = new ArrayList<String>();
        for (var spec : testSpecs) {
            int flags = spec.flags != null ? spec.flags.flags : 0;
            var pair = Pair.of(spec.pattern, flags);
            var pattern = patterns.computeIfAbsent(pair, (p) -> DFACompiler.compile(spec.pattern, baseName + counter.incrementAndGet(), flags));
            if (spec.successful) {
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
                try {
                    find(pattern, spec.target);
                }
                catch (Exception e) {
                    errors.add("Matching spec=" + spec.pattern + " against needle=" + spec.target + " had error in find " + e);
                }
                catch (AssertionError e) {
                    errors.add("Matching spec=" + spec.pattern + " against needle=" + spec.target + " had AssertionError in find " + e);
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

    @Test
    void findDoesntBacktrackPastSpecifiedWindow() {
        String regex = "a*baa";
        Pattern pattern = DFACompiler.compile(regex, "noOverbacktracking");
        String hayStack = "aaaabaa";
        var result = pattern.matcher(hayStack).find(3, 7);

        assertEquals(3, result.start);
        assertEquals(7, result.end);
    }

    @Test
    void findDoesntBacktrackPastSpecifiedWindow2() {
        // Not sure if there are interesting ways to fail this but not the previous test
        String regex = "(a*tgc*|t*acg*)*(cg)(a|t)*";
        Pattern pattern = anonymousPattern(regex);
        String hayStack = "cgatgccgaa";

        var matcher = pattern.matcher(hayStack);
        var result = matcher.find(6, 10);
        assertEquals(6, result.start);
        assertEquals(10, result.end);
    }

    @Test
    void findDoesntRollOver() {
        var pattern = DFACompiler.compile("a", "rolloverTestClass");
        var matcher = pattern.matcher("aa");
        assertTrue(matcher.find().matched);
        assertTrue(matcher.find().matched);
        // Question: is it arbitrary I did three failing matches?
        assertFalse(matcher.find().matched);
        assertFalse(matcher.find().matched);
        assertFalse(matcher.find().matched);
    }

    @Test
    void dotAll() {
        var pattern = DFACompiler.compile("a.*c", "aDotStarc");
        assertTrue(pattern.matcher("abc").matches());
        assertEquals(MatchResult.success(0, 3), pattern.matcher("abc\nc").find());

        pattern = DFACompiler.compile("a.*c", "aDotStarc_DOTALL", CompilerOptions.fromFlags(Pattern.DOTALL));
        assertTrue(pattern.matcher("abc").matches());
        assertEquals(MatchResult.success(0, 5), pattern.matcher("abc\nc").find());
    }
}
