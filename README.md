## Java 代码热部署总结

#### 本工程提供的方案

核心代码：common-hotswap-core

有以下两种方案：

* 动态加载`Class`和`Jar`

  - 基于`Instrumentation`的函数体级别更热

    此更新方案只允许更新方法体内的代码。比如一个类某个方法有bug，在方法体内修改完之后可以热更，但是如果新的类中添加了新的字段，新的方法或者父类发生变化了，是不行的。不过大部我们的bug都是方法内的逻辑bug，使用此方案还是可以的。 

  - 基于`Instrumentation`的`Jar`动态加载

    此更新方案允许新增Class。比如某个方法需要增加一个排序比较器`Comparator`，或者做代理转发（把原方法的调用转发到新的Class方法调用上），等等。。

  agent包：

  源码：ariescat-hotswap-agent

  `Instrumentation`实例的获取需要从`agent`代理程序上获取，上面的`agent`包同时支持`premain`方式和`agentmain`方式。

  `premain`方式启动：需要加上启动参数`-javaagent:libs\\hotswap-agent-1.1.jar`

  `agentmain`方式启动：采用`pid`绑定进行`attach`获取`VirtualMachine`，由`VirtualMachine`来`loadAgent`。

  核心实现：`com.ariescat.hotswap.instrument.ClassesHotLoadWatch`

  测试代码：`com.ariescat.hotswap.example.instrument.TestClassesHotLoadWatch`



* **动态编译脚本**，可随意更改类结构进行热更改

  采用`JDK`动态编译，并整合进`Spring`架构，用户无需关注具体实现方式

  用法：

  把自己的热点代码写进一个单独的非`source folders`的文件夹（本工程是`script`），然后用`maven`的`maven-resourczes-plugin`把该文件夹当成`resources`编译进运行目录。

  测试代码：`com.ariescat.hotswap.example.javacode.TestSpringInject`

  **注意事项**：

  经测试发现如果在脚本里开启**循环**线程，热更的话之前的线程会得不到释放，可能会导致内存溢出。因此尽量不要在脚本里开启**循环**线程：

  ```java
  public class Person implements IHello {
      public Person() {
  //        TODO 如果在脚本里开启循环线程，热更的话会得不到释放，可能会导致内存溢出。
  //        new Thread(()->{
  //            while (true) {
  //                System.err.println("Person xxx1");
  //            }
  //        }).start();
      }
  }
  ```

  

* 以上两种方案应该可以满足大部分生产环境上的bug修复。

  

#### 本人探索到的一些热更方式

* `JDK`提供

  * 通过 `premain` 或`agentmain`（推荐后者）获取到 `Instrumentation` 这个类的实例， 调用`retransformClass/redefineClass`进行函数体级别的字节码更新 

    基于`Attach`机制实现的热更新，更新类需要与原来的类在包名，类名，修饰符上完全一致，否则在`classRedefine`过程中会产生`classname don't match` 的异常。

    例如显示这样的报错：`redefineClasses exception class redefinition failed: attempted to delete a method.`

    具体来说，`JVM`热更新局限总结：

    1. 函数参数格式不能修改，只能修改函数内部的逻辑
    2. 不能增加类的函数或变量
    3. 函数必须能够退出，如果有函数在死循环中，无法执行更新类（笔者实验发现，死循环跳出之后，再执行类的时候，才会是更新类）

  * 通过`Instrumentation#appendToSystemClassLoaderSearch`来增加一个`classpath`，可以动态加载Jar包。

    只要能动态加载Jar包，就能做很多事情了

    See：[AgentAddAnonymousInnerClass](https://github.com/Ariescat/study-metis/blob/82838045ceda1d70df594f0628c1a110ac7ae2a8/agent/src/main/java/com/agent/AgentAddAnonymousInnerClass.java)

  * 定义不同的`classloader`

    该方式必须要使用新的`ClassLoader`实例来创建类的对象，运行新对象的方法。

    Tomcat的动态部署就是监听`war`变化，然后调用`StandardContext.reload()`，用新的`WebContextClassLoader`实例来加载`war`，然后初始化`servlet`来实现。类似的实现还有`OSGi`等。

  * `JavaCompiler` 动态编译（ `JDK` 1.6 开始引入 ）

* 脚本

  * `java`结合`groovy`，把热点代码写进脚本里

* 第三方

  * `Github` [HotswapAgent](https://github.com/HotswapProjects/HotswapAgent)

    该工程基于`dcevm`，需要给`jvm`打上补丁（也就是要修改原生的`jvm`），该做法存在风险（自己团队没有在生产环境上跑过这种补丁，是否会存在未知风险？），同时没有对最新的`JDK`进行支持（目前最新补丁支持到 `Java 8u181 build 2`）。

  * 阿里`arthas`

    使用`Arthas`三个命令就可以搞定热更新 ：

    ```shell
    jad --source-only com.example.demo.arthas.user.UserController > /tmp/UserController.java
    
    mc /tmp/UserController.java -d /tmp
    
    redefine /tmp/com/example/demo/arthas/user/UserController.class
    ```

    * 这个工具还可以协助完成下面这些事情(转自官网)：
      1. 这个类是从哪个`jar`包加载而来的？
      2. 为什么会报各种类相关的`Exception`？
      3. 线上遇到问题无法`debug`好蛋疼，难道只能反复通过增加`System.out`或通过加日志再重新发布吗？
      4. 线上的代码为什么没有执行到这里？是由于代码没有`commit`？还是搞错了分支？
      5. 线上遇到某个用户的数据处理有问题，但线上同样无法 `debug`，线下无法重现。
      6. 是否有一个全局视角来查看系统的运行状况？
      7. 有什么办法可以监控到`JVM`的实时运行状态？



#### 参考

* [youxijishu/game-hot-update](https://github.com/youxijishu/game-hot-update)
* [HotswapProjects/HotswapAgent](https://github.com/HotswapProjects/HotswapAgent)
* [HotswapProjects/HotswapAgentExamples](https://github.com/HotswapProjects/HotswapAgentExamples)
