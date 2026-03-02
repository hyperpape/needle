package com.justinblank.strings;

public class PatternSyntaxException extends PatternException {

    PatternSyntaxException(String s) {
        super(s);
    }

    PatternSyntaxException(String s, Exception e) {
        super(s, e);
    }
}
