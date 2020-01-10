package com.ariescat.hotswap.javasource;

import com.ariescat.hotswap.javasource.definition.ICodeDefinition;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

import static javax.tools.JavaFileObject.Kind;

/**
 * @author Ariescat
 * @version 2020/1/10 15:43
 */
public class CompilationUnit {

    private final static Logger log = LoggerFactory.getLogger(CompilationUnit.class);

    /**
     * The compiler.
     */
    private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

    /**
     * The class loader.
     */
    private final ClassLoaderImpl classLoader;

    /**
     * The java file manager.
     */
    private final JavaFileManagerImpl javaFileManager;

    /**
     * The options.
     */
    private volatile List<String> options;

    /**
     * The Constant DEFAULT_JDK_VERSION.
     */
    private static final String DEFAULT_JDK_VERSION = "1.8";

    public CompilationUnit(final ClassLoader loader) {
        this(loader, DEFAULT_JDK_VERSION);
    }

    /**
     * Instantiates a new jdk compiler.
     *
     * @param loader     the loader
     * @param jdkVersion the jdk version
     */
    public CompilationUnit(final ClassLoader loader, final String jdkVersion) {
        options = new ArrayList<String>();
        options.add("-source");
        options.add(jdkVersion);
        options.add("-target");
        options.add(jdkVersion);

        // set compiler's classpath to be same as the runtime's
        if (compiler == null) {
            throw new RuntimeException("compiler is null maybe you are on JRE enviroment please change to JDK environment.");
        }
        // 存放编译过程中输出的信息
        DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<JavaFileObject>();
        // 标准的内容管理器,更换成自己的实现，覆盖部分方法
        StandardJavaFileManager manager = compiler.getStandardFileManager(diagnosticCollector, null, StandardCharsets.UTF_8);
        if (loader instanceof URLClassLoader
                && (!loader.getClass().getName().equals("sun.misc.Launcher$AppClassLoader"))) {
            try {
                URLClassLoader urlClassLoader = (URLClassLoader) loader;
                List<File> files = new ArrayList<File>();
                for (URL url : urlClassLoader.getURLs()) {

                    String file = url.getFile();
                    files.add(new File(file));
                    if (StringUtils.endsWith(file, "!/")) {
                        file = StringUtils.removeEnd(file, "!/");
                    }
                    if (file.startsWith("file:")) {
                        file = StringUtils.removeStart(file, "file:");
                    }

                    files.add(new File(file));
                }
                manager.setLocation(StandardLocation.CLASS_PATH, files);
            } catch (IOException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }

        classLoader = AccessController.doPrivileged(new PrivilegedAction<ClassLoaderImpl>() {
            public ClassLoaderImpl run() {
                return new ClassLoaderImpl(loader);
            }
        });
        javaFileManager = new JavaFileManagerImpl(manager, classLoader);
    }

    public Class<?> doCompile(ICodeDefinition codeDefinition) throws Exception {
        return doCompile(codeDefinition, null);
    }

    public synchronized Class<?> doCompile(ICodeDefinition codeDefinition, OutputStream os) throws Exception {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        String className = codeDefinition.getClassName();
        if (log.isDebugEnabled()) {
            log.debug("Begin to compile source code: class is '{}'", className);
        }

        //构造源代码对象
        JavaFileObject javaFileObject = codeDefinition.createJavaFileObject();
        javaFileManager.putFileForInput(StandardLocation.SOURCE_PATH, codeDefinition.getPackageName(), className + Kind.SOURCE.extension, javaFileObject);

        DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
        Boolean result = compiler.getTask(null, javaFileManager, diagnosticCollector, options, null, Collections.singletonList(javaFileObject))
                .call();
        if (result == null || !result) {
            // 编译信息(错误 警告)
            throw new IllegalStateException("Compilation failed. class: " + className + ", diagnostics: " + diagnosticCollector.getDiagnostics());
        }

        stopWatch.stop();
        if (log.isDebugEnabled()) {
            log.debug("compile source code done: class is '{}', cost '{}' mills", className, stopWatch.getTotalTimeMillis());
        }

        Class<?> retClass = classLoader.loadClass(className);
        if (log.isDebugEnabled()) {
            log.debug("loading class done  '{}'", className);
        }

        if (os != null) {
            byte[] bytes = classLoader.loadClassBytes(className);
            if (bytes != null) {
                os.write(bytes);
                os.flush();
            }
        }
        return retClass;
    }

    /**
     * 自定义类加载器, 用来加载动态的字节码
     */
    private static final class ClassLoaderImpl extends ClassLoader {

        /**
         * The classes.
         */
        private final Map<String, JavaFileObject> classes = new HashMap<>();

        /**
         * Instantiates a new class loader impl.
         *
         * @param parentClassLoader the parent class loader
         */
        ClassLoaderImpl(final ClassLoader parentClassLoader) {
            super(parentClassLoader);
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
                byte[] bytes = ((JavaFileObjectImpl) file).getByteCode();
                return bytes;
            }
            return null;
        }

        /**
         * (non-Javadoc)
         *
         * @see java.lang.ClassLoader#findClass(java.lang.String)
         */
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

        /**
         * Adds the.
         *
         * @param qualifiedClassName the qualified class name
         * @param javaFile           the java file
         */
        void add(final String qualifiedClassName, final JavaFileObject javaFile) {
            classes.put(qualifiedClassName, javaFile);
        }

        /**
         * (non-Javadoc)
         *
         * @see java.lang.ClassLoader#loadClass(java.lang.String, boolean)
         */
        @Override
        protected synchronized Class<?> loadClass(final String name, final boolean resolve)
                throws ClassNotFoundException {
            return super.loadClass(name, resolve);
        }

        /**
         * (non-Javadoc)
         *
         * @see java.lang.ClassLoader#getResourceAsStream(java.lang.String)
         */
        @Override
        public InputStream getResourceAsStream(final String name) {
            if (name.endsWith(Kind.CLASS.extension)) {
                String qualifiedClassName =
                        name.substring(0, name.length() - Kind.CLASS.extension.length()).replace('/', '.');
                JavaFileObjectImpl file = (JavaFileObjectImpl) classes.get(qualifiedClassName);
                if (file != null) {
                    return new ByteArrayInputStream(file.getByteCode());
                }
            }
            return super.getResourceAsStream(name);
        }
    }

    /**
     * 自定义一个编译之后的字节码对象
     */
    public static final class JavaFileObjectImpl extends SimpleJavaFileObject {

        private ByteArrayOutputStream bytecode;
        private final CharSequence source;

        /**
         * Instantiates a new java file object impl.
         *
         * @param className the base name
         * @param source    the source
         */
        public JavaFileObjectImpl(final String className, final CharSequence source) {
            super(URI.create("string:///" + className.replaceAll("\\.", "/") + Kind.SOURCE.extension), Kind.SOURCE);
            this.source = source;
        }

        public JavaFileObjectImpl(String className, Kind kind) {
            super(URI.create("string:///" + className.replaceAll("\\.", "/") + kind.extension), kind);
            this.source = null;
        }

        @Override
        public CharSequence getCharContent(final boolean ignoreEncodingErrors) throws UnsupportedOperationException {
            if (source == null) {
                throw new UnsupportedOperationException("source == null");
            }
            return source;
        }

        @Override
        public InputStream openInputStream() {
            return new ByteArrayInputStream(getByteCode());
        }

        @Override
        public OutputStream openOutputStream() {
            return bytecode = new ByteArrayOutputStream();
        }

        public byte[] getByteCode() {
            return bytecode.toByteArray();
        }
    }

    /**
     * 自定义一个JavaFileManage来控制编译之后字节码的输出位置
     */
    private static final class JavaFileManagerImpl extends ForwardingJavaFileManager<JavaFileManager> {

        /**
         * The class loader.
         */
        private final ClassLoaderImpl classLoader;

        /**
         * The file objects.
         */
        private final Map<URI, JavaFileObject> fileObjects = new HashMap<URI, JavaFileObject>();

        /**
         * Instantiates a new java file manager impl.
         *
         * @param fileManager the file manager
         * @param classLoader the class loader
         */
        public JavaFileManagerImpl(JavaFileManager fileManager, ClassLoaderImpl classLoader) {
            super(fileManager);
            this.classLoader = classLoader;
        }

        /**
         * (non-Javadoc)
         *
         * @see javax.tools.ForwardingJavaFileManager#getFileForInput(javax.tools.JavaFileManager.Location,
         * java.lang.String, java.lang.String)
         */
        @Override
        public FileObject getFileForInput(Location location, String packageName, String relativeName)
                throws IOException {
            FileObject o = fileObjects.get(uri(location, packageName, relativeName));
            if (o != null) {
                return o;
            }
            return super.getFileForInput(location, packageName, relativeName);
        }

        /**
         * Put file for input.
         *
         * @param location     the location
         * @param packageName  the package name
         * @param relativeName the relative name
         * @param file         the file
         */
        public void putFileForInput(StandardLocation location, String packageName, String relativeName,
                                    JavaFileObject file) {
            fileObjects.put(uri(location, packageName, relativeName), file);
        }

        /**
         * Uri.
         *
         * @param location     the location
         * @param packageName  the package name
         * @param relativeName the relative name
         * @return the uri
         */
        private URI uri(Location location, String packageName, String relativeName) {
            return URI.create(location.getName() + '/' + packageName + '/' + relativeName);
        }

        /**
         * (non-Javadoc)
         *
         * @see javax.tools.ForwardingJavaFileManager#getJavaFileForOutput(javax.tools.JavaFileManager.Location,
         * java.lang.String, javax.tools.JavaFileObject.Kind, javax.tools.FileObject)
         */
        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String qualifiedName, Kind kind,
                                                   FileObject outputFile) throws IOException {
            JavaFileObject file = new JavaFileObjectImpl(qualifiedName, kind);
            classLoader.add(qualifiedName, file);
            return file;
        }

        /**
         * (non-Javadoc)
         *
         * @see javax.tools.ForwardingJavaFileManager#getClassLoader(javax.tools.JavaFileManager.Location)
         */
        @Override
        public ClassLoader getClassLoader(JavaFileManager.Location location) {
            return classLoader;
        }

        /**
         * (non-Javadoc)
         *
         * @see javax.tools.ForwardingJavaFileManager#inferBinaryName(javax.tools.JavaFileManager.Location,
         * javax.tools.JavaFileObject)
         */
        @Override
        public String inferBinaryName(Location loc, JavaFileObject file) {
            if (file instanceof JavaFileObjectImpl) {
                return file.getName();
            }
            return super.inferBinaryName(loc, file);
        }

        /**
         * (non-Javadoc)
         *
         * @see javax.tools.ForwardingJavaFileManager#list(javax.tools.JavaFileManager.Location, java.lang.String,
         * java.util.Set, boolean)
         */
        @Override
        public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse)
                throws IOException {
            Iterable<JavaFileObject> result = super.list(location, packageName, kinds, recurse);

            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            List<URL> urlList = new ArrayList<URL>();
            Enumeration<URL> e = contextClassLoader.getResources("com");
            while (e.hasMoreElements()) {
                urlList.add(e.nextElement());
            }

            ArrayList<JavaFileObject> files = new ArrayList<JavaFileObject>();

            if (location == StandardLocation.CLASS_PATH && kinds.contains(JavaFileObject.Kind.CLASS)) {
                for (JavaFileObject file : fileObjects.values()) {
                    if (file.getKind() == JavaFileObject.Kind.CLASS && file.getName().startsWith(packageName)) {
                        files.add(file);
                    }
                }

                files.addAll(classLoader.files());
            } else if (location == StandardLocation.SOURCE_PATH && kinds.contains(JavaFileObject.Kind.SOURCE)) {
                for (JavaFileObject file : fileObjects.values()) {
                    if (file.getKind() == JavaFileObject.Kind.SOURCE && file.getName().startsWith(packageName)) {
                        files.add(file);
                    }
                }
            }

            for (JavaFileObject file : result) {
                files.add(file);
            }

            return files;
        }
    }
}
