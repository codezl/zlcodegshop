package com.atguigu.gmall.seckill.controller;

import com.atguigu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import redis.clients.jedis.Jedis;


@Controller
public class SecKillController {

    @Autowired
    RedisUtil redisUtil;
    @RequestMapping("kill")
    @ResponseBody
    public String kill(){
        String membrId = "1";
        Jedis jedis = redisUtil.getJedis();

        jedis.incrBy("106",-1);
        return "1";

    }
}
