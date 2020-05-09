package com.atguigu.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PaymentController {

    @Autowired
    AlipayClient alipayClient;

    @Autowired
    PaymentService paymentService;

    //远程dubbo的代理服务
    @Reference
    OrderService orderService;


    @RequestMapping("alipay/callback/return")
    @LoginRequired(loginSuccess = true)
    public String aliPayCallBackReturn(HttpServletRequest request, ModelMap modelMap) {

        //回调中获取支付宝返回参数
        String sign = request.getParameter("sign");
        String trade_no = request.getParameter("trade_no");
        String out_trade_no = request.getParameter("out_trade_no");
        String trade_status = request.getParameter("trade_status");
        String total_amount = request.getParameter("total_amount");
        String subject = request.getParameter("subject");
        String call_back_content = request.getQueryString();


        //支付宝2.0去掉了验签的参数，导致无法直接验签，组要部署服务器在后台异步验签
        if(StringUtils.isNotBlank(sign)){
            // 验签成功
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setOrderSn(out_trade_no);
            paymentInfo.setPaymentStatus("已支付");
            //得到支付宝支付金额,确保款项正确（嘿嘿）
            //BigDecimal rel_amount=new BigDecimal(total_amount);
            paymentInfo.setRelAmount(total_amount);
            // 支付宝凭证号
            paymentInfo.setAlipayTradeNo(trade_no);
            paymentInfo.setCallbackContent(call_back_content);//回调请求信息
            paymentInfo.setConfirmTime(new Date());
            // 更新用户的支付状态
            paymentService.updatePayment(paymentInfo);

        }else {
            //shibai
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setOrderSn("交易未完成");
            paymentInfo.setPaymentStatus("创建订单失败");
            // 支付宝凭证号
            paymentInfo.setAlipayTradeNo(trade_no);
            paymentInfo.setCallbackContent(call_back_content);//回调请求信息
            paymentInfo.setCallbackTime(new Date());
            // 更新用户的支付状态
            paymentService.updatePayment(paymentInfo);
        }




        //完成后，引起系统服务，去更新后台数据
        return "finish";
    }

        @RequestMapping("mx/submit")
    @LoginRequired(loginSuccess = true)
    @ResponseBody
    public String mx(String outTradeNo, BigDecimal totalAmount, HttpServletRequest request, ModelMap modelMap) {


        //更新支付状态

        return null;
    }

    @RequestMapping("alipay/submit")
    @LoginRequired(loginSuccess = true)
    @ResponseBody
    public String alipay(String outTradeNo, BigDecimal totalAmount, HttpServletRequest request, ModelMap modelMap){

        // 获得一个支付宝请求的客户端(是一个封装的http表单请求)
        String form = null;
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建API对应的request

        // 回调函数
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);

        Map<String,Object> map = new HashMap<>();
        map.put("out_trade_no",outTradeNo);
        map.put("product_code","FAST_INSTANT_TRADE_PAY");
        map.put("total_amount",0.01);
        map.put("subject","2020code商家");

        String param = JSON.toJSONString(map);

        alipayRequest.setBizContent(param);

        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
            System.out.println(form);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        //订单完成保存用户信息
        OmsOrder omsOrder = orderService.getOrderByOutTradeNo(outTradeNo);
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setOrderId(omsOrder.getId());
        paymentInfo.setOrderSn(outTradeNo);
        paymentInfo.setPaymentStatus("订单待付款");
        paymentInfo.setSubject("XXX商品");
        paymentInfo.setTotalAmount(omsOrder.getTotalAmount());

        paymentService.savePaymentInfo(paymentInfo);

        //消息中间件mq发送一个检查支付状态的延迟消息队列
        paymentService.sendDeletePaymentResultCheckQueue(outTradeNo,5);

        //向支付宝提交请求
        return form;
    }

        @RequestMapping("index")
    @LoginRequired(loginSuccess = true)
    public String index(String outTradeNo, BigDecimal totalAmount, HttpServletRequest request, ModelMap modelMap){

        String memberId = (String)request.getAttribute("memberId");
        String nickname = (String)request.getAttribute("nickname");

        modelMap.put("nickname",nickname);
        modelMap.put("outTradeNo",outTradeNo);
        modelMap.put("totalAmount",totalAmount);
        return "index";
    }
}
