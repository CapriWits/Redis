package com.hypocrite30.redis_springboot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 首先要清楚分布式锁下每台主机运行分三步
 * 1. 获得锁 2. 执行相关操作 3. 手动释放锁「这一步是写在代码里的，因此是手动释放」
 * 下面生成uuid，是为了在释放锁的时候识别这把锁是不是自己的。
 * 比如：A主机执行操作出问题，执行时间超出预设的过期时间，系统「自动释放」锁，接着锁就被其他主机夺走
 * 当A恢复正常时，程序继续向下运行，在最后一步「手动释放锁」的时候，就不可以释放锁了，因为锁在别人那里
 * @Description: 测试Controller 「服务器要关防火墙」
 * @Author: Hypocrite30
 * @Date: 2021/5/17 21:53
 */
@RestController
@RequestMapping("/redisTest")
public class RedisTestController {

    @Autowired
    private RedisTemplate redisTemplate;

    /* 简单集成Springboot */
    @GetMapping
    public String testRedis() {
        //设置值到redis
        redisTemplate.opsForValue().set("name", "hypocrite");
        //从redis获取值
        String name = (String) redisTemplate.opsForValue().get("name");
        return name;
    }

    /* 测试使用key-value，先在redis预存变量[num, 0]，操作是基于num自增 */
    @GetMapping("testLock")
    public void testLock() {
        // 生成uuid，本机只能释放本机的锁，如果超时锁被「自动」释放，则恢复正常后不可以「手动」释放别人的锁
        String uuid = UUID.randomUUID().toString();
        // 1获取锁，setne，锁名自己取，值可以是uuid的唯一主机标识，加上过期时间，超时锁自动释放，避免死锁
        Boolean isLocked = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 3, TimeUnit.SECONDS);
        // 2获取锁成功、查询num的值
        if (isLocked) {
            Object value = redisTemplate.opsForValue().get("num");
            // 2.1判断num为空return
            if (StringUtils.isEmpty(value)) {
                return;
            }
            // 2.2有值就转成成int
            int num = Integer.parseInt(value + "");
            // 2.3把redis的num加1
            redisTemplate.opsForValue().set("num", ++num);
            // 2.4释放锁，del
            // 判断比较uuid值是否一样
            String lockUuid = (String) redisTemplate.opsForValue().get("lock");
            // 判断锁在不在本机上
            if (lockUuid.equals(uuid)) {
                redisTemplate.delete("lock");
            }
        } else {
            // 3获取锁失败、每隔0.1秒再获取
            try {
                Thread.sleep(100);
                testLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 假如在if判断成功此时锁是在本机上的，准备释放时，正好过期，且被其他主机获得锁
     * 这时再执行手动释放锁操作，就会把别人的锁释放掉，归根结底是「释放锁操作不具备原子性」
     * 这属于Redis的缺陷，使用「Lua脚本」对这一操作添加原子性
     */
    @GetMapping("testLockLua")
    public void testLockLua() {
        // 1 声明一个uuid ,将做为一个value 放入我们的key所对应的值中
        String uuid = UUID.randomUUID().toString();
        // 2 定义一个锁：lua 脚本可以使用同一把锁，来实现删除！
        String skuId = "25"; // 访问skuId 为25号的商品 100008348542
        String locKey = "lock:" + skuId; // 锁住的是每个商品的数据

        // 3 获取锁
        Boolean isLocked = redisTemplate.opsForValue().setIfAbsent(locKey, uuid, 3, TimeUnit.SECONDS);

        // 第一种： lock 与过期时间中间不写任何的代码。
        // redisTemplate.expire("lock",10, TimeUnit.SECONDS);//设置过期时间
        // 如果true
        if (isLocked) {
            // 执行的业务逻辑开始
            // 获取缓存中的num 数据
            Object value = redisTemplate.opsForValue().get("num");
            // 如果是空直接返回
            if (StringUtils.isEmpty(value)) {
                return;
            }
            // 不是空 如果说在这出现了异常！ 那么delete 就删除失败！ 也就是说锁永远存在！
            int num = Integer.parseInt(value + "");
            // 使num 每次+1 放入缓存
            redisTemplate.opsForValue().set("num", String.valueOf(++num));
            // 使用lua脚本来锁
            // 定义lua 脚本，其含义是如果锁value是本机uuid，则释放，否则return 0结束
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            // 使用redis执行lua执行
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptText(script);
            // 设置一下返回值类型 为Long
            // 因为删除判断的时候，返回的0,给其封装为数据类型。如果不封装那么默认返回String 类型，
            // 那么返回字符串与0 会有发生错误。
            redisScript.setResultType(Long.class);
            // 第一个要是script 脚本 ，第二个需要判断的key，第三个就是key所对应的值。
            redisTemplate.execute(redisScript, Arrays.asList(locKey), uuid);
        } else {
            // 其他线程等待
            try {
                // 睡眠
                Thread.sleep(1000);
                // 睡醒了之后，调用方法。
                testLockLua();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


}
