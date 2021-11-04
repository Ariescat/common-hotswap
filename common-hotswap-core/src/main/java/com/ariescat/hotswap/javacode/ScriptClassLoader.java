package com.ariescat.hotswap.javacode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scripting.ScriptSource;

import javax.tools.JavaFileObject;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
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

    private final static Logger log = LoggerFactory.getLogger(ScriptClassLoader.class);

    /**
     * this cache contains the loaded classes or PARSING, if the class is currently parsed
     * <p>
     * 一个class可能有多个内部类
     */
    private final Map<String, JavaFileObject> classes = new ConcurrentHashMap<>();

    public ScriptClassLoader() {
        super();
    }

    /**
     * Instantiates a new class loader impl.
     *
     * @param parentClassLoader the parent class loader
     */
    public ScriptClassLoader(ClassLoader parentClassLoader) {
        super(parentClassLoader);
    }

    /**
     * Adds the cache.
     *
     * @param qualifiedClassName the qualified class name
     * @param javaFile           the java file
     */
    void add(String qualifiedClassName, JavaFileObject javaFile) {
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
    private byte[] loadClassBytes(String qualifiedClassName) {
        JavaFileObject file = classes.get(qualifiedClassName);
        if (file != null) {
            return ((JavaCompiledByteCode) file).getByteCode();
        }
        return null;
    }

    public Class<?> parseClass(String scriptSourceLocator, ScriptSource scriptSource) throws Exception {
        return doParseClass(JavaSource.create(scriptSourceLocator, scriptSource));
    }

    public Class<?> parseClass(File javaFile) throws Exception {
        return doParseClass(JavaSource.create(javaFile));
    }

    private Class<?> doParseClass(JavaSource javaSource) throws Exception {
        InnerLoader loader = AccessController.doPrivileged(
                (PrivilegedAction<InnerLoader>) () -> new InnerLoader(ScriptClassLoader.this)
        );
        CompilationUnit unit = new CompilationUnit(loader, new LoadClassExec(loader));
        return unit.doCompile(javaSource);
    }

    @Override
    protected Class<?> findClass(String qualifiedClassName) throws ClassNotFoundException {
        byte[] bytes = loadClassBytes(qualifiedClassName);
        if (bytes != null) {
            return defineClass(qualifiedClassName, bytes, 0, bytes.length);
        }
        try {
            return ClassHelper.forNameWithCallerClassLoader(qualifiedClassName, getClass());
        } catch (ClassNotFoundException nf) {
            return super.findClass(qualifiedClassName);
        }
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
        return super.loadClass(name, resolve);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        if (name.endsWith(JavaFileObject.Kind.CLASS.extension)) {
            String qualifiedClassName =
                    name.substring(0, name.length() - JavaFileObject.Kind.CLASS.extension.length())
                            .replace(File.separatorChar, '.');
            JavaCompiledByteCode file = (JavaCompiledByteCode) classes.get(qualifiedClassName);
            if (file != null) {
                return new ByteArrayInputStream(file.getByteCode());
            }
        }
        return super.getResourceAsStream(name);
    }

    static class InnerLoader extends ScriptClassLoader {
        InnerLoader(ScriptClassLoader delegate) {
            super(delegate);
        }
    }

    static class LoadClassExec implements CompilationUnit.ClassgenCallback {
        private final ScriptClassLoader cl;

        LoadClassExec(ScriptClassLoader cl) {
            this.cl = cl;
        }

        @Override
        public Class<?> call(String className) throws Exception {
            Class<?> clazz = cl.loadClass(className);
            if (log.isDebugEnabled()) {
                log.debug("loading class done [{}]", className);
            }
            return clazz;
        }
    }
}
