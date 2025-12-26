package com.justinblank.strings.precompile;

import org.assertj.core.util.Files;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PrecompileTest {

    @Test
    void precompilation() throws IOException {
        File dir = Files.newTemporaryFolder();
        Precompile.precompile("a", "PrecompiledRegex", dir);
        var target = dir.getAbsolutePath() + "/PrecompiledRegex.class";
        var proc = Runtime.getRuntime().exec("file " + target, new String[0]);
        var output = new String(proc.getInputStream().readAllBytes());
        assertTrue(output.contains("compiled Java class data"));
    }
}
