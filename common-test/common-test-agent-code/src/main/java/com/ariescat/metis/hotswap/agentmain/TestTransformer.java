package com.ariescat.metis.hotswap.agentmain;

import com.ariescat.metis.hotswap.Person;

import java.lang.management.ManagementFactory;

/**
 * @author Ariescat
 * @version 2020/1/9 17:24
 */
public class TestTransformer {

    public static void main(String[] args) throws Exception {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                    new Person().sayHello();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        ).start();

        Thread.sleep(500);

        String name = ManagementFactory.getRuntimeMXBean().getName();
        //这里为了方便测试，打印出来进程id
        String pid = name.split("@")[0];
        System.err.println(pid);

        LoadAgent.loadAgent(pid);
    }

    public void test() {
        System.out.println("Hello World!!");
    }
}
