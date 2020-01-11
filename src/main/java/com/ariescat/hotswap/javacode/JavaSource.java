package com.ariescat.hotswap.javacode;

import org.apache.commons.io.FilenameUtils;
import org.springframework.scripting.ScriptSource;
import sun.misc.IOUtils;

import javax.tools.SimpleJavaFileObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;

/**
 * �Զ���һ��JavaԴ
 *
 * @author Ariescat
 * @version 2020/1/11 16:46
 */
public class JavaSource extends SimpleJavaFileObject {

    /**
     * �ļ�Դ
     */
    private Source source;

    /**
     * Instantiates a new java file object impl.
     *
     * @param source �ļ�Դ
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
                int start = name.indexOf('\\');
                if (start != -1) {
                    int end = name.lastIndexOf('.');
                    if (end != -1) {
                        name = name.substring(start + 1, end);
                    } else {
                        name = name.substring(start + 1);
                    }
                }
                return name.replaceAll("\\\\", "\\.");
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
                String name = javaFile.getPath();
                // TODO ����Ϊ�˲���д��com��Ȼ������ͨ�ã�
                int start = name.indexOf("com");
                if (start != -1) {
                    int end = name.lastIndexOf('.');
                    if (end != -1) {
                        name = name.substring(start, end);
                    } else {
                        name = name.substring(start);
                    }
                }
                return name.replaceAll("\\\\", "\\.");
            }
        });
    }

    interface Source {
        String getScriptAsString() throws IOException;

        boolean isModified();

        String suggestedClassName();
    }
}