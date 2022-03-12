package com.metis.groovy.java_call_groovy;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

import java.util.List;

public class TestGroovy1 {

    public List<String> print() {
        // throw new RuntimeException("ttt");

        String jsonString = "[\"111\",\"222\",\"333\"]";
        return JSON.parseObject(jsonString, new TypeReference<List<String>>() {
        });
    }

    public List<String> printArgs(String str1, String str2, String str3) {
        // throw new RuntimeException("ttt");

        String jsonString = "[\"" + str1 + "\",\"" + str2 + "\",\"" + str3 + "\"]";
        return JSON.parseObject(jsonString, new TypeReference<List<String>>() {
        });
    }
}
