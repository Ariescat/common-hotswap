package com.ariescat.hotswap.javasource;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Ariescat
 * @version 2020/1/10 14:42
 */
public class JavaCodeClassLoader extends URLClassLoader {

    /**
     * this cache contains the loaded classes or PARSING, if the class is currently parsed
     */
    protected final Map<String, Class> classCache = new ConcurrentHashMap<>();

    public JavaCodeClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
    }

    public Class<?> parseClass(String text, String fileName) {
        return null;
    }
}
