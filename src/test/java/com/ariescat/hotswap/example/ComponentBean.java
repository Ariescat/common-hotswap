package com.ariescat.hotswap.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Ariescat
 * @version 2020/1/11 18:41
 */
@Component
public class ComponentBean {

    @Autowired
    private IHello hello;

    @Autowired
    private IEat eat;

    public void test() {
        System.err.println("ComponentBean Autowired test ---->");
        hello.sayHello();
        eat.eat();
        System.err.println("ComponentBean Autowired test ---->");
    }
}
