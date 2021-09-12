package com.ariescat.hotswap.javacode;

import org.apache.commons.io.FilenameUtils;
import org.springframework.scripting.ScriptSource;
import sun.misc.IOUtils;

import javax.tools.SimpleJavaFileObject;
import java.io.*;
import java.net.URI;

/**
 * 自定义一个Java源
 *
 * @author Ariescat
 * @version 2020/1/11 16:46
 */
public class JavaSource extends SimpleJavaFileObject {

    /**
     * 文件源
     */
    private Source source;

    /**
     * Instantiates a new java file object impl.
     *
     * @param source 文件源
     */
    private JavaSource(Source source) {
        super(URI.create(source.suggestedClassName().replaceAll("\\.", "/") + Kind.SOURCE.extension), Kind.SOURCE);
        this.source = source;
    }

    public String getName() {
        return source.suggestedClassName();
    }

    @Override
    public CharSequence getCharContent(final boolean ignoreEncodingErrors) throws UnsupportedOperationException {
        if (source == null) {
            throw new UnsupportedOperationException("source is null");
        }
        try {
            return source.getScriptAsString();
        } catch (IOException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    static JavaSource create(String scriptSourceLocator, ScriptSource scriptSource) {
        return new JavaSource(new Source() {

            @Override
            public String getScriptAsString() throws IOException {
                return scriptSource.getScriptAsString();
            }

            @Override
            public boolean isModified() {
                return scriptSource.isModified();
            }

            @Override
            public String suggestedClassName() {
                String name = scriptSourceLocator;
                int start = name.indexOf(File.separatorChar);
                if (start != -1) {
                    int end = name.lastIndexOf(Kind.SOURCE.extension);
                    if (end != -1) {
                        name = name.substring(start + 1, end);
                    } else {
                        name = name.substring(start + 1);
                    }
                }
                return name.replace(File.separatorChar, '.');
            }
        });
    }

    static JavaSource create(File javaFile) throws IllegalAccessException {
        if (!"java".equals(FilenameUtils.getExtension(javaFile.getName()))) {
            throw new IllegalAccessException("not a java file!" + javaFile.getPath());
        }

        long lastModified = javaFile.lastModified();
        return new JavaSource(new Source() {

            @Override
            public String getScriptAsString() throws IOException {
                try (FileInputStream fileInputStream = new FileInputStream(javaFile)) {
                    byte[] bytes = IOUtils.readFully(fileInputStream, -1, false);
                    return new String(bytes);
                }
            }

            @Override
            public boolean isModified() {
                return javaFile.lastModified() == lastModified;
            }

            @Override
            public String suggestedClassName() {
                String className = null;
                try (BufferedReader reader = new BufferedReader(new FileReader(javaFile), 20)) {
                    String packageName = reader.readLine();
                    while (packageName != null) {
                        if (packageName.startsWith("package")) {
                            packageName = packageName.substring(8, packageName.length() - 1); // 7 + 1个空格, 去掉最后的;号
                            String fileName = javaFile.getName();
                            className = packageName + "." + fileName.substring(0, fileName.length() - 5);
                            break;
                        }
                        packageName = reader.readLine();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return className;
//                // 这里为了测试写死com，然而并不通用！
//                String name = javaFile.getAbsolutePath().substring(System.getProperty("user.dir").length());
//                int start = name.indexOf("com/");
//                if (start != -1) {
//                    int end = name.lastIndexOf('.');
//                    if (end != -1) {
//                        name = name.substring(start, end);
//                    } else {
//                        name = name.substring(start);
//                    }
//                }
//                return name.replaceAll(File.separator, "\\.");
            }
        });
    }

    interface Source {
        String getScriptAsString() throws IOException;

        boolean isModified();

        String suggestedClassName();
    }
}
