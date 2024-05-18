package com.justinblank.strings.precompile;

import com.justinblank.strings.DFACompiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Precompile {

    /**
     * Compile a regex to bytecode and write it to a specified file
     * @param regex the regex
     * @param className the name of the created class
     * @param directory the directory where the file should be written
     * @throws IOException in case the file cannot be written
     */
    public static void precompile(String regex, String className, String directory) throws IOException {
        precompile(regex, className, new File(directory));
    }

    /**
     * Compile a regex to bytecode and write it to a specified file
     * @param regex the regex
     * @param className the name of the created class
     * @param directory the directory where the file should be written
     * @throws IOException in case the file cannot be written
     */
    public static String precompile(String regex, String className, File directory) throws IOException {
        var bytes = DFACompiler.compileToBytes(regex, className);
        var target = directory.getAbsolutePath() + "/" + className + ".class";
        try (var fos = new FileOutputStream(target)) {
            fos.write(bytes);
        }
        return target;
    }
}
