package com.ariescat.metis.agent.transformer;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class PerfMonXformer implements ClassFileTransformer {

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        System.out.println("--->" + className);

        if (!"com/ariescat/metis/hotswap/Person".equals(className)) {
            return classfileBuffer;
        }

        System.err.println("find...");
        className = className.replaceAll("/", ".");

        try {
            CtClass ctClass = ClassPool.getDefault().get(className);
            CtMethod test = ctClass.getDeclaredMethod("sayHello");

            test.insertBefore("{System.out.println(\"className\");}");

            return ctClass.toBytecode();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return classfileBuffer;
    }
}
