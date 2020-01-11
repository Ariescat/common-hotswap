package com.ariescat.hotswap.javasource;

import javax.tools.JavaFileObject;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Ariescat
 * @version 2020/1/10 23:15
 */
public class LoadedCache {

    /**
     * this cache contains the loaded classes or PARSING, if the class is currently parsed
     */
    private final static Map<String, JavaFileObject> classes = new ConcurrentHashMap<>();

    /**
     * Adds the cache.
     *
     * @param qualifiedClassName the qualified class name
     * @param javaFile           the java file
     */
    public static void add(final String qualifiedClassName, final JavaFileObject javaFile) {
        classes.put(qualifiedClassName, javaFile);
    }

    /**
     * Files.
     *
     * @return the collection
     */
    public static Collection<JavaFileObject> files() {
        return Collections.unmodifiableCollection(classes.values());
    }

    static Map<String, JavaFileObject> classes() {
        return classes;
    }

    /**
     * Load class bytes.
     *
     * @param qualifiedClassName the qualified class name
     * @return the byte[]
     */
    public static byte[] loadClassBytes(final String qualifiedClassName) {
        JavaFileObject file = classes.get(qualifiedClassName);
        if (file != null) {
            return ((JavaFileObjectImpl) file).getByteCode();
        }
        return null;
    }

}
