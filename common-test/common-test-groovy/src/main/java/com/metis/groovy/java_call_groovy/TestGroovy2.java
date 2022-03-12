package com.metis.groovy.java_call_groovy;

/**
 * See groovy-decompile-atrhas/TestGroovy2.java
 *
 * and
 *
 * See groovy-decompile-target/TestGroovy2.java
 */
public class TestGroovy2 implements TestInterface {

    @Override
    public void print(String msg) {
        System.out.println(msg);

        // 直接抛出异常
        // throw new RuntimeException("pppp");

        // 调用本类方法
        // testCall();

        // 调用其他类方法
        JavaBean.func(null);
    }

    private void testCall() {
        // new TestGroovy1().printArgs("1", "2", "3");
        throw new RuntimeException("testCall");
    }

}
