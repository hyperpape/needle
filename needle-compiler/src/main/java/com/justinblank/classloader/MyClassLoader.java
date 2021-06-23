package com.justinblank.classloader;

public class MyClassLoader extends ClassLoader {

    private static final MyClassLoader INSTANCE = new MyClassLoader();

    public static MyClassLoader getInstance() {
        return INSTANCE;
    }

    public Class<?> loadClass(String name, byte[] b)
            throws ClassFormatError {
        return defineClass(name, b, 0, b.length, null);
    }
}
