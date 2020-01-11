package com.ariescat.hotswap.test;

import com.ariescat.hotswap.javacode.ScriptClassLoader;

import java.io.File;

/**
 * CustomStringJavaCompiler
 *
 * @author Ariescat
 * @version 2020/1/10 17:20
 */
public class TestCompiler {

    public static void main(String[] args) {
        new Thread(() -> {
            File file = new File(System.getProperty("user.dir") + "\\src\\main\\script\\com\\ariescat\\hotswap\\test\\Person.java");
            long lastModified = 0;
            IHello hello = null;

            ScriptClassLoader classLoader = new ScriptClassLoader(Thread.currentThread().getContextClassLoader());
            while (true) {
                try {

                    if (lastModified != file.lastModified()) {
                        Class<?> clazz = classLoader.parseClass(file);
                        hello = (IHello) clazz.newInstance();
                        lastModified = file.lastModified();
                    }

                    hello.sayHello();
                    System.out.println("----------------------");
                } catch (Throwable e) {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(3500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        ).start();
    }
}
