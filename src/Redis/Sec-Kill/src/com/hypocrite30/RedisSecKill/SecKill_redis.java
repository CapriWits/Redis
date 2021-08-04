package com.hypocrite30.RedisSecKill;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

import java.io.IOException;
import java.util.List;

/**
 * 高并发测试使用 apache 的 ab工具「yum install httpd-tools」
 * 在当前目录下创建文件postfile，下面指令-n:请求数 -c:并发数 -T:请求方式 最后跟上请求url
 * ab -n 2000 -c 200 -p ./postfile -T application/x-www-form-urlencoded http://192.168.43.132:8080/Seckill/doseckill
 * 出现问题：
 * 1.连接超时。2000条请求同时过来，redis无法处理，直到超时也没能处理。使用「redis连接池」解决
 * 2.超卖问题。添加事务。注：redis事务不具有原子性，exec阶段错的命令只是跳过，不会回滚
 * 测试前redis中预设商品10件：set sk:0101:qt 20
 * 加上连接池和事务之后，就不会出现超卖问题，即库存为负数，和连接超时问题
 * 但是如果把库存数设置成 500，则会出现卖不完的问题，因为添加事务，redis乐观锁假设有一人抢到商品，打上新版本号
 * 但是这期间的很多人都在抢，因版本号不同而导致抢不到，最终导致商品卖不完。
 * 关于乐观锁没有原子性的缺陷，需要使用Lua脚本来解决，请看SecKill_redisByScript
 * @Description: 秒杀主业务
 * @Author: Hypocrite30
 * @Date: 2021/5/24 17:45
 */
public class SecKill_redis {
    public static final String remoteAddr = "192.168.248.128";

    // 测试连接，关Linux防火墙
    public static void main(String[] args) {
        Jedis jedis = new Jedis(remoteAddr, 6379);
        System.out.println(jedis.ping());
        jedis.close();
    }

    /**
     * 秒杀过程
     * 1.用户id商品id判空 2.连接redis「线程池连接」
     * 3.拼接 库存key「key-value存储」 秒杀成功用户key「set存储」
     * 判断特殊情况：4.判断存储是否null「秒杀未开始」5.判断用户是否重复抢 6.判断商品是否抢完
     * 7.秒杀过程「使用事务添加原子性」「库存-1」「秒杀成功用户存入set记录」
     */
    public static boolean doSeckill(String uid, String prodid) throws IOException {
        // 1 uid prodid判空
        if (uid == null || prodid == null) {
            return false;
        }
        // 2 连接redis
        // Jedis jedis = new Jedis(remoteAddr, 6379);
        // 通过连接池得到jedis对象
        JedisPool jedisPoolInstance = JedisPoolUtil.getJedisPoolInstance();
        Jedis jedis = jedisPoolInstance.getResource();
        // 3 拼接key
        // 3.1 库存key
        String kcKey = "sk:" + prodid + ":qt";
        //监视库存，防止超卖
        jedis.watch(kcKey);
        // 3.2 秒杀成功用户key
        String userKey = "sk:" + prodid + ":user";
        // 4 获取库存，如果库存null，秒杀还没有开始
        String kc = jedis.get(kcKey);
        if (kc == null) {
            System.out.println("秒杀还没开始，请等待");
            jedis.close();
            return false;
        }
        // 5 判断用户是否重复秒杀操作，set集合判断是否存在元素
        if (jedis.sismember(userKey, uid)) {
            System.out.println("已经秒杀成功了，不能重复秒杀");
            jedis.close();
            return false;
        }
        // 6 判断如果商品数量，库存数量小于1，秒杀结束
        kc = jedis.get(kcKey);  // 再获取一次库存值，防止上一次获取期间发生变化影响判断
        if (Integer.parseInt(kc) <= 0) {
            System.out.println("秒杀已经结束了");
            jedis.close();
            return false;
        }
        // 7 秒杀过程
        //使用事务
        Transaction transaction = jedis.multi();
        // jedis.decr(kcKey);
        // jedis.sadd(userKey, uid);
        // 组队操作
        transaction.decr(kcKey);
        transaction.sadd(userKey, uid);
        //执行
        List<Object> res = transaction.exec();
        if (res == null || res.size() == 0) {
            System.out.println("秒杀失败");
            jedis.close();
            return false;
        }
        System.out.println("秒杀成功");
        jedis.close();
        return true;
    }

}
