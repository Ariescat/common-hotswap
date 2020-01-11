package com.ariescat.hotswap.javasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scripting.ScriptSource;

import javax.tools.JavaFileObject;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * 自定义类加载器, 用来加载动态的字节码
 *
 * @author Ariescat
 * @version 2020/1/10 14:42
 */
public class ScriptClassLoader extends ClassLoader {

    private final static Logger log = LoggerFactory.getLogger(ScriptClassLoader.class);

    /**
     * Instantiates a new class loader impl.
     *
     * @param parentClassLoader the parent class loader
     */
    public ScriptClassLoader(final ClassLoader parentClassLoader) {
        super(parentClassLoader);
    }


    public Class<?> parseClass(ScriptSource scriptSource) throws Exception {
        return doParseClass(JavaFileObjectImpl.create(scriptSource));
    }

    public Class<?> parseClass(File javaFile) throws Exception {
        return doParseClass(JavaFileObjectImpl.create(javaFile));
    }

    private Class<?> doParseClass(JavaFileObjectImpl javaFileObject) throws Exception {
        InnerLoader loader = AccessController.doPrivileged(
                (PrivilegedAction<InnerLoader>) () -> new InnerLoader(ScriptClassLoader.this)
        );
        CompilationUnit unit = new CompilationUnit(loader, new ClassCollector(loader));
        return unit.doCompile(javaFileObject);
    }

    @Override
    protected Class<?> findClass(final String qualifiedClassName) throws ClassNotFoundException {
        JavaFileObject file = LoadedCache.classes().get(qualifiedClassName);
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
            JavaFileObjectImpl file = (JavaFileObjectImpl) LoadedCache.classes().get(qualifiedClassName);
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

    public static class ClassCollector implements CompilationUnit.ClassgenCallback {
        private final ScriptClassLoader cl;

        ClassCollector(ScriptClassLoader cl) {
            this.cl = cl;
        }

        @Override
        public Class<?> call(String className, byte[] bytes) throws Exception {
            Class<?> clazz = cl.loadClass(className);
//            Class<?> clazz = cl.defineClass(className, bytes, 0, bytes.length);
            if (log.isDebugEnabled()) {
                log.debug("loading class done [{}]", className);
            }
            return clazz;
        }
    }
}
