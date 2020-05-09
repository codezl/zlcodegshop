package com.atguigu.gmall.payment.mymq;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestMqMyContr {

    @Value("${spring.activemq.broker-url:disabled}")
    String brokerURL ;

    @Value("${activemq.listener.enable:disable}")
    String listenerEnable;

}
