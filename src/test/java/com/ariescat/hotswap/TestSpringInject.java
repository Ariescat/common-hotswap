package com.ariescat.hotswap;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Ariescat
 * @version 2020/1/10 14:25
 */
public class TestSpringInject {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");
        IHello bean = context.getBean(IHello.class);
        bean.sayHello();
    }
}
