package com.ariescat.hotswap.javasource.definition;

import com.ariescat.hotswap.javasource.JavaFileObjectImpl;
import org.apache.commons.io.FilenameUtils;
import sun.misc.IOUtils;

import javax.tools.JavaFileObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * @author Ariescat
 * @version 2020/1/10 18:16
 */
public class JavaCodeFileDefinition implements ICodeDefinition {

    private File javaFile;

    public JavaCodeFileDefinition(File javaFile) {
        this.javaFile = javaFile;
    }

    @Override
    public String getClassName() {
        // TODO 获取全限定类名还有bug
        return FilenameUtils.getName(javaFile.getName());
    }

    @Override
    public JavaFileObject createJavaFileObject() {
        try {
            FileInputStream fileInputStream = new FileInputStream(javaFile);
            byte[] bytes = IOUtils.readFully(fileInputStream, -1, false);
            fileInputStream.close();
            return new JavaFileObjectImpl(getClassName(), new String(bytes));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
