package com.ariescat.hotswap.javasource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static javax.tools.JavaFileObject.Kind;

/**
 * 自定义一个编译任务
 *
 * @author Ariescat
 * @version 2020/1/10 15:43
 */
public class CompilationUnit {

    private final static Logger log = LoggerFactory.getLogger(CompilationUnit.class);

    /**
     * 取得 JavaCompiler 对象
     */
    private final static JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

    /**
     * The Constant DEFAULT_JDK_VERSION
     */
    private static final String DEFAULT_JDK_VERSION = "1.8";

    /**
     * The options
     */
    private volatile List<String> options;

    /**
     * The class loader
     */
    private final ScriptClassLoader classLoader;

    /**
     * A callback for finish compile
     */
    private final ClassgenCallback classgenCallback;

    /**
     * The java file manager
     */
    private JavaFileManagerImpl javaFileManager;

    /**
     * 初始化诊断收集器
     */
    private DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();


    CompilationUnit(ScriptClassLoader loader, ClassgenCallback callback) {
        this(loader, callback, DEFAULT_JDK_VERSION);
    }

    /**
     * Instantiates a new jdk compiler.
     *
     * @param loader     the loader
     * @param jdkVersion the jdk version
     */
    CompilationUnit(ScriptClassLoader loader, ClassgenCallback classCollector, String jdkVersion) {
        options = new ArrayList<>();
        options.add("-source");
        options.add(jdkVersion);
        options.add("-target");
        options.add(jdkVersion);

        // set compiler's classpath to be same as the runtime's
        if (compiler == null) {
            throw new RuntimeException("compiler is null maybe you are on JRE enviroment please change to JDK environment.");
        }

        this.classLoader = loader;
        this.classgenCallback = classCollector;

        // 通过 JavaCompiler 取得标准 StandardJavaFileManager 对象，StandardJavaFileManager 对象主要负责编译文件对象的创建，编译的参数等等，我们只对它做些基本设置比如编译 CLASSPATH 等。
        StandardJavaFileManager manager = compiler.getStandardFileManager(diagnosticCollector, null, StandardCharsets.UTF_8);
        ClassLoader parent = loader.getParent();
        if (parent instanceof URLClassLoader
                && (!parent.getClass().getName().equals("sun.misc.Launcher$AppClassLoader"))) {
            try {
                URLClassLoader urlClassLoader = (URLClassLoader) parent;
                List<File> files = new ArrayList<>();
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

        // 更换成自己的实现，覆盖部分方法
        this.javaFileManager = new JavaFileManagerImpl(manager, classLoader);
    }

    Class<?> doCompile(JavaFileObjectImpl javaFileObject) throws Exception {
        return doCompile(javaFileObject, null);
    }

    synchronized Class<?> doCompile(JavaFileObjectImpl javaFileObject, OutputStream os) throws Exception {
        long start = System.currentTimeMillis();

        String suggestedClassName = javaFileObject.getName();
        if (log.isDebugEnabled()) {
            log.debug("Begin to compile source code [{}] --> [{}]", suggestedClassName, javaFileObject.getSourcePath());
        }

        //构造源代码对象
        Boolean result = compiler.getTask(null, javaFileManager, diagnosticCollector, options, null, Collections.singletonList(javaFileObject))
                .call();
        if (result == null || !result) {
            // 编译信息(错误 警告)
            throw new IllegalStateException("Compilation failed! [" + suggestedClassName + "] error: " + diagnosticCollector.getDiagnostics());
        }

        long end = System.currentTimeMillis();
        if (log.isDebugEnabled()) {
            log.debug("compile source code done. class [{}] cost {} mills", suggestedClassName, end - start);
        }

        String compiledClassName = javaFileObject.getCompiledClassName();
        byte[] bytes = javaFileObject.getByteCode();
        Class<?> clazz = classgenCallback.call(compiledClassName, bytes);

        if (os != null) {
            if (bytes != null) {
                os.write(bytes);
                os.flush();
            }
        }
        return clazz;
    }

    /**
     * 自定义一个JavaFileManage来控制编译之后字节码的输出位置
     */
    private static final class JavaFileManagerImpl extends ForwardingJavaFileManager<JavaFileManager> {

        /**
         * The class loader.
         */
        private final ScriptClassLoader classLoader;

        /**
         * The file objects.
         */
        private final Map<String, JavaFileObject> fileObjects = new HashMap<>();

        /**
         * Instantiates a new java file manager impl.
         *
         * @param fileManager the file manager
         * @param classLoader the class loader
         */
        JavaFileManagerImpl(JavaFileManager fileManager, ScriptClassLoader classLoader) {
            super(fileManager);
            this.classLoader = classLoader;
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject outputFile) {
            JavaFileObjectImpl impl = (JavaFileObjectImpl) outputFile;
            impl.setCompiledClassName(className);
            LoadedCache.classes().put(className, impl);
            return impl;
        }

        @Override
        public ClassLoader getClassLoader(JavaFileManager.Location location) {
            return classLoader;
        }

        @Override
        public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse)
                throws IOException {
            ArrayList<JavaFileObject> files = new ArrayList<>();

            Iterable<JavaFileObject> result = super.list(location, packageName, kinds, recurse);
            for (JavaFileObject file : result) {
                files.add(file);
            }

            if (location == StandardLocation.CLASS_PATH && kinds.contains(JavaFileObject.Kind.CLASS)) {
                for (JavaFileObject file : fileObjects.values()) {
                    if (file.getKind() == JavaFileObject.Kind.CLASS && file.getName().startsWith(packageName)) {
                        files.add(file);
                    }
                }
                files.addAll(LoadedCache.classes().values());
            } else if (location == StandardLocation.SOURCE_PATH && kinds.contains(JavaFileObject.Kind.SOURCE)) {
                for (JavaFileObject file : fileObjects.values()) {
                    if (file.getKind() == JavaFileObject.Kind.SOURCE && file.getName().startsWith(packageName)) {
                        files.add(file);
                    }
                }
            }
            return files;
        }
    }

    public interface ClassgenCallback {
        Class<?> call(String className, byte[] after) throws Exception;
    }
}
