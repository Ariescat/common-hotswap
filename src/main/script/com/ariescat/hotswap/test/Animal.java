package com.ariescat.hotswap.test;

/**
 * @author Ariescat
 * @version 2020/1/11 16:35
 */
public class Animal implements IEat {

    @Override
    public void eat() {
        System.out.println("Animal eat some");
    }
}
