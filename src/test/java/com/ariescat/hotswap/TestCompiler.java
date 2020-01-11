package com.ariescat.hotswap;

import com.ariescat.hotswap.javasource.ScriptClassLoader;

import java.io.File;

/**
 * @author Ariescat
 * @version 2020/1/10 17:20
 */
public class TestCompiler {

    public static void main(String[] args) {
        new Thread(() -> {
            while (true) {
                try {
                    File file = new File(System.getProperty("user.dir") + "\\src\\main\\script\\com\\ariescat\\hotswap\\Person.java");

                    ScriptClassLoader classLoader = new ScriptClassLoader(Thread.currentThread().getContextClassLoader());
                    Class<?> clazz = classLoader.parseClass(file);

                    IHello hello = (IHello) clazz.newInstance();
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
