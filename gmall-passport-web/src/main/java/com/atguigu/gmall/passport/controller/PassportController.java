package com.atguigu.gmall.passport.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.HttpclientUtil;
import com.atguigu.gmall.util.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {

    @Reference
    UserService userService;

    @RequestMapping("vlogin")
    public String vlogin(String code, HttpServletRequest request, HttpServletResponse response){
        //授权码获取token
        String s3 = "https://api.weibo.com/oauth2/access_token?";//?client_id=1945460522&client_secret=ea4b0a5af5936b88da29d0f29cafbd31&grant_type=authorization_code&redirect_uri=http://passport.gmall.com:8085/vlogin&code=CODE";

        Map<String,String> paramMap = new HashMap<>();
        paramMap.put("client_id","1176073937");
        paramMap.put("client_secret","949fe047ed9e121bcce744b7e63bd9af");
        paramMap.put("grant_type","authorization_code");
        paramMap.put("redirect_uri","http://passport.gmall.com:8085/vlogin");
        paramMap.put("code",code);
        String access_token_json = HttpclientUtil.doPost(s3,paramMap);

        Map<String,Object> access_map = JSON.parseObject(access_token_json,Map.class);



        //token获取用户信息
        String uid = (String)access_map.get("uid");
        String access_token = (String)access_map.get("access_token");
        String show_user_url = "https://api.weibo.com/2/users/show.json?access_token="+access_token+"&uid="+uid;
        String user_json = HttpclientUtil.doGet(show_user_url);

        Map<String,Object> user_map = JSON.parseObject(user_json,Map.class);

        //用户信息保存数据库，用为类型设置为微博
        UmsMember umsMember = new UmsMember();
        umsMember.setSourceType("2");
        umsMember.setAccessToken(access_token);
        umsMember.setAccessCode(code);
        umsMember.setNickname((String)user_map.get("screen_name"));
        umsMember.setSourceUid((String) user_map.get("idstr"));
        umsMember.setCity((String) user_map.get("location"));

        //性别0为女，1为男，2为未知，微博接口传入的为m，f，n
        String g = "0";
        String gender = (String) user_map.get("gender");
        if (gender.equals("m")){
            g = "1";
        }
        umsMember.setGender(g);

        UmsMember umsCheck = new UmsMember();
        umsCheck.setSourceUid(umsMember.getSourceUid());
        UmsMember umsMemberCheck = userService.checkOauthUser(umsCheck);
        if (umsMemberCheck==null){
            umsMember = userService.addOauthUser(umsMember);
        }else {
            umsMember = umsMemberCheck;
        }

        //生成token，重定向到首页，携带token
        String token = null;
        String memberId = umsMember.getId();//rpc主键策略失效
        String nickname = umsMember.getNickname();
        //可以优化代码
        Map<String,Object> userMap = new HashMap<>();
        userMap.put("memberId",memberId);//保存数据库后主键返回策略生成id,faxian主键策略失效，修改方法为继承类
        userMap.put("nickname",nickname);

        String ip = request.getHeader("x-forwarded-for");//nginx服务器转发客户端ip
        if (StringUtils.isBlank(ip)){
            ip = request.getRemoteAddr();//从request中得到ip
            if (StringUtils.isBlank(ip)){
                ip = "127.0.0.1";
            }
        }

        //设计一套加密算法对下面参数进行加秘密，生成token
        //可以加上时间参数使得算法更加安全，ip + time，毕设缘故可以用就行
        //String time = new SimpleDateFormat("yyyyMMdd HHmmss").format(new Date());
        token = JwtUtil.encode("2020gmall0105", userMap, ip);

        //token存入缓存redis
        userService.addUserToken(token,memberId);
        //将nickename和id存入cookie

        CookieUtil.setCookie(request, response, "username", nickname, 60 * 60 * 2, false);
        CookieUtil.setCookie(request, response, "userid", memberId, 60 * 60 * 2, true);


        //携带token
        return "redirect:http://search.gmall.com:8083/index?token="+token+"&nickname="+nickname;
    }
    @RequestMapping("verify")
    @ResponseBody
    public String verify(String token,String currentIp,HttpServletRequest request){

        // 通过jwt校验token
        Map<String,String> map = new HashMap<>();


        Map<String, Object> decode = JwtUtil.decode(token, "2020gmall0105", currentIp);

        if (decode!=null){
            map.put("status","success");
            map.put("memberId",(String) decode.get("memberId"));
            map.put("nickname",(String) decode.get("nickname"));
        }else {
            map.put("status","fail");
        }


        return JSON.toJSONString(map);
    }

    @RequestMapping("login")
    @ResponseBody
    public String login(UmsMember umsMember, HttpServletRequest request){

        String token = "";
        //调用用户服务验证用户信息
        UmsMember umsMemberLogin = userService.login(umsMember);

        if (umsMemberLogin!=null){
            //登陆成功

            //jwt制作token
            String memberId = umsMemberLogin.getId();
            String nickname = umsMemberLogin.getNickname();
            Map<String,Object> userMap = new HashMap<>();
            userMap.put("memberId",memberId);
            userMap.put("nickname",nickname);

            String ip = request.getHeader("x-forwarded-for");//nginx服务器转发客户端ip
            if (StringUtils.isBlank(ip)){
                ip = request.getRemoteAddr();//从request中得到ip
                if (StringUtils.isBlank(ip)){
                    ip = "127.0.0.1";
                }
            }

            //设计一套加密算法对下面参数进行加秘密，生成token
            token = JwtUtil.encode("2020gmall0105", userMap, ip);

            //token存入缓存redis
            userService.addUserToken(token,memberId);

        }else {
            //shibai
            token = "fail";
        }
        return token;
    }
    @RequestMapping("index")
    public String index(String ReturnUrl,ModelMap modelMap){

        modelMap.put("ReturnUrl",ReturnUrl);

        return "index";
    }
}
