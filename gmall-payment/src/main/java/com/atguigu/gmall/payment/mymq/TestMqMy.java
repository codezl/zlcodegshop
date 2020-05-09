package com.atguigu.gmall.payment.mymq;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.jms.*;

@Component
public class TestMqMy {

    @Value("${spring.activemq.broker-url:disabled}")
    String brokerUrl ;

        ConnectionFactory connect = new ActiveMQConnectionFactory();
    public ConnectionFactory init(String brokerUrl) {
             connect = new ActiveMQConnectionFactory(ActiveMQConnection.DEFAULT_USER,ActiveMQConnection.DEFAULT_PASSWORD,brokerUrl);
//            try {
//            Connection connection = connect.createConnection();
//            connection.start();
//            //第一个值表示是否使用事务，如果选择true，第二个值相当于选择0
//            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
//            Destination testqueue = session.createQueue("Help");
//
//            MessageConsumer consumer = session.createConsumer(testqueue);
//            consumer.setMessageListener(new MessageListener() {
//                @Override
//                public void onMessage(Message message) {
//                    if(message instanceof TextMessage){
//                        try {
//                            String text = ((TextMessage) message).getText();
//                            System.err.println(text+"i am grade to help you");
//
//                            //session.rollback();
//                        } catch (JMSException e) {
//                            // TODO Auto-generated catch block
//                            e.printStackTrace();
//                        }
//                    }
//                }
//            });
//
//
//        }catch (Exception e){
//            e.printStackTrace();;
//        }
            return connect;
    }
    public ConnectionFactory getConnectionFactory(){
        return connect;
    }
}
