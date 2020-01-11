package com.ariescat.hotswap.javasource;

import org.apache.commons.io.FilenameUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.support.ResourceScriptSource;
import sun.misc.IOUtils;

import javax.tools.SimpleJavaFileObject;
import java.io.*;
import java.net.URI;

/**
 * 自定义一个编译之后的字节码对象
 *
 * @author Ariescat
 * @version 2020/1/10 20:34
 */
public class JavaFileObjectImpl extends SimpleJavaFileObject {

    private static String p = System.getProperty("user.dir");

    /**
     * 文件源
     */
    private Source source;
    /**
     * 编译后的字节流
     */
    private ByteArrayOutputStream compiledBytecode;
    /**
     * 编译后的全限定类名
     */
    private String compiledClassName;

    public static JavaFileObjectImpl create(ScriptSource scriptSource) {
        return new JavaFileObjectImpl(new Source() {

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
                return scriptSource.suggestedClassName();
            }

            @Override
            public String getPath() {
                try {
                    if (scriptSource instanceof ResourceScriptSource) {
                        ResourceScriptSource resourceScriptSource = (ResourceScriptSource) scriptSource;
//                        Resource resource = resourceScriptSource.getResource();
//                        if (resource instanceof ClassPathResource) {
//                            return ((ClassPathResource) resource).getPath();
//                        }
//                        return resource.getURL().getPath().replaceFirst(p, "");
                        return resourceScriptSource.getResource().getURL().getPath();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return "";
            }
        });
    }

    public static JavaFileObjectImpl create(File javaFile) throws IllegalAccessException {
        if (!"java".equals(FilenameUtils.getExtension(javaFile.getName()))) {
            throw new IllegalAccessException("not a java file!" + javaFile.getPath());
        }
        long lastModified = javaFile.lastModified();
        return new JavaFileObjectImpl(new Source() {

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
                return FilenameUtils.getBaseName(javaFile.getName());
            }

            @Override
            public String getPath() {
                return javaFile.getPath();
            }
        });
    }

    /**
     * Instantiates a new java file object impl.
     *
     * @param source 文件源
     */
    private JavaFileObjectImpl(Source source) {
        super(URI.create(source.suggestedClassName().replaceAll("\\.", "/") + Kind.SOURCE.extension), Kind.SOURCE);
        this.source = source;
    }

    public JavaFileObjectImpl(URI uri, Kind kind) {
        super(uri, kind);
        this.source = null;
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

    @Override
    public OutputStream openOutputStream() {
        return compiledBytecode = new ByteArrayOutputStream();
    }

    /**
     * 获取编译成功的字节码byte[]
     */
    public byte[] getByteCode() {
        return compiledBytecode.toByteArray();
    }

    @Override
    public String getName() {
        return source.suggestedClassName();
    }

    public String getCompiledClassName() {
        return compiledClassName;
    }

    public void setCompiledClassName(String compiledClassName) {
        this.compiledClassName = compiledClassName;
    }

    public String getSourcePath() {
        return source.getPath();
    }

    interface Source {
        String getScriptAsString() throws IOException;

        boolean isModified();

        String suggestedClassName();

        String getPath();
    }
}
