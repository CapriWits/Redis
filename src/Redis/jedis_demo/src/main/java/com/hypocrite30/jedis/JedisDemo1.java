package com.hypocrite30.jedis;

import org.junit.Test;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.Set;

/**
 * @Description: jedis基本使用
 * @Author: Hypocrite30
 * @Date: 2021/5/16 21:20
 */
public class JedisDemo1 {

    /* redis服务器地址 */
    public static final String remoteAddr = "192.168.248.128";

    /* 记得把Linux防火墙关了「systemctl status/stop firewalld」 */
    @Test
    public void LinkToRedis() {
        // 创建redis对象
        Jedis jedis = new Jedis(remoteAddr, 6379);
        // ping测试
        String value = jedis.ping();
        // PONG
        System.out.println(value);
        jedis.close();
    }

    /* 操作key string */
    @Test
    public void demo1() {
        Jedis jedis = new Jedis(remoteAddr, 6379);

        jedis.set("name", "lucy");
        String name = jedis.get("name");
        System.out.println(name);

        // 设置多个key-value
        jedis.mset("k1", "v1", "k2", "v2");
        List<String> mget = jedis.mget("k1", "k2");
        System.out.println(mget);

        Set<String> keys = jedis.keys("*");
        for (String key : keys) {
            System.out.println(key);
        }
        jedis.close();
    }

    /* 操作list */
    @Test
    public void demo2() {
        Jedis jedis = new Jedis(remoteAddr, 6379);

        jedis.lpush("key1", "lucy", "mary", "jack");
        List<String> values = jedis.lrange("key1", 0, -1);
        System.out.println(values);
        jedis.close();
    }

    /* 操作set */
    @Test
    public void demo3() {
        Jedis jedis = new Jedis(remoteAddr, 6379);

        jedis.sadd("name", "lucy");
        jedis.sadd("name", "lucy");

        Set<String> names = jedis.smembers("name");
        System.out.println(names);
        jedis.close();
    }

    /* 操作hash */
    @Test
    public void demo4() {
        Jedis jedis = new Jedis(remoteAddr, 6379);

        jedis.hset("users", "age", "20");
        String hget = jedis.hget("users", "age");
        System.out.println(hget);
        jedis.close();
    }

    /* 操作zset */
    @Test
    public void demo5() {
        //创建Jedis对象
        Jedis jedis = new Jedis(remoteAddr, 6379);

        jedis.zadd("china", 100d, "shanghai");
        Set<String> china = jedis.zrange("china", 0, -1);
        System.out.println(china);
        jedis.close();
    }

}
