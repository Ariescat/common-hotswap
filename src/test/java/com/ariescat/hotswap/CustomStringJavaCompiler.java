package com.ariescat.hotswap;

import sun.misc.IOUtils;

import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Create by andy on 2018-12-06 21:25
 */
public class CustomStringJavaCompiler {
    //类全名
    private String fullClassName;
    private String sourceCode;
    //存放编译之后的字节码(key:类全名,value:编译之后输出的字节码)
    private Map<String, ByteJavaFileObject> javaFileObjectMap = new ConcurrentHashMap<>();
    //获取java的编译器
    private JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    //存放编译过程中输出的信息
    private DiagnosticCollector<JavaFileObject> diagnosticsCollector = new DiagnosticCollector<>();
    //执行结果（控制台输出的内容）
    private String runResult;
    //编译耗时(单位ms)
    private long compilerTakeTime;
    //运行耗时(单位ms)
    private long runTakeTime;


    public CustomStringJavaCompiler(String sourceCode) {
        this.sourceCode = sourceCode;
        this.fullClassName = getFullClassName(sourceCode);
    }

    public static void main(String[] args) {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(500);

                    test();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        ).start();

    }

    private static void test() {
        String code = "public class HelloWorld {\n" +
                "    public static void main(String []args) {\n" +
                "\t\tfor(int i=0; i < 1; i++){\n" +
                "\t\t\t       System.out.println(\"Hello World!\");\n" +
                "\t\t}\n" +
                "    }\n" +
                "}";
        CustomStringJavaCompiler compiler = new CustomStringJavaCompiler(code);
        boolean res = compiler.compiler();
        if (res) {
            System.out.println("编译成功");
            System.out.println("compilerTakeTime：" + compiler.getCompilerTakeTime());
            try {
                compiler.runMainMethod();
                System.out.println("runTakeTime：" + compiler.getRunTakeTime());
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println(compiler.getRunResult());
            System.out.println("诊断信息：" + compiler.getCompilerMessage());
        } else {
            System.out.println("编译失败");
            System.out.println(compiler.getCompilerMessage());
        }
    }

    /**
     * 编译字符串源代码,编译失败在 diagnosticsCollector 中获取提示信息
     *
     * @return true:编译成功 false:编译失败
     */
    public boolean compiler() {
        long startTime = System.currentTimeMillis();
        //标准的内容管理器,更换成自己的实现，覆盖部分方法
        StandardJavaFileManager standardFileManager = compiler.getStandardFileManager(diagnosticsCollector, null, null);
        JavaFileManager javaFileManager = new StringJavaFileManage(standardFileManager);
        //构造源代码对象
//        JavaFileObject javaFileObject = new StringJavaFileObject(fullClassName, sourceCode);
//        TODO 记得要把这个路径排除出编译路径
        JavaScoreFileObject javaFileObject = new JavaScoreFileObject(fullClassName, "C:\\LQZ_Projects\\Idea_Projects\\ariescat-hotswap-core\\src\\test\\script\\com\\ariescat\\hotswap\\Person.java");

        //获取一个编译任务
        JavaCompiler.CompilationTask task = compiler.getTask(null, javaFileManager, diagnosticsCollector, null, null, Arrays.asList(javaFileObject));
        //设置编译耗时
        compilerTakeTime = System.currentTimeMillis() - startTime;
        return task.call();
    }

    /**
     * 执行main方法，重定向System.out.print
     */
    public void runMainMethod() throws Exception {
        PrintStream out = System.out;
        try {
            long startTime = System.currentTimeMillis();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintStream printStream = new PrintStream(outputStream);
            //PrintStream PrintStream = new PrintStream("/Users/andy/Desktop/tem.sql"); //输出到文件
            System.setOut(printStream);

            StringClassLoader scl = new StringClassLoader();
            Class<?> aClass = scl.findClass(fullClassName);
//            Method main = aClass.getMethod("sayHello", String[].class);
//            Object[] pars = new Object[]{1};
//            pars[0] = new String[]{};
//            main.invoke(null, pars); //调用main方法

            IHello hello = (IHello) aClass.newInstance();
            hello.sayHello();
            //设置运行耗时
            runTakeTime = System.currentTimeMillis() - startTime;
            //设置打印输出的内容
            runResult = new String(outputStream.toByteArray(), "utf-8");
        } finally {
            //还原默认打印的对象
            System.setOut(out);
        }

    }

    /**
     * @return 编译信息(错误 警告)
     */
    public String getCompilerMessage() {
        StringBuilder sb = new StringBuilder();
        List<Diagnostic<? extends JavaFileObject>> diagnostics = diagnosticsCollector.getDiagnostics();
        for (Diagnostic diagnostic : diagnostics) {
            sb.append(diagnostic.toString()).append("\r\n");
        }
        return sb.toString();
    }

    /**
     * @return 控制台打印的信息
     */
    public String getRunResult() {
        return runResult;
    }


    public long getCompilerTakeTime() {
        return compilerTakeTime;
    }

    public long getRunTakeTime() {
        return runTakeTime;
    }

    /**
     * 获取类的全名称
     *
     * @param sourceCode 源码
     * @return 类的全名称
     */
    public static String getFullClassName(String sourceCode) {
//        String className = "";
//        Pattern pattern = Pattern.compile("package\\s+\\S+\\s*;");
//        Matcher matcher = pattern.matcher(sourceCode);
//        if (matcher.find()) {
//            className = matcher.group().replaceFirst("package", "").replace(";", "").trim() + ".";
//        }
//
//        pattern = Pattern.compile("class\\s+\\S+\\s+\\{");
//        matcher = pattern.matcher(sourceCode);
//        if (matcher.find()) {
//            className += matcher.group().replaceFirst("class", "").replace("{", "").trim();
//        }
//        return className;
        return "com.ariescat.hotswap.Person";
    }

    /**
     * 自定义一个字符串的源码对象
     */
    private class StringJavaFileObject extends SimpleJavaFileObject {
        //等待编译的源码字段
        private String contents;

        //java源代码 => StringJavaFileObject对象 的时候使用
        public StringJavaFileObject(String className, String contents) {
            super(URI.create("string:///" + className.replaceAll("\\.", "/") + Kind.SOURCE.extension), Kind.SOURCE);
            this.contents = contents;
        }

        //字符串源码会调用该方法
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return contents;
        }

    }

    private class JavaScoreFileObject extends SimpleJavaFileObject {

        private String path;

        public JavaScoreFileObject(String className, String path) {
            super(URI.create("string:///" + className.replaceAll("\\.", "/") + Kind.SOURCE.extension), Kind.SOURCE);
            this.path = path;
        }

        //字符串源码会调用该方法
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            FileInputStream fileInputStream = new FileInputStream(path);
            byte[] bytes = IOUtils.readFully(fileInputStream, -1, false);
            fileInputStream.close();
            return new String(bytes);
        }
    }

    /**
     * 自定义一个编译之后的字节码对象
     */
    private class ByteJavaFileObject extends SimpleJavaFileObject {
        //存放编译后的字节码
        private ByteArrayOutputStream outPutStream;

        public ByteJavaFileObject(String className, Kind kind) {
            super(URI.create("string:///" + className.replaceAll("\\.", "/") + Kind.SOURCE.extension), kind);
        }

        //StringJavaFileManage 编译之后的字节码输出会调用该方法（把字节码输出到outputStream）
        @Override
        public OutputStream openOutputStream() {
            outPutStream = new ByteArrayOutputStream();
            return outPutStream;
        }

        //在类加载器加载的时候需要用到
        public byte[] getCompiledBytes() {
            return outPutStream.toByteArray();
        }
    }

    /**
     * 自定义一个JavaFileManage来控制编译之后字节码的输出位置
     */
    private class StringJavaFileManage extends ForwardingJavaFileManager {
        StringJavaFileManage(JavaFileManager fileManager) {
            super(fileManager);
        }

        //获取输出的文件对象，它表示给定位置处指定类型的指定类。
        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
            ByteJavaFileObject javaFileObject = new ByteJavaFileObject(className, kind);
            javaFileObjectMap.put(className, javaFileObject);
            return javaFileObject;
        }
    }

    /**
     * 自定义类加载器, 用来加载动态的字节码
     */
    private class StringClassLoader extends ClassLoader {
        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            ByteJavaFileObject fileObject = javaFileObjectMap.get(name);
            if (fileObject != null) {
                byte[] bytes = fileObject.getCompiledBytes();
                return defineClass(name, bytes, 0, bytes.length);
            }
            try {
                return ClassLoader.getSystemClassLoader().loadClass(name);
            } catch (Exception e) {
                return super.findClass(name);
            }
        }
    }
}
