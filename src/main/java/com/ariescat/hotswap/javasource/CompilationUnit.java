package com.ariescat.hotswap.javasource;

import com.ariescat.hotswap.javasource.definition.ICodeDefinition;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
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
     * The compiler.
     */
    private final static JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

    /**
     * The Constant DEFAULT_JDK_VERSION.
     */
    private static final String DEFAULT_JDK_VERSION = "1.8";

    /**
     * The class loader.
     */
    private final ScriptClassLoader classLoader;

    /**
     * The java file manager.
     */
    private final JavaFileManagerImpl javaFileManager;

    /**
     * The options.
     */
    private volatile List<String> options;


    public CompilationUnit(ScriptClassLoader loader) {
        this(loader, DEFAULT_JDK_VERSION);
    }

    /**
     * Instantiates a new jdk compiler.
     *
     * @param loader     the loader
     * @param jdkVersion the jdk version
     */
    public CompilationUnit(ScriptClassLoader loader, String jdkVersion) {
        options = new ArrayList<>();
        options.add("-source");
        options.add(jdkVersion);
        options.add("-target");
        options.add(jdkVersion);

        // set compiler's classpath to be same as the runtime's
        if (compiler == null) {
            throw new RuntimeException("compiler is null maybe you are on JRE enviroment please change to JDK environment.");
        }
        // 存放编译过程中输出的信息
        DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
        // 标准的内容管理器,更换成自己的实现，覆盖部分方法
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

        classLoader = loader;
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
            log.debug("Begin to compile source code [{}]", className);
        }

        //构造源代码对象
        JavaFileObject javaFileObject = codeDefinition.createJavaFileObject();
        javaFileManager.putFileForInput(StandardLocation.SOURCE_PATH, codeDefinition.getPackageName(), className + Kind.SOURCE.extension, javaFileObject);

        DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
        Boolean result = compiler.getTask(null, javaFileManager, diagnosticCollector, options, null, Collections.singletonList(javaFileObject))
                .call();
        if (result == null || !result) {
            // 编译信息(错误 警告)
            throw new IllegalStateException("Compilation failed! [" + className + "] error: " + diagnosticCollector.getDiagnostics());
        }

        stopWatch.stop();
        if (log.isDebugEnabled()) {
            log.debug("compile source code done. class [{}] cost {} mills", className, stopWatch.getTotalTimeMillis());
        }

        Class<?> retClass = classLoader.loadClass(className);
        if (log.isDebugEnabled()) {
            log.debug("loading class done [{}]", className);
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
        private final Map<URI, JavaFileObject> fileObjects = new HashMap<>();

        /**
         * Instantiates a new java file manager impl.
         *
         * @param fileManager the file manager
         * @param classLoader the class loader
         */
        public JavaFileManagerImpl(JavaFileManager fileManager, ScriptClassLoader classLoader) {
            super(fileManager);
            this.classLoader = classLoader;
        }

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

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String qualifiedName, Kind kind, FileObject outputFile) {
            JavaFileObject file = new JavaFileObjectImpl(qualifiedName, kind);
            classLoader.add(qualifiedName, file);
            return file;
        }

        @Override
        public ClassLoader getClassLoader(JavaFileManager.Location location) {
            return classLoader;
        }

        @Override
        public String inferBinaryName(Location loc, JavaFileObject file) {
            if (file instanceof JavaFileObjectImpl) {
                return file.getName();
            }
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
            return files;
        }
    }
}
