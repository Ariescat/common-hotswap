package com.ariescat.hotswap.example.javacode;

import com.ariescat.hotswap.javacode.ScriptClassLoader;
import org.apache.commons.lang3.StringUtils;

import java.io.File;

/**
 * CustomStringJavaCompiler
 *
 * @author Ariescat
 * @version 2020/1/10 17:20
 */
public class TestCompiler2 {

    public static void main(String[] args) {
        new Thread(() -> {
            // Working directory 更改为%MODULE_WORKING_DIR%
            String personPath = StringUtils.join(new String[]{"src", "main", "script", "com", "ariescat", "hotswap", "example", "bean", "Person.java"}, File.separator);
            File file = new File(System.getProperty("user.dir") + File.separator + personPath);
            if (!file.exists()) {
                file = new File(System.getProperty("user.dir") + File.separator + "common-hotswap-example" + File.separator + personPath);
            }
            if (!file.exists()) {
                System.err.println("!file.exists()");
                return;
            }

            long lastModified = 0;
            IHello hello = null;

            ScriptClassLoader classLoader = new ScriptClassLoader(Thread.currentThread().getContextClassLoader());
            while (true) {
                try {

                    // 运行后，可以尝试修改[源码包下]的Person.java
                    if (lastModified != file.lastModified()) {
                        Class<?> clazz = classLoader.parseClass(file);
                        hello = (IHello) clazz.newInstance();
                        lastModified = file.lastModified();
                    }

                    hello.sayHello();
                } catch (Throwable e) {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(3500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
