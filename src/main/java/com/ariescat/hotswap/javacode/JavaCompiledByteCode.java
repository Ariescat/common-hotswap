package com.ariescat.hotswap.javacode;

import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;

/**
 * 自定义一个编译之后的字节码对象
 *
 * @author Ariescat
 * @version 2020/1/10 20:34
 */
public class JavaCompiledByteCode extends SimpleJavaFileObject {

    /**
     * 编译后的字节流
     */
    private ByteArrayOutputStream compiledBytecode;

    /**
     * Instantiates a new java file object impl.
     */
    public JavaCompiledByteCode(URI uri, Kind kind) {
        super(uri, kind);
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
}
