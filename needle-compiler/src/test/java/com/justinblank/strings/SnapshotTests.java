package com.justinblank.strings;

import com.justinblank.strings.precompile.Precompile;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.fail;

// Generates classfiles for several regexes, and stores them in src/test/resources/snapshots.
// Subsequent runs compare newly generated classes to the existing ones so that we can detect any drift.
// Run record examples (it is ignored by default) to update the checked in classfiles.
class SnapshotTests {

    private static final List<Pair<String, String>> EXAMPLES = new ArrayList<>();

    private static final List<Pair<String, String>> LARGE_EXAMPLES = new ArrayList<>();
    static final String SNAPSHOT_PATH = "src/test/resources/snapshots/";

    static {
        // Should compile to indexOf
        EXAMPLES.add(Pair.of("Sherlock", "Sherlock"));
        // Should use indexOf to find the 'S' prefix
        EXAMPLES.add(Pair.of("Sherlock|Street", "SherlockStreet"));
        // Should do a search for S|s
        EXAMPLES.add(Pair.of("[Ss]herlock", "SherlockInitialCharCaseInsensitive"));
        // Basic regexes, no prefix, should dispatch entirely with byteClasses
        EXAMPLES.add(Pair.of("Sherlock|Holmes|Watson|Irene|Adler|John|Baker", "UnionOfManyNames"));
        // Suffix
        EXAMPLES.add(Pair.of("anywhere|somewhere", "Suffix"));
        // Predicate
        EXAMPLES.add(Pair.of("([Ss]herlock)|([Hh]olmes)", "TwoNamesCaseInsensitiveFirstChar"));
        // Dot character including unicode
        EXAMPLES.add(Pair.of("a.c", "aDotc"));
        EXAMPLES.add(Pair.of("[0-9]+", "DigitPlus"));
        // Unicode needles
        EXAMPLES.add(Pair.of("ε", "SingleCharacterUnicode"));
        EXAMPLES.add(Pair.of("ε|λ", "UnicodeUnion"));
        // No prefix or suffix, just transitions using byteClasses
        EXAMPLES.add(Pair.of("(ab|a|bcdef|g)+", "RepeatingUnionOfShortStrings"));

        // Large dfa with dot character including unicode
        LARGE_EXAMPLES.add(Pair.of("Holmes.{0,25}Watson|Watson.{0,25}Holmes", "HolmesWithin25CharactersOfWatson"));
    }

    @Test
    void generateExamples() {
        var debugOptions = DebugOptions.writeToFsOnly();
        for (var pair : EXAMPLES) {
            DFACompiler.compile(pair.getLeft(), pair.getRight(), Pattern.DOTALL, debugOptions);
        }
    }

    @Disabled
    @Test
    void recordExamples() throws IOException {
        for (var pair : EXAMPLES) {
            Precompile.precompile(pair.getLeft(), pair.getRight(), SNAPSHOT_PATH);
        }
    }

    @Disabled
    @Test
    void generateLargeExamples() {
        var debugOptions = DebugOptions.writeToFsOnly();
        for (var pair : LARGE_EXAMPLES) {
            DFACompiler.compile(pair.getLeft(), pair.getRight(), Pattern.DOTALL, debugOptions);
        }
    }

    @Test
    void compareSnapshots() throws Exception {
        for (var pair : EXAMPLES) {
            compareToSnapshot(pair.getLeft(), pair.getRight(), SNAPSHOT_PATH + pair.getRight() + ".class");
        }
    }

    void compareToSnapshot(String regex, String className, String priorVersion) throws Exception {
        File tempDir = null;
        String compiled = null;
        try {
            tempDir = org.assertj.core.util.Files.newTemporaryFolder();
            compiled = Precompile.precompile(regex, className, tempDir, 0);
            boolean equal = equalContents(priorVersion, compiled);
            if (!equal) {
                var output = computeDiff(priorVersion, compiled);
                fail(output);
            }
        }
        finally {
            if (compiled != null) {
                new File(compiled).delete();
            }
            if (tempDir != null) {
                tempDir.delete();
            }
        }
    }

    // TODO: Because of how javap presents information, this ends up being pretty noisy...it might be better to just
    // dump the javap output for the old and new files to the target dir, and print a message indicating where to find
    // it
    /**
     * Create a diff between javap -v applied to the old version of the classfile and the new version.
     * @param priorVersion file path to the prior version of the class
     * @param compiled file path to the new version of the class
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    private String computeDiff(String priorVersion, String compiled) throws IOException, InterruptedException {
        var priorOutput = Files.createTempFile("priorOutput", null);
        var currentOutput = Files.createTempFile("currentOutput", null);
        var runtime = Runtime.getRuntime();
        var process1 = new ProcessBuilder().command("javap", "-v", priorVersion).redirectOutput(priorOutput.toFile()).start();
        var process2 = new ProcessBuilder().command("javap", "-v", compiled).redirectOutput(currentOutput.toFile()).start();

        if (!process1.waitFor(10, TimeUnit.SECONDS)) {
            fail("Failed to wait for javap");
        }
        if (!process2.waitFor(10, TimeUnit.SECONDS)) {
            fail("Failed to wait for javap");
        }

        var diffProcess = runtime.exec(new String[] {"diff", priorOutput.toString(), currentOutput.toString()});
        return new String(diffProcess.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    private boolean equalContents(String priorVersion, String compiled) throws IOException {
        byte[] priorBytes = Files.readAllBytes(Path.of(priorVersion));
        byte[] currentBytes = Files.readAllBytes(Path.of(compiled));
        return Arrays.equals(priorBytes, currentBytes);
    }
}
