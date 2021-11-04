package com.ariescat.hotswap.example.instrument;

/**
 * @author Ariescat
 * @version 2020/1/11 23:40
 */
public class RedefineBean {

    public void print() {
        System.out.println("print ori");
//        System.out.println("print after");
    }

    public static class InnerClass2 {
        private int j;

        public InnerClass2(int j) {
            this.j = j;
        }

        @Override
        public String toString() {
            return "InnerClass2{" +
                    "j=" + j +
                    '}';
        }
    }
}
