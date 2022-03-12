package com.ariescat.metis.hotswap.agentmain;

import com.ariescat.metis.hotswap.Person;

import java.lang.management.ManagementFactory;

/**
 * @author Ariescat
 * @version 2020/1/9 18:03
 */
public class TestAddAnonymousInnerClass {

    /**
     * 测试Person list 方法 增加一个内部类
     *
     * 思路：打一个新的jar包，把新的jar包路径 appendToSystemClassLoaderSearch，然后 redefineClasses
     */
    public static void main(String[] args) throws Exception {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(500);

                    new Person().list();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        ).start();

        Thread.sleep(20000);

        String name = ManagementFactory.getRuntimeMXBean().getName();
        //这里为了方便测试，打印出来进程id
        String pid = name.split("@")[0];
        System.err.println(pid);

        LoadAgent.loadAgent(pid);
    }
}
