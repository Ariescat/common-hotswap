package com.ariescat.hotswap.test;

import com.ariescat.hotswap.example.IEat;
import com.ariescat.hotswap.example.IHello;
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
                    IHello bean = context.getBean(IHello.class);
                    bean.sayHello();
                    IEat eat = context.getBean(IEat.class);
                    eat.eat();
//                    ComponentBean componentBean = context.getBean(ComponentBean.class);
//                    componentBean.test();
                    System.out.println("----------------------");
                } catch (Throwable e) {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(3500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        ).start();
    }
}
