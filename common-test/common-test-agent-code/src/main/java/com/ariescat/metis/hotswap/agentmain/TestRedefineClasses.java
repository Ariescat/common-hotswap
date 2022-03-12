package com.ariescat.metis.hotswap.agentmain;

import com.ariescat.metis.hotswap.Person;

import java.lang.management.ManagementFactory;

/**
 * @author Ariescat
 * @version 2020/1/7 20:00
 */
public class TestRedefineClasses {

    /**
     * 测试修改类内容 进行热更 目前是方法级别的修改
     */
    public static void main(String[] args) throws Exception {
        Person p = new Person();  //内存只有一个实例对象
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(2500);
                    p.sayHello();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        ).start();

        String name = ManagementFactory.getRuntimeMXBean().getName();
        //这里为了方便测试，打印出来进程id
        String pid = name.split("@")[0];
        System.err.println(pid);
    }
}
