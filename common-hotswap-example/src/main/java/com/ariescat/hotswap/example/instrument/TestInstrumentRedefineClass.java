package com.ariescat.hotswap.example.instrument;

import com.sun.tools.attach.VirtualMachine;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.lang.management.ManagementFactory;

/**
 * -javaagent:libs\\common-hotswap-agent-2.0.jar 以 premain 方式启动
 *
 * @author Ariescat
 * @version 2020/1/11 23:39
 */
public class TestInstrumentRedefineClass {

    public static void main(String[] args) {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        //这里为了方便测试，打印出来进程id
        String pid = name.split("@")[0];
        System.out.println("进程Id：" + pid);

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

        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5500);

                    System.out.println("loadAgent...");

                    // 以 agentmain 方式启动
                    // VirtualMachine是jdk中tool.jar里面的东西，所以要在pom.xml引用这个jar
                    VirtualMachine vm = VirtualMachine.attach(pid);
                    // 这个路径是相对于被热更的服务的，也就是这个pid的服务，也可以使用绝对路径。

                    // Working directory 更改为%MODULE_WORKING_DIR% 才能找到libs
                    vm.loadAgent(StringUtils.join(new String[]{"libs", "common-hotswap-agent-2.0.jar"}, File.separator));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
