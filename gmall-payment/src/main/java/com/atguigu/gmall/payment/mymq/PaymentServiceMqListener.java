package com.atguigu.gmall.payment.mymq;

import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.service.PaymentService;
import jdk.nashorn.internal.ir.IfNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import java.util.Date;
import java.util.Map;

@Component
public class PaymentServiceMqListener {

    @Autowired
    PaymentService paymentService;

    @JmsListener(destination = "PAYMENT_CHECK_QUEUE",containerFactory = "")
    public void consumePaymentCheckResult(MapMessage mapMessage) throws JMSException {
        String out_trade_no = mapMessage.getString("out_trade_no");
        Integer count = 0;
        if (mapMessage.getString("count")!=null){
            count =Integer.parseInt( mapMessage.getString("count"));
        }


        //调用pay支付宝接口方法
        System.out.println("进行延迟检查，调用延迟接口哦");
        Map<String,Object> resultMap = paymentService.checkAlipayPayment(out_trade_no);

        if (resultMap != null&&!resultMap.isEmpty()){
            String trade_status = (String)resultMap.get("trade_status");

            //根据支付宝返回状态判断是否进行下一步
            if (trade_status.equals("TRADE_SUCCESS")){
                //ZHI FU 成功，更新状态，发送支付消息队列
                //paymentService.updatePayment(null);
                PaymentInfo paymentInfo = new PaymentInfo();
                paymentInfo.setOrderSn(out_trade_no);
                paymentInfo.setPaymentStatus("已支付");
                paymentInfo.setAlipayTradeNo((String)resultMap.get("trade_no"));// 支付宝的交易凭证号
                paymentInfo.setCallbackContent((String)resultMap.get(("call_back_content")));//回调请求字符串
                paymentInfo.setCallbackTime(new Date());
                System.out.println("已经支付成功，调用支付服务");
                paymentService.updatePayment(paymentInfo);
                return;
            }
        }
                if (count>0){
                    //发送延迟检查，设置计时
                    System.out.println("未成功支付，剩余次数"+count+"继续发送检查消息");
                    count--;
                    paymentService.sendDeletePaymentResultCheckQueue(out_trade_no,count);
                }else {
                    System.out.println("未成功支付，剩余次数不足，不再发送消息检查");
                }
                }
        }



