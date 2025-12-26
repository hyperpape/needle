package com.justinblank.strings.precompile;

import com.justinblank.strings.CompilerOptions;
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
    public static void precompile(String regex, String className, File directory) throws IOException {
        precompile(regex, className, directory, 0);
    }

    /**
     * Compile a regex to bytecode and write it to a specified file
     * @param regex the regex
     * @param className the name of the created class
     * @param directory the directory where the file should be written
     * @throws IOException in case the file cannot be written
     */
    public static String precompile(String regex, String className, File directory, int flags) throws IOException {
        var bytes = DFACompiler.compileToBytes(regex, className, flags);
        var target = directory.getAbsolutePath() + "/" + className + ".class";
        try (var fos = new FileOutputStream(target)) {
            fos.write(bytes);
        }
        return target;
    }

    /**
     * Compile a regex to bytecode and write it to a specified file
     * @param regex the regex
     * @param className the name of the created class
     * @param directory the directory where the file should be written
     * @throws IOException in case the file cannot be written
     */
    public static String precompile(String regex, String className, File directory, CompilerOptions compilerOptions) throws IOException {
        var bytes = DFACompiler.compileToBytes(regex, className, compilerOptions);
        var target = directory.getAbsolutePath() + "/" + className + ".class";
        try (var fos = new FileOutputStream(target)) {
            fos.write(bytes);
        }
        return target;
    }
}
