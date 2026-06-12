package com.bbq.module.flashsale.controller;

import com.alibaba.fastjson2.JSONObject;
import com.gitee.apanlh.util.random.RandomUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 哈希冲突测试类
 * 用于分析字符串或自定义对象的 hashCode 分布情况，统计冲突次数
 */
@RestController
public class HashCollisionTest {

    private final AtomicInteger count = new AtomicInteger(0);
//    private final ReentrantLock lock = new ReentrantLock();

    Integer str;
    @GetMapping("/test")
    public Object test() {
        str = RandomUtils.randomInt(100);
        System.out.println(str);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("count", count.get());
        return jsonObject;
    }

    private synchronized void test2() {
        for (int i = 0; i < 100; i++) {
            count.getAndIncrement();
        }
        System.out.println(count.get());

    }

    public static void main(String[] args) {
        TreeSet<Object> objects1 = new TreeSet<>();
        objects1.add(3);
        objects1.add(4);
        objects1.add(1);
        objects1.add(6);
        TreeSet<Object> objects2 = new TreeSet<>();
        objects2.add("bann");
        objects2.add("orig");
        objects2.add("apple");
        objects2.add("zook");
        objects2.add("jugo");
        HashMap<Object, Object> map = new HashMap<>();
        map.put("123", "123");
        map.hashCode();
        map.entrySet();
        System.out.println(objects1);
        System.out.println(objects2);
    }
}