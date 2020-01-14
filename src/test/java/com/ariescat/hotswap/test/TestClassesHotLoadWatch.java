package com.ariescat.hotswap.test;

import com.ariescat.hotswap.example.RedefineBean;
import com.ariescat.hotswap.instrument.ClassesHotLoadWatch;

/**
 * @author Ariescat
 * @version 2020/1/14 15:38
 */
public class TestClassesHotLoadWatch {

    public static void main(String[] args) throws Exception {
        ClassesHotLoadWatch.start("libs", "target/test-classes");

        new Thread(() -> {
            RedefineBean bean = new RedefineBean();
            while (true) {
                try {
                    //热更新执行之后，再次使用这个类
                    bean.print();

                    Thread.sleep(2500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
