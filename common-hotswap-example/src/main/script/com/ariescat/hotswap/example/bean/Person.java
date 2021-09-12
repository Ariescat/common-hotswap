package com.ariescat.hotswap.example.bean;

import com.ariescat.hotswap.example.javacode.IHello;

public class Person implements IHello {

    private int i = 3;
    private String name;

    public Person() {
//        TODO 如果在脚本里开启循环线程，热更的话会得不到释放，可能会导致内存溢出。
//        new Thread(()->{
//            while (true) {
//                System.err.println("Person xxx1");
//                try {
//                    Thread.sleep(4000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
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
        // import java.util.ArrayList;
        // import java.util.Collections;
        // import java.util.Comparator;
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
