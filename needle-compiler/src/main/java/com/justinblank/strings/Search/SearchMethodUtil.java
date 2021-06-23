package com.justinblank.strings.Search;

public class SearchMethodUtil {
    public static void checkIndices(String s, int start, int end) {
        int length = s.length();
        if (start > length || start < 0) {
            throw new IndexOutOfBoundsException("starting index " + start + " is out of bounds");
        }
        if (end > length || end < 0) {
            throw new IndexOutOfBoundsException("ending index " + end + " is out of bounds");
        }
        if (start > end) {
            throw new IllegalArgumentException("Illegal indices " + start + ", " + end);
        }
    }
}
