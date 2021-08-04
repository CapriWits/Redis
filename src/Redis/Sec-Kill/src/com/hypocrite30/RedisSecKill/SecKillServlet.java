package com.hypocrite30.RedisSecKill;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Random;

/**
 * @Description: 电商秒杀 servlet
 * @Author: Hypocrite30
 * @Date: 2021/5/24 17:25
 */
public class SecKillServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 用户id
        String userid = new Random().nextInt(50000) + "";
        // 商品id
        String prodid = request.getParameter("prodid");
        // 加了线程池和事务操作
        // boolean isSuccess = SecKill_redis.doSeckill(userid, prodid);
        // 使用Lua脚本增加原子性
        boolean isSuccess = SecKill_redisByScript.doSecKill(userid, prodid);
        response.getWriter().print(isSuccess);
    }
}
