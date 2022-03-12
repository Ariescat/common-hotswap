package com.ariescat.metis.hotswap.agentmain;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

import java.io.IOException;

/**
 * @author Ariescat
 * @version 2020/1/9 16:11
 */
public class LoadAgent {

    private static final String pid = "18376";
    private static final String agent_path = "/test-agent-2.0.jar";

    public static void main(String[] args) throws Exception {
        loadAgent(pid);
    }

    public static void loadAgent(String pid)
            throws AttachNotSupportedException, IOException, AgentLoadException, AgentInitializationException {
        String dir = System.getProperty("user.dir") + agent_path;

        //VirtualMachine是jdk中tool.jar里面的东西，所以要在pom.xml引用这个jar
        VirtualMachine vm = VirtualMachine.attach(pid);
        // 这个路径是相对于被热更的服务的，也就是这个pid的服务，也可以使用绝对路径。
        vm.loadAgent(dir);

        System.err.println("loadAgent suc: " + dir);
    }
}
