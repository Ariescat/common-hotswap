package com.ariescat.hotswap.example.instrument;

import com.ariescat.hotswap.instrument.ClassesHotLoadWatch;

/**
 * @author Ariescat
 * @version 2020/1/14 15:38
 */
public class TestClassesHotLoadWatch {

    public static void main(String[] args) throws Exception {
        // Working directory 更改为%MODULE_WORKING_DIR% 才能找到libs
        ClassesHotLoadWatch.start("libs", "target/classes");

        new Thread(() -> {
            RedefineBean bean = new RedefineBean();
            while (true) {
                try {
                    //可以修改RedefineBean, javac编译后, 替换到上面的编译目录
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
