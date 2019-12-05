package com.justinblank.strings;

public class RegexSyntaxException extends RuntimeException {

    // TODO: remove, use good error messages everywhere
    RegexSyntaxException() {
        super();
    }

    RegexSyntaxException(String s) {
        super(s);
    }

    RegexSyntaxException(Exception e) {
        super(e);
    }
}
