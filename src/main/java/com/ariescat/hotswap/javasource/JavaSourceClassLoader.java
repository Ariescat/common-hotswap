package com.ariescat.hotswap.javasource;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author Ariescat
 * @version 2020/1/10 14:42
 */
public class JavaSourceClassLoader extends URLClassLoader {

    public JavaSourceClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
    }

    public Class<?> parseClass(String text, String fileName) {
        return null;
    }
}
