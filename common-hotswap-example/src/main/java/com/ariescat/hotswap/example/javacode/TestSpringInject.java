package com.ariescat.hotswap.example.javacode;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Ariescat
 * @version 2020/1/10 14:25
 */
public class TestSpringInject {

    public static void main(String[] args) {
        new Thread(() -> {
            ConfigurableApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");
            while (true) {
                try {
                    // IHello 和 IEat 的具体实现类在 script/目录下, 这个目录开发的时候设置为源码包，发布的时候单独出来
                    // 可以直接修改script/目录下的类
                    IHello bean = context.getBean(IHello.class);
                    bean.sayHello();
                    IEat eat = context.getBean(IEat.class);
                    eat.eat();
                    ComponentBean componentBean = context.getBean(ComponentBean.class);
                    componentBean.test();
                } catch (Throwable e) {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(2500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
