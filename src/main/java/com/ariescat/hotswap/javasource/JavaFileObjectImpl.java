package com.ariescat.hotswap.javasource;

import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

/**
 * 自定义一个编译之后的字节码对象
 *
 * @author Ariescat
 * @version 2020/1/10 20:34
 */
public class JavaFileObjectImpl extends SimpleJavaFileObject {

    private ByteArrayOutputStream bytecode;
    private final CharSequence source;

    /**
     * Instantiates a new java file object impl.
     *
     * @param className the base name
     * @param source    the source
     */
    public JavaFileObjectImpl(final String className, final CharSequence source) {
        super(URI.create(className.replaceAll("\\.", "/") + Kind.SOURCE.extension), Kind.SOURCE);
        this.source = source;
    }

    public JavaFileObjectImpl(String className, Kind kind) {
        super(URI.create(className.replaceAll("\\.", "/") + kind.extension), kind);
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
