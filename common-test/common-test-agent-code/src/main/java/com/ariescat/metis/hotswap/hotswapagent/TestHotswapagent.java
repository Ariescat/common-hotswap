package com.ariescat.metis.hotswap.hotswapagent;

import com.ariescat.metis.hotswap.Person;

/**
 * 研究 GitHub上的 hotswapagent 项目
 * See https://github.com/HotswapProjects/HotswapAgent
 *
 * -XXaltjvm=dcevm -javaagent:libs\hotswap-agent-1.3.0.jar
 *
 * @author Ariescat
 * @version 2020/1/9 16:52
 */
public class TestHotswapagent {

    public static void main(String[] args) {
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
    }
}
