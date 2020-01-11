package com.ariescat.hotswap.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Person implements IHello {

    private int i = 3;
    private String name;

    public Person() {
    }

    public Person(String name) {
        this.name = name;
    }

//    public void test() {
//        System.out.println("test");
//    }
//
//
//    public void test2() {
//        System.out.println("test2");
//    }

    @Override
    public void sayHello() {
        System.out.println("hello world!!~~");

        // 测试新增变量
        System.err.println(i);

        // 测试新增方法
//        test();
//        test2();

        // 测试新增内部类
//        ArrayList<Integer> list = new ArrayList<>();
//        list.add(5);
//        list.add(1);
//        list.add(4);
//        list.add(7);
//        Collections.sort(list, new Comparator<Integer>() {
//            @Override
//            public int compare(Integer o1, Integer o2) {
//                return o2 - o1;
//            }
//        });
//        System.err.println(list);
    }
}
