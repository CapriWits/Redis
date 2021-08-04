package com.hypocrite30.jedis;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

/**
 * @Description: Redis集群操作
 * @Author: Hypocrite30
 * @Date: 2021/5/23 17:41
 */
public class RedisClusterDemo {

    public static final String remoteAddr = "192.168.248.128";

    public static void main(String[] args) {
        //创建对象，即使连接的不是主机，也是主机写，从机读
        //无中心化主从集群，无论哪台主机写数据，其他主机都能读到数据
        HostAndPort hostAndPort = new HostAndPort(remoteAddr, 6379);
        //创建集群对象
        JedisCluster jedisCluster = new JedisCluster(hostAndPort);

        //进行操作
        jedisCluster.set("b1", "value1");
        String value = jedisCluster.get("b1");
        System.out.println("value: " + value);

        jedisCluster.close();
    }

}
