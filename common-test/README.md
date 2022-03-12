关于libs里的jar：

- arthas-boot.jar：
  Arthas（阿尔萨斯）是阿里巴巴开源的 Java 诊断工具
  可以反编译代码（查看groovy编译后的java代码），甚至热替换（暂未研究）

- hotswap-agent-1.3.0.jar：

  来源于[Release 1.3.0 · HotswapProjects/HotswapAgent · GitHub](https://github.com/HotswapProjects/HotswapAgent/releases/tag/RELEASE-1.3.0)

  该项目也能实现热替换代码，但缺点是需要打一个DCEVM补丁（jvm级别的补丁，稳定性未知），曾经也研究过该工程的源码，现在估计也忘得七七八八了吧＞﹏＜

- hotswap-code/libs/metis-hotswap-agent-${project.version}.jar

  自己研究的`Instrumentation`代码
  
  ***Instrumentation 接口***
  
  ```java
  void addTransformer(ClassFileTransformer transformer, boolean canRetransform)//注册ClassFileTransformer实例，注册多个会按照注册顺序进行调用。所有的类被加载完毕之后会调用ClassFileTransformer实例，相当于它们通过了redefineClasses方法进行重定义。布尔值参数canRetransform决定这里被重定义的类是否能够通过retransformClasses方法进行回滚。
  
  void addTransformer(ClassFileTransformer transformer)//相当于addTransformer(transformer, false)，也就是通过ClassFileTransformer实例重定义的类不能进行回滚。
  
  boolean    removeTransformer(ClassFileTransformer transformer)//移除(反注册)ClassFileTransformer实例。
  
  void retransformClasses(Class<?>... classes)//已加载类进行重新转换的方法，重新转换的类会被回调到ClassFileTransformer的列表中进行处理。
  
  void appendToBootstrapClassLoaderSearch(JarFile jarfile)//将某个jar加入到Bootstrap Classpath里优先其他jar被加载。
  
  void appendToSystemClassLoaderSearch(JarFile jarfile)//将某个jar加入到Classpath里供AppClassloard去加载。
  
  Class[]    getAllLoadedClasses()//获取所有已经被加载的类。
  
  Class[]    getInitiatedClasses(ClassLoader loader)//获取所有已经被初始化过了的类。
  
  long getObjectSize(Object objectToSize)//获取某个对象的(字节)大小，注意嵌套对象或者对象中的属性引用需要另外单独计算。
  
  boolean    isModifiableClass(Class<?> theClass)//判断对应类是否被修改过。
  
  boolean    isNativeMethodPrefixSupported()//是否支持设置native方法的前缀。
  
  boolean    isRedefineClassesSupported()//返回当前JVM配置是否支持重定义类（修改类的字节码）的特性。
  
  boolean    isRetransformClassesSupported()//返回当前JVM配置是否支持类重新转换的特性。
  
  void redefineClasses(ClassDefinition... definitions)//重定义类，也就是对已经加载的类进行重定义，ClassDefinition类型的入参包括了对应的类型Class<?>对象和字节码文件对应的字节数组。
  
  void setNativeMethodPrefix(ClassFileTransformer transformer, String prefix)//设置某些native方法的前缀，主要在找native方法的时候做规则匹配。
  ```
