package com.justinblank.strings;

import org.junit.Ignore;
import org.junit.Test;

// For generating compiled classes for manual inspection of their contents
public class GenerateExamples {

    @Ignore
    @Test
    public void generateExamples() {
        var debugOptions = DebugOptions.writeToFsOnly();
        // Should compile to indexOf
        DFACompiler.compile("Sherlock", "Sherlock", debugOptions);
        // Should use indexOf to find the 'S' prefix
        DFACompiler.compile("Sherlock|Street", "SherlockStreet", debugOptions);
        // Should do a search for S|s
        DFACompiler.compile("[Ss]herlock", "SherlockInitialCharCaseInsensitive", debugOptions);
        // Basic regexes, no prefix, should dispatch entirely with byteClasses
        DFACompiler.compile("Sherlock|Holmes|Watson|Irene|Adler|John|Baker", "ManyNames", debugOptions);
        DFACompiler.compile("([Ss]herlock)|([Hh]olmes)", "TwoNamesCaseInsensitiveFirstChar", debugOptions);
        // Dot character including unicode
        DFACompiler.compile("a.c", "aDotc", debugOptions);
        // Large dfa with doc character including unicode
        DFACompiler.compile("Holmes.{0,25}Watson|Watson.{0,25}Holmes", "HolmesWithin25CharactersOfWatson", debugOptions);

        // Unicode needles
        DFACompiler.compile("ε", "SingleUnicodeCharNeedle", debugOptions);
        DFACompiler.compile("ε|λ", "UnicodeUnion", debugOptions);

        DFACompiler.compile("[0-9]+", "DigitPlus", debugOptions);
        // No prefix or suffix, just transitions using byteClasses
        DFACompiler.compile("(ab|a|bcdef|g)+", "Option", debugOptions);
    }
}
