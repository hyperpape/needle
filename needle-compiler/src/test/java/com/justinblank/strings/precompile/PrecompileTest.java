package com.justinblank.strings.precompile;

import org.assertj.core.util.Files;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class PrecompileTest {

    @Test
    public void testPrecompilation() throws IOException {
        File dir = Files.newTemporaryFolder();
        Precompile.precompile("a", "PrecompiledRegex", dir.getAbsolutePath());
        var target = dir.getAbsolutePath() + "PrecompiledRegex.class";
        var proc = Runtime.getRuntime().exec("file " + target, new String[0]);
        var output = new String(proc.getInputStream().readAllBytes());
        assertTrue(output.contains("compiled Java class data"));
    }
}
