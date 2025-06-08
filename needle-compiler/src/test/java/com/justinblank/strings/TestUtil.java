package com.justinblank.strings;

import com.justinblank.strings.RegexAST.Node;

import static org.junit.jupiter.api.Assertions.fail;

public class TestUtil {

    public static Node parse(String regex) {
        try {
            Node node = RegexParser.parse(regex);
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
            return node;
        }
        catch (RegexSyntaxException e) {
            try {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
                e.printStackTrace();
                fail("Encountered exception parsing '" + regex + "', which was parsed by the Java library");
            }
            catch (Exception e2) {
                throw e;
            }
        }
        return null; // unreachable...
    }
}
