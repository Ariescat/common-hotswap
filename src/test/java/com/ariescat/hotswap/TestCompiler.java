package com.ariescat.hotswap;

import com.ariescat.hotswap.javasource.CompilationUnit;
import sun.misc.IOUtils;

import java.io.FileInputStream;

/**
 * @author Ariescat
 * @version 2020/1/10 17:20
 */
public class TestCompiler {

    public static void main(String[] args) {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(500);

                    FileInputStream fileInputStream = new FileInputStream("C:\\LQZ_Projects\\Idea_Projects\\ariescat-hotswap-core\\src\\main\\script\\com\\ariescat\\hotswap\\Person.java");
                    byte[] bytes = IOUtils.readFully(fileInputStream, -1, false);
                    fileInputStream.close();

                    CompilationUnit unit = new CompilationUnit(TestCompiler.class.getClassLoader());
                    Class<?> clazz = unit.doCompile("com.ariescat.hotswap.Person", new String(bytes), null);

                    IHello hello = (IHello) clazz.newInstance();
                    hello.sayHello();
                    System.out.println("----------------------");

                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
        ).start();
    }
}
