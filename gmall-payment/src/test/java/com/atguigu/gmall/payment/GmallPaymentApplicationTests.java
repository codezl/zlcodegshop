package com.atguigu.gmall.payment;

import com.atguigu.gmall.mq.ActiveMQUtil;
import com.atguigu.gmall.payment.mymq.TestMqMy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringRunner;
import tk.mybatis.spring.annotation.MapperScan;

import javax.jms.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallPaymentApplicationTests {

    @Autowired
    TestMqMy activeMQUtil;

    @Test
    public void contextLoads() throws JMSException {

        ConnectionFactory connectionFactory = activeMQUtil.getConnectionFactory();

        Connection connection = connectionFactory.createConnection();

        System.out.println(connection);

    }
}
