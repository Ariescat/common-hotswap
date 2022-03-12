ClassLoader:                                                                    
+-groovy.lang.GroovyClassLoader$InnerLoader@36fc695d                            
  +-groovy.lang.GroovyClassLoader@3abeb210                                      
    +-sun.misc.Launcher$AppClassLoader@18b4aac2                                 
      +-sun.misc.Launcher$ExtClassLoader@2ac50a41                               

Location:                                                                       
*********/common-hotswap/common-test/src/main/java/com/metis/groovy/java_call_groovy/TestGroovy2.java

       /*
        * Decompiled with CFR.
        * 
        * Could not load the following classes:
        *  com.metis.groovy.java_call_groovy.JavaBean
        *  com.metis.groovy.java_call_groovy.TestInterface
        */
       package com.metis.groovy.java_call_groovy;
       
       import com.metis.groovy.java_call_groovy.JavaBean;
       import com.metis.groovy.java_call_groovy.TestInterface;
       import groovy.lang.GroovyObject;
       import groovy.lang.MetaClass;
       import groovy.transform.Generated;
       import groovy.transform.Internal;
       import java.lang.ref.SoftReference;
       import org.codehaus.groovy.reflection.ClassInfo;
       import org.codehaus.groovy.runtime.ScriptBytecodeAdapter;
       import org.codehaus.groovy.runtime.callsite.CallSite;
       import org.codehaus.groovy.runtime.callsite.CallSiteArray;
       
       public class TestGroovy2
       implements TestInterface,
       GroovyObject {
           private static /* synthetic */ ClassInfo $staticClassInfo;
           public static transient /* synthetic */ boolean __$stMC;
           private transient /* synthetic */ MetaClass metaClass;
           private static /* synthetic */ SoftReference $callSiteArray;
       
           @Generated
           public TestGroovy2() {
               MetaClass metaClass;
               CallSite[] callSiteArray = TestGroovy2.$getCallSiteArray();
               this.metaClass = metaClass = this.$getStaticMetaClass();
           }
       
           public void print(String msg) {
               CallSite[] callSiteArray = TestGroovy2.$getCallSiteArray();
/*14*/         callSiteArray[0].call(callSiteArray[1].callGetProperty(System.class), msg);
/*23*/         callSiteArray[2].call((Object)JavaBean.class, (Object)null);
           }
       
           private void testCall() {
               CallSite[] callSiteArray = TestGroovy2.$getCallSiteArray();
/*28*/         throw (Throwable)callSiteArray[3].callConstructor(RuntimeException.class, "testCall");
           }
       
           protected /* synthetic */ MetaClass $getStaticMetaClass() {
               if (this.getClass() != TestGroovy2.class) {
                   return ScriptBytecodeAdapter.initMetaClass(this);
               }
               ClassInfo classInfo = $staticClassInfo;
               if (classInfo == null) {
                   $staticClassInfo = classInfo = ClassInfo.getClassInfo(this.getClass());
               }
               return classInfo.getMetaClass();
           }
       
           @Override
           @Generated
           @Internal
           public /* synthetic */ MetaClass getMetaClass() {
               MetaClass metaClass = this.metaClass;
               if (metaClass != null) {
                   return metaClass;
               }
               this.metaClass = this.$getStaticMetaClass();
               return this.metaClass;
           }
       
           @Override
           @Generated
           @Internal
           public /* synthetic */ void setMetaClass(MetaClass metaClass) {
               this.metaClass = metaClass;
           }
       
           @Override
           @Generated
           @Internal
           public /* synthetic */ Object invokeMethod(String string, Object object) {
               return this.getMetaClass().invokeMethod((Object)this, string, object);
           }
       
           @Override
           @Generated
           @Internal
           public /* synthetic */ Object getProperty(String string) {
               return this.getMetaClass().getProperty(this, string);
           }
       
           @Override
           @Generated
           @Internal
           public /* synthetic */ void setProperty(String string, Object object) {
               this.getMetaClass().setProperty(this, string, object);
           }
       
           private static /* synthetic */ void $createCallSiteArray_1(String[] stringArray) {
               stringArray[0] = "println";
               stringArray[1] = "out";
               stringArray[2] = "func";
               stringArray[3] = "<$constructor$>";
           }
       
           private static /* synthetic */ CallSiteArray $createCallSiteArray() {
               String[] stringArray = new String[4];
               TestGroovy2.$createCallSiteArray_1(stringArray);
               return new CallSiteArray(TestGroovy2.class, stringArray);
           }
       
           private static /* synthetic */ CallSite[] $getCallSiteArray() {
               CallSiteArray callSiteArray;
               if ($callSiteArray == null || (callSiteArray = (CallSiteArray)$callSiteArray.get()) == null) {
                   callSiteArray = TestGroovy2.$createCallSiteArray();
                   $callSiteArray = new SoftReference<CallSiteArray>(callSiteArray);
               }
               return callSiteArray.array;
           }
       }

Affect(row-cnt:1) cost in 792 ms.