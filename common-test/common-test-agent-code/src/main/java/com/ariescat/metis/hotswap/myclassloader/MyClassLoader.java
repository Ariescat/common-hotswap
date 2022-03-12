package com.ariescat.metis.hotswap.myclassloader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class MyClassLoader extends ClassLoader {

//  @Override
//  public Class<?> loadClass(String name) throws ClassNotFoundException {
//      return findClass(name);
//  }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String classPath = MyClassLoader.class.getResource("/").getPath(); //得到classpath
        File classFile = new File(classPath, name + ".class");
        if (!classFile.exists()) {
            throw new ClassNotFoundException(classFile.getPath() + " 不存在");
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ByteBuffer bf = ByteBuffer.allocate(1024);
        FileInputStream fis = null;
        FileChannel fc = null;
        try {
            fis = new FileInputStream(classFile);
            fc = fis.getChannel();
            while (fc.read(bf) > 0) {
                bf.flip();
                bos.write(bf.array(), 0, bf.limit());
                bf.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fis.close();
                fc.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return defineClass(bos.toByteArray(), 0, bos.toByteArray().length);
    }
}
