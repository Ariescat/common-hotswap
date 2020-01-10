package com.ariescat.hotswap;

import com.ariescat.hotswap.javasource.CompilationUnit;
import com.ariescat.hotswap.javasource.ScriptClassLoader;
import com.ariescat.hotswap.javasource.definition.JavaCodeStringDefinition;
import sun.misc.IOUtils;

import java.io.File;
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
                    String dir = System.getProperty("user.dir");
                    File file = new File(dir + "\\src\\main\\script\\com\\ariescat\\hotswap\\Person.java");
                    FileInputStream fileInputStream = new FileInputStream(file);
                    byte[] bytes = IOUtils.readFully(fileInputStream, -1, false);
                    fileInputStream.close();

                    CompilationUnit unit = new CompilationUnit(new ScriptClassLoader(TestCompiler.class.getClassLoader()));
                    Class<?> clazz = unit.doCompile(new JavaCodeStringDefinition("com.ariescat.hotswap.Person", new String(bytes)));
//                    Class<?> clazz = unit.doCompile(new JavaCodeFileDefinition(file));

                    IHello hello = (IHello) clazz.newInstance();
                    hello.sayHello();
                    System.out.println("----------------------");

                    Thread.sleep(500);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
        ).start();
    }
}
