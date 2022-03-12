package com.ariescat.metis.agent;

import java.lang.instrument.Instrumentation;
import java.util.jar.JarFile;

/**
 * @author Ariescat
 * @version 2020/1/9 17:03
 *
 * {@link com.ariescat.metis.hotswap.agentmain.TestAppendSystemClassLoader}
 */
public class AgentAppendSystemClassLoader {

    public static void agentmain(String args, Instrumentation inst) throws Exception {
        System.out.println("agent 启动成功....");
        inst.appendToSystemClassLoaderSearch(new JarFile("some-function/target/some-function-1.0-SNAPSHOT.jar"));
        System.out.println("appendToSystemClassLoaderSearch...");
    }
}
