package com.atguigu.gmall.interceptors;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.HttpclientUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;


@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //拦截代码

        //判断被拦截的请求方法注解
        HandlerMethod hm = (HandlerMethod) handler;
        LoginRequired methodAnnotation = hm.getMethodAnnotation(LoginRequired.class);

        StringBuffer url = request.getRequestURL();
        System.out.println(url);
        //是否拦截
        if (methodAnnotation == null) {
            return true;
        }

        String token = "";

        String oldToken = CookieUtil.getCookieValue(request, "oldToken", true);
        if (StringUtils.isNotBlank(oldToken)) {
            token = oldToken;
        }

        String newToken = request.getParameter("token");
        if (StringUtils.isNotBlank(newToken)) {
            token = newToken;
        }

        boolean loginSuccess = methodAnnotation.loginSuccess();//获得请i去是否必须登陆

        //验证：调用验证中心
        String success = "fail";
        Map<String,String> successMap = new HashMap<>();
        if (StringUtils.isNotBlank(token)) {
            //json包含以下判断的success，member，name
            String ip = request.getHeader("x-forwarded-for");//nginx服务器转发客户端ip
            if (StringUtils.isBlank(ip)){
                ip = request.getRemoteAddr();//从request中得到ip
                if (StringUtils.isBlank(ip)){
                    ip = "127.0.0.1";
                }
            }
            String successJson = HttpclientUtil.doGet("http://passport.gmall.com:8085/verify?token=" + token+"&currentIp="+ip);

            successMap = JSON.parseObject(successJson, Map.class);
            success = successMap.get("status");
        }

        if (loginSuccess) {
            //登陆成功才可使用
            if (!success.equals("success")) {
                //重定向回登陆
                StringBuffer requestURL = request.getRequestURL();
                response.sendRedirect("http://passport.gmall.com:8085/index?ReturnUrl=" + requestURL);
                return false;
            }
            //验证通过，覆盖缓存token
            //将token携带的用户信息写入
            request.setAttribute("memberId", successMap.get("memberId"));
            request.setAttribute("nickname", successMap.get("nickname"));
            //验证通过，覆盖缓存token
            if (StringUtils.isNotBlank(token)) {
                CookieUtil.setCookie(request, response, "oldToken", token, 60 * 60 * 2, true);
            }

        } else {
            //未登录可使用，但需要验证，例如购物车
            //验证
            if (success.equals("success")) {
                //将token携带的用户信息写入
                request.setAttribute("memberId", successMap.get("memberId"));
                request.setAttribute("nickname", successMap.get("nickname"));
                //验证通过，覆盖缓存token
                if (StringUtils.isNotBlank(token)) {
                    CookieUtil.setCookie(request, response, "oldToken", token, 60 * 60 * 2, true);
                }
            }
        }

        //System.out.println("出发拦截器拦截");
        return true;
    }
}