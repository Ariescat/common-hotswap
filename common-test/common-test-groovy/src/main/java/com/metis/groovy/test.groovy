package com.metis.groovy

System.out.println("hello world")
System.out.println "hello world"

println("hello world")
println 'hello world'

int[] array = [1, 2, 3]

static int method(String arg) {
    return 1
}

static int method(Object arg) {
    return 2
}

Object o = "Object"
int result = method(o)
// In Java
assertEquals(2, result)
// In Groovy
assertEquals(1, result)