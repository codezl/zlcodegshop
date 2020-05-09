package com.atguigu.gmall.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class testJwt {
    public static void main(String[] args){
        Map<String,Object> map = new HashMap<>();
        map.put("memberId","1");
        map.put("nickname","zhang");
        String ip = "127.0.0.0";
        String time = new SimpleDateFormat("yyyyMMdd HHmmss").format(new Date());
        String encode = JwtUtil.encode("2020gmallzzl", map, ip + time);

        System.err.println(encode);
    }
}
