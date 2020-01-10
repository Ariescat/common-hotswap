package com.ariescat.hotswap.javasource;

import com.ariescat.hotswap.javasource.definition.JavaCodeStringDefinition;

import javax.tools.JavaFileObject;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自定义类加载器, 用来加载动态的字节码
 *
 * @author Ariescat
 * @version 2020/1/10 14:42
 */
public class ScriptClassLoader extends ClassLoader {

    /**
     * this cache contains the loaded classes or PARSING, if the class is currently parsed
     */
    private final static Map<String, JavaFileObject> classes = new ConcurrentHashMap<>();

    /**
     * Instantiates a new class loader impl.
     *
     * @param parentClassLoader the parent class loader
     */
    public ScriptClassLoader(final ClassLoader parentClassLoader) {
        super(parentClassLoader);
    }

    public Class<?> parseClass(String scriptAsString, String suggestedClassName) throws Exception {
        JavaCodeStringDefinition definition = new JavaCodeStringDefinition(suggestedClassName, scriptAsString);
        return new CompilationUnit(this).doCompile(definition);
    }

    /**
     * Adds the cache.
     *
     * @param qualifiedClassName the qualified class name
     * @param javaFile           the java file
     */
    void add(final String qualifiedClassName, final JavaFileObject javaFile) {
        classes.put(qualifiedClassName, javaFile);
    }

    /**
     * Files.
     *
     * @return the collection
     */
    Collection<JavaFileObject> files() {
        return Collections.unmodifiableCollection(classes.values());
    }

    /**
     * Load class bytes.
     *
     * @param qualifiedClassName the qualified class name
     * @return the byte[]
     */
    public byte[] loadClassBytes(final String qualifiedClassName) {
        JavaFileObject file = classes.get(qualifiedClassName);
        if (file != null) {
            return ((JavaFileObjectImpl) file).getByteCode();
        }
        return null;
    }

    @Override
    protected Class<?> findClass(final String qualifiedClassName) throws ClassNotFoundException {
        JavaFileObject file = classes.get(qualifiedClassName);
        if (file != null) {
            byte[] bytes = ((JavaFileObjectImpl) file).getByteCode();
            return defineClass(qualifiedClassName, bytes, 0, bytes.length);
        }
        try {
            return ClassHelper.forNameWithCallerClassLoader(qualifiedClassName, getClass());
        } catch (ClassNotFoundException nf) {
            return super.findClass(qualifiedClassName);
        }
    }

    @Override
    protected synchronized Class<?> loadClass(final String name, final boolean resolve)
            throws ClassNotFoundException {
        return super.loadClass(name, resolve);
    }


    @Override
    public InputStream getResourceAsStream(final String name) {
        if (name.endsWith(JavaFileObject.Kind.CLASS.extension)) {
            String qualifiedClassName =
                    name.substring(0, name.length() - JavaFileObject.Kind.CLASS.extension.length())
                            .replace('/', '.');
            JavaFileObjectImpl file = (JavaFileObjectImpl) classes.get(qualifiedClassName);
            if (file != null) {
                return new ByteArrayInputStream(file.getByteCode());
            }
        }
        return super.getResourceAsStream(name);
    }

}
