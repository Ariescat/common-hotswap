package com.ariescat.hotswap.javacode;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
     * The callback when finish compile
     */
    private final ClassgenCallback classgenCallback;

    /**
     * The java file manager
     */
    private JavaFileManager javaFileManager;

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
        this.javaFileManager = new JavaFileManagerImpl(manager, loader);
    }

    synchronized Class<?> doCompile(JavaSource javaSource) throws Exception {

        long start = System.currentTimeMillis();

        String suggestedClassName = javaSource.getName();
        if (log.isDebugEnabled()) {
            log.debug("Begin to compile source code [{}]", suggestedClassName);
        }

        //构造源代码对象
        Boolean result = compiler.getTask(null, javaFileManager, diagnosticCollector, options, null, Collections.singletonList(javaSource))
                .call();
        if (result == null || !result) {
            // 编译信息(错误 警告)
            throw new IllegalStateException("Compilation failed! " + diagnosticCollector.getDiagnostics());
        }

        long end = System.currentTimeMillis();
        if (log.isDebugEnabled()) {
            log.debug("compile source code done. class [{}] cost {} mills", suggestedClassName, end - start);
        }

        return classgenCallback.call(suggestedClassName);
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
        public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject outputFile) throws IOException {
            if (kind == JavaFileObject.Kind.CLASS) {
                JavaCompiledByteCode object = new JavaCompiledByteCode(URI.create(className + Kind.CLASS.extension), Kind.CLASS);
                // 这里是InnerLoader
                classLoader.add(className, object);
                return object;
            } else {
                return super.getJavaFileForOutput(location, className, kind, outputFile);
            }
        }

        @Override
        public ClassLoader getClassLoader(JavaFileManager.Location location) {
            return classLoader;
        }

        /**
         * 直接用 ScriptClassLoader 加载， file 类型为 JavaCompiledByteCode，
         * JavacFileManager#inferBinaryName 作了 instanceof BaseFileObject 的判断，不重写会抛异常
         * <p>
         * 但如果用 InnerLoader 加载则是这个类型：RegularFileObject，便不会报错... 好奇怪...
         */
        @Override
        public String inferBinaryName(Location loc, JavaFileObject file) {
//            if (file instanceof JavaCompiledByteCode) {
//                return file.getName();
//            }
            return super.inferBinaryName(loc, file);
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
                files.addAll(classLoader.files());
            }

            return files;
        }
    }

    public interface ClassgenCallback {
        Class<?> call(String className) throws Exception;
    }
}
