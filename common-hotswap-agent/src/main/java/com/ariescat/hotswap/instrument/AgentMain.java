package com.ariescat.hotswap.instrument;

import java.lang.instrument.Instrumentation;

/**
 * 代理入口
 *
 * @author Ariescat
 * @version 2020/1/10 9:57
 */
public class AgentMain {

    public static Instrumentation inst;

    public static void agentmain(String args, Instrumentation inst) {
        premain(args, inst);
    }

    public static void premain(String args, Instrumentation inst) {
        System.out.println("Hotswap agent start...  [args is " + args + "]");

        AgentMain.inst = inst;

        System.out.println("Hotswap agent end.");
    }
}
