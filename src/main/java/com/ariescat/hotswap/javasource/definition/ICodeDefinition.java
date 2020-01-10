package com.ariescat.hotswap.javasource.definition;

import javax.tools.JavaFileObject;

/**
 * @author Ariescat
 * @version 2020/1/10 18:09
 */
public interface ICodeDefinition {

    String getClassName();

    JavaFileObject createJavaFileObject();

    default String getPackageName() {
        String className = getClassName();
        int i = className.lastIndexOf('.');
        return i < 0 ? "" : className.substring(0, i);
    }
}
