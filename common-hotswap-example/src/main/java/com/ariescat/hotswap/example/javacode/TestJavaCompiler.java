package com.ariescat.hotswap.example.javacode;

import com.ariescat.hotswap.javacode.ScriptClassLoader;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class TestJavaCompiler {

    public static void main(String[] args) throws Exception {

        ScriptClassLoader classLoader = new ScriptClassLoader();
        File javaSourceFile = new File(System.getProperty("user.dir") +
                "/common-hotswap-example/src/main/java/com/ariescat/hotswap/example/instrument/RedefineBean.java");
        Class<?> clazz = classLoader.parseClass(javaSourceFile);
        System.err.println(clazz);

        Object bean = clazz.newInstance();
        Method method = clazz.getMethod("print");
        method.invoke(bean);

        // 这里要用clazz.getClassLoader()，它是InnerLoader加载的
        Class<?> innerClazz = Class.forName("com.ariescat.hotswap.example.instrument.RedefineBean$InnerClass2", true, clazz.getClassLoader());
        System.err.println(innerClazz);

        Field[] declaredFields = innerClazz.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            System.err.println(declaredField.getName());
        }
    }
}
