package com.ariescat.hotswap.instrument;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPoolException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.jar.JarFile;

/**
 * 动态加载Class
 * 动态加载Jar
 *
 * @author Ariescat
 * @version 2020/1/14 14:48
 */
public class ClassesHotLoadWatch {

    private final static Logger log = LoggerFactory.getLogger(ClassesHotLoadWatch.class);

    private static final String AGENT_JAR_NAME = "common-hotswap-agent-";
    private static final String AGENT_CLASS_NAME = "com.ariescat.hotswap.instrument.AgentMain";
    private static final String AGENT_INST_FIELD_NAME = "inst";

    private final String agentJarPath;
    private Instrumentation instrumentation;

    private ClassesHotLoadWatch(String agentJarPath) {
        this.agentJarPath = agentJarPath;
    }

    public static void start(String agentJarPath, String watchPath) throws Exception {
        start(agentJarPath, watchPath, 5000);
    }

    public static void start(String agentJarPath, String watchPath, int refreshCheckDelay) throws Exception {
        Path libPath = Paths.get(agentJarPath);
        Optional<Path> jarPath = findJar(libPath, (path, basicFileAttributes) -> path.getFileName().toString().startsWith(AGENT_JAR_NAME));

        if (!jarPath.isPresent()) {
            // 寻找备用的 agent lib包
            try {
                URL resource = ClassesHotLoadWatch.class.getResource("");

                jarPath = findJar(Paths.get(resource.toURI()), (path, basicFileAttributes) -> path.getFileName().toString().startsWith(AGENT_JAR_NAME));
                if (jarPath.isPresent()) {
                    log.debug("提供的path[{}]下无法找到[{}xxx.jar], 启用备用agent包", libPath.toAbsolutePath(), AGENT_JAR_NAME);
                } else {
                    // 本模块提供兜底的agent包，不应该走进来
                    log.error("无法启动热加载监听，以下路径搜寻不到agent包：\n[{}]\n[{}]", libPath.toAbsolutePath(), resource.getPath());
                    return;
                }
            } catch (URISyntaxException e) {
                log.error("", e);
                return;
            }
        }

        Path agentJar = jarPath.get();
        log.debug("agentJar:[{}]", agentJar);

        ClassesHotLoadWatch hotLoadWatch = new ClassesHotLoadWatch(agentJar.toAbsolutePath().toString());
        hotLoadWatch.tryPermainOnStart();
        hotLoadWatch.startWatch(watchPath, refreshCheckDelay);
    }

    private static Optional<Path> findJar(Path libPath, BiPredicate<Path, BasicFileAttributes> matcher) throws IOException {
        if (!Files.exists(libPath)) {
            return Optional.empty();
        }
        return Files.find(libPath, 1, matcher).findAny();
    }

    /**
     * 启动的时候尝试permain方式获取一次
     */
    private void tryPermainOnStart() {
        try {
            // permain方式获取
            this.instrumentation = tryGetInstrumentation();
        } catch (Exception e) {
            log.debug("try permain get instrumentation is null");
        }
    }

    /**
     * 启动热加载目录监听
     *
     * @param watchPath         监听目录
     * @param refreshCheckDelay 检测文件是否修改时间间隔
     * @throws Exception 如果目录不存在，抛出异常
     */
    private void startWatch(String watchPath, int refreshCheckDelay) throws Exception {
        File file = new File(watchPath);
        if (!file.exists()) {
            throw new RuntimeException("Not Found HotLoad Directory [" + watchPath + "]");
        }

        FileAlterationObserver observer = new FileAlterationObserver(file);
        observer.addListener(new FileAlterationListenerAdaptor() {

            private HashSet<File> changedFiles = new HashSet<>();

            @Override
            public void onStart(FileAlterationObserver observer) {
                changedFiles.clear();
            }

            @Override
            public void onStop(FileAlterationObserver observer) {
                if (!changedFiles.isEmpty()) {
                    tryHotLoad(changedFiles);
                }
                changedFiles.clear();
            }

            @Override
            public void onFileDelete(File file) {
                log.warn("onFileDelete:{}", file.getName());
            }

            @Override
            public void onFileCreate(File file) {
                log.debug("onFileDelete:{}", file.getName());
                changedFiles.add(file);
            }

            @Override
            public void onFileChange(File file) {
                log.debug("onFileChange:{}", file.getName());
                changedFiles.add(file);
            }
        });

        FileAlterationMonitor monitor = new FileAlterationMonitor(refreshCheckDelay);
        monitor.addObserver(observer);
        monitor.start();

        log.info("start watch hotload {}", file.getAbsolutePath());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                monitor.stop();
            } catch (Exception e) {
                log.error("", e);
            }
        }));
    }

    private void tryHotLoad(HashSet<File> changedFiles) {
        tryHotLoad(changedFiles, (inst, file) -> {
            log.debug("tryHotLoad [{}] start", file.getName());

            String extension = FilenameUtils.getExtension(file.getName());
            if ("class".equals(extension)) {
                redefineClasses(inst, file);

            } else if ("jar".equals(extension)) {
                String absolutePath = file.getAbsolutePath();
                inst.appendToSystemClassLoaderSearch(new JarFile(absolutePath));

                log.warn("append [{}] ToSystemClassLoaderSearch", absolutePath);
            }
        });
    }

    private void tryHotLoad(HashSet<File> changedFiles, Consumer consumer) {
        if (this.instrumentation == null) {
            VirtualMachine vm = null;
            try {
                String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
                vm = VirtualMachine.attach(pid);
                vm.loadAgent(agentJarPath, null);

                this.instrumentation = tryGetInstrumentation();
            } catch (Exception e) {
                log.error("try agentmain get instrumentation fail", e);
            } finally {
//                if (agentClazz != null && instField != null) {
//                    try {
//                        instField.set(null, null); // help GC
//                    } catch (Exception e) {
//                        log.error("", e);
//                    }
//                }
                if (vm != null) {
                    try {
                        vm.detach();
                    } catch (IOException e) {
                        log.error("", e);
                    }
                }
            }
        }
        if (this.instrumentation == null) {
            return; // should not happen
        }
        for (File changedFile : changedFiles) {
            try {
                consumer.accept(instrumentation, changedFile);
            } catch (Exception e) {
                log.error("tryHotLoad fail", e);
            }
        }
    }

    private static void redefineClasses(Instrumentation inst, File f)
            throws IOException, ConstantPoolException, ClassNotFoundException, UnmodifiableClassException {

        byte[] targetClassFile = FileUtils.readFileToByteArray(f);
        ClassFile cf = ClassFile.read(f);
        String className = cf.getName().replace('/', '.');
        Class<?> oldClass = Class.forName(className);

        ClassDefinition classDef = new ClassDefinition(oldClass, targetClassFile);
        inst.redefineClasses(classDef);

        log.warn("redefineClasses {}", className);
    }

    private Instrumentation tryGetInstrumentation()
            throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {

        Class<?> agentClazz = Class.forName(AGENT_CLASS_NAME);
        Field instField = agentClazz.getDeclaredField(AGENT_INST_FIELD_NAME);
        return (Instrumentation) instField.get(null);
    }

    interface Consumer {
        void accept(Instrumentation inst, File file) throws Exception;
    }
}
