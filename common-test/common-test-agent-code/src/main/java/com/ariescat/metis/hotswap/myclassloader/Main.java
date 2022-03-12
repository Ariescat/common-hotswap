package com.ariescat.metis.hotswap.myclassloader;

import com.ariescat.metis.hotswap.Person;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws ClassNotFoundException {
        int i = 0;

        Person mm = new Person("MM");

        while (true) {
            System.out.println(++i);

            MyClassLoader mcl = new MyClassLoader();
            System.out.println(mcl.getParent());
            Class<?> personClass = mcl.findClass("com/ariescat/metis/hotswap/Person");

//            Class<?> tempClass = Main.class.getClassLoader().loadClass("hotswap.Person");

            try {
                Object person = personClass.newInstance();
                Method sayHelloMethod = personClass.getMethod("sayHello");
                sayHelloMethod.invoke(person);

//                mm = (Person) tempClass.newInstance();
//                mm.sayHello();

//                Field field = Person.class.getDeclaredField("name");
//                field.setAccessible(true);
//                Object o = field.get(new Person("222"));
//                System.err.println(o);

            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

}
