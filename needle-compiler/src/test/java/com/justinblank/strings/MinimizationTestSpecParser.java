package com.justinblank.strings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MinimizationTestSpecParser {

    List<MinimizationTestSpec> readTests() throws Exception {
        var resource = this.getClass().getClassLoader().getResource("minimization.txt");
        var text = Files.readAllLines(Path.of(resource.toURI()));
        return text.stream()
                .filter(s -> !s.isBlank() && !s.startsWith("#"))
                .map(this::readSpec)
                .collect(Collectors.toList());
    }

    private MinimizationTestSpec readSpec(String s) {
        try {
            // Split on whitespace - trim leading/trailing and collapse multiple spaces
            String[] fields = s.trim().split("\\s+", 3);

            if (fields.length < 3) {
                throw new RuntimeException("Expected 3 fields, got " + fields.length + ": " + s);
            }

            var pattern = fields[0];
            int stateCount = Integer.parseInt(fields[1]);
            int withMinimization = Integer.parseInt(fields[2]);

            return new MinimizationTestSpec(pattern, stateCount, withMinimization);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to parse spec: " + s, e);
        }
    }
}
