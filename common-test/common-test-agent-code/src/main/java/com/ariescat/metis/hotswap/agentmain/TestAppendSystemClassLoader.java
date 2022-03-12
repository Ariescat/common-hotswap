package com.ariescat.metis.hotswap.agentmain;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;

/**
 * 将某个jar加入到Classpath里供AppClassloard去加载。
 *
 * @author Ariescat
 * @version 2020/1/9 17:14
 */
public class TestAppendSystemClassLoader {

    /**
     * 测试程序启动后加载外部jar包，如 抢红包 这个模块
     */
    public static void main(String[] args) throws Exception {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(500);

                    System.err.println("...");
                    Class<?> clazz = Class.forName("redPacket.TestRedPacket");
                    System.err.println(clazz);

                    Method redpacket = clazz.getDeclaredMethod("redpacket");
                    redpacket.setAccessible(true);
                    redpacket.invoke(null);

//                    Constructor<?> constructor = clazz.getConstructor();
//                    Object o = constructor.newInstance();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        ).start();

        Thread.sleep(1000);

        String name = ManagementFactory.getRuntimeMXBean().getName();
        //这里为了方便测试，打印出来进程id
        String pid = name.split("@")[0];
        System.err.println(pid);

        LoadAgent.loadAgent(pid);
    }
}
