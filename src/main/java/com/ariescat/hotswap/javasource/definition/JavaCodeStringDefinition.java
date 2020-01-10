package com.ariescat.hotswap.javasource.definition;

import com.ariescat.hotswap.javasource.JavaFileObjectImpl;

import javax.tools.JavaFileObject;

/**
 * @author Ariescat
 * @version 2020/1/10 17:50
 */
public class JavaCodeStringDefinition implements ICodeDefinition {
    private String className;
    private String sourceCode;

    public JavaCodeStringDefinition(String className, String sourceCode) {
        this.className = className;
        this.sourceCode = sourceCode;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public JavaFileObject createJavaFileObject() {
        return new JavaFileObjectImpl(className, sourceCode);
    }
}
