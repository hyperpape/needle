package com.justinblank.strings;

public class PatternException extends RuntimeException {

    public PatternException(String message) {
        super(message);
    }

    public PatternException(String message, Throwable cause) {
        super(message, cause);
    }
}
