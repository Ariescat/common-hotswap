package com.ariescat.hotswap.example.javacode;

import com.ariescat.hotswap.javacode.ScriptClassLoader;

import java.io.File;

public class TestJavaCompiler {

    public static void main(String[] args) {
        try {
            ScriptClassLoader classLoader = new ScriptClassLoader(Thread.currentThread().getContextClassLoader());
            File javaSourceFile = new File(System.getProperty("user.dir") +
                    "/common-hotswap-example/src/main/java/com/ariescat/hotswap/example/instrument/RedefineBean.java");
            Class<?> clazz = classLoader.parseClass(javaSourceFile);
            System.err.println(clazz);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
