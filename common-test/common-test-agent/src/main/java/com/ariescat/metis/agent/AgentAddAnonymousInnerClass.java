package com.ariescat.metis.agent;

import java.lang.instrument.Instrumentation;
import java.util.jar.JarFile;

/**
 * @author Ariescat
 * @version 2020/1/9 18:18
 *
 * {@link com.ariescat.metis.hotswap.agentmain.TestAddAnonymousInnerClass}
 */
public class AgentAddAnonymousInnerClass {

    public static void agentmain(String args, Instrumentation inst) throws Exception {
        System.out.println("agent 启动成功....");
        inst.appendToSystemClassLoaderSearch(new JarFile("base-test/target/base-test-1.0-SNAPSHOT.jar"));
        System.out.println("appendToSystemClassLoaderSearch...");

        AgentRedefineClasses.redefineClasses(inst);
    }
}
