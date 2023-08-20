package com.justinblank.strings;

import com.justinblank.strings.Search.SearchMethod;
import org.junit.Assert;
import org.junit.Test;

import static com.justinblank.strings.RegexParserMalformedRegexTest.expectError;
import static com.justinblank.strings.RegexParserTest.check;
import static com.justinblank.strings.SearchMethodTestUtil.*;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class EscapeSequenceTests {

    @Test
    public void testEscape_a() {
        SearchMethod method = NFA.createNFA("\\a");
        match(method, "\u0007");
        for (int i = 0; i < 128; i++) {
            if (i != 7) {
                fail(method, String.valueOf((char) i));
            }
        }
        find(method, "abc\u0007def");
    }

    @Test
    public void testEscape_d() {
        SearchMethod method = NFA.createNFA("\\d");
        for (int i = 1; i < 128; i++) {
            if (i < 48 || i > 57) {
                fail(method, String.valueOf((char) i));
            }
            else {
                match(method, String.valueOf((char) i));
            }
        }
        fail(method, "abc");
        fail(method, "%");
    }

    @Test
    public void testEscape_D() {
        SearchMethod method = NFA.createNFA("\\D");
        for (int i = 1; i < 128; i++) {
            if (i < 48 || i > 57) {
                match(method, String.valueOf((char) i));
            }
            else {
                fail(method, String.valueOf((char) i));
            }
        }
        find(method, "abc");
        find(method, "%@{}*");
    }

    @Test
    public void testEscape_e() {
        SearchMethod method = NFA.createNFA("\\e");
        for (int i = 1; i < 128; i++) {
            if (i == 27) {
                match(method, String.valueOf((char) i));
            }
            else {
                fail(method, String.valueOf((char) i));
            }
        }
        find(method, "abc\u001B");
        find(method, "%@{}*\u001B");
    }

    @Test
    public void testEscape_f() {
        SearchMethod method = NFA.createNFA("\\f");
        for (int i = 1; i < 128; i++) {
            if (i == 12) {
                match(method, String.valueOf((char) i));
            }
            else {
                fail(method, String.valueOf((char) i));
            }
        }
        find(method, "abc\u000C");
        find(method, "%@{}*\u000C");
    }

    @Test
    public void testEscape_h() {
        String charsToMatch = " \u00A0\u1680\u180e\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007\u2008\u2009\200a\u202f\u205f\u3000";
        SearchMethod method = NFA.createNFA("\\h");
        for (int i = 0; i < 4096; i++) {
            if (charsToMatch.contains(String.valueOf((char) i))) {
                match(method, String.valueOf((char) i));
            }
            else {
                fail(method, String.valueOf((char) i));
            }
        }
    }

    @Test
    public void testEscape_s() {
        String charsToMatch = "  \t\n\u000B\f\n\r";
        SearchMethod method = NFA.createNFA("\\s");
        for (int i = 0; i < 4096; i++) {
            if (charsToMatch.contains(String.valueOf((char) i))) {
                match(method, String.valueOf((char) i));
            }
            else {
                fail(method, String.valueOf((char) i));
            }
        }
    }

    @Test
    public void testEscape_S() {
        String charsToMatch = "  \t\n\u000B\f\n\r";
        SearchMethod method = NFA.createNFA("\\S");
        for (int i = 1; i < 4096; i++) {
            if (charsToMatch.contains(String.valueOf((char) i))) {
                fail(method, String.valueOf((char) i));
            }
            else {
                match(method, String.valueOf((char) i));
            }
        }
    }


    @Test
    public void testEscape_t() {
        SearchMethod method = NFA.createNFA("\\t");
        for (int i = 0; i < 128; i++) {
            if (i != 9) {
                fail(method, String.valueOf((char) i));
            }
            else {
                match(method, String.valueOf((char) i));
            }
        }
    }

    @Test
    public void testEscape_w() {
        SearchMethod method = NFA.createNFA("\\w");
        assertTrue(method.matches("a"));
        assertTrue(method.matches("n"));
        assertTrue(method.matches("_"));
        assertFalse(method.matches("$"));
        assertFalse(method.matches("ab"));
    }

    @Test
    public void testEscape_W() {
        SearchMethod nfa = NFA.createNFA("\\W");
        assertFalse(nfa.matches("a"));
        assertFalse(nfa.matches("n"));
        fail(nfa, "_");
        match(nfa, "$");
        assertFalse(nfa.matches("ab"));
    }

    @Test
    public void testEscape_x() {
        SearchMethod nfa = NFA.createNFA("\\x30");
        match(nfa, "0");
        find(nfa, "abc0def");
        fail(nfa, "hi");
        expectError("\\xGH");
    }

    @Test
    public void testEscape_0() {
        check("\\061", "1");
        expectError("\\099");
    }

    @Test
    public void testEscape_0_WorksWithLongDigitStrings() {
        check("\\01234", "" + ((char) 83) + "4");
    }
}
