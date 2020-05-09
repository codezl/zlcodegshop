package com.atguigu.gmall.payment.test;


import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;

/**
 * activemq demo测试
 * @author ouyangjun
 */
public class testMqSp2 {

    // TCP是ActiveMQ默认的协议,访问方式是 : tcp://hostname:port?key=value&key=value,如: tcp://localhost:61616?trace=true
    private final static String ACTIVEMQ_CONNECTION_URL = "tcp://localhost:61616";

    // 队列名称
    private final static String ACTIVEMQ_QUEUE_NAME = "QUEUE.codezl";

    /**
     * 消息生产者
     */
    public static class HelloActiveMQProducer implements Runnable {

        @Override
        public void run() {
            int count = 1;
            while(true) {
                String text = "Hello ActiveMQ! Number: " + count;
                producer(text);

                count++;
                try {
                    // 睡眠
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public static void producer(String text) {
            ActiveMQConnectionFactory connectionFactory = null;
            Connection connection = null;
            Session session = null;
            MessageProducer messageProducer = null;
            try {
                // 创建activemq工厂,连接到activemq
                connectionFactory = new ActiveMQConnectionFactory(ACTIVEMQ_CONNECTION_URL);

                // 创建连接
                connection = connectionFactory.createConnection();
                connection.start();

                // 创建session
				/*
				JMS规范的ack消息确认机制有一下四种，定于在session对象中：
				AUTO_ACKNOWLEDGE = 1 ：自动确认
				CLIENT_ACKNOWLEDGE = 2：客户端手动确认
				DUPS_OK_ACKNOWLEDGE = 3： 自动批量确认
				SESSION_TRANSACTED = 0：事务提交并确认
				*/
                session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

                // 创建目标(主题或队列)
                Destination destination = session.createQueue(ACTIVEMQ_QUEUE_NAME);

                // 从会话到主题或队列创建MessageProducer
                messageProducer = session.createProducer(destination);
                messageProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

                // 创建文本消息
                System.err.println("HelloProducer Text Received: " + text);
                TextMessage message = session.createTextMessage(text);

                // 告诉制片人发送信息
                messageProducer.send(message);
            } catch (JMSException e) {
                e.printStackTrace();
            } finally {
                try {
                    // 关闭messageProducer
                    if(messageProducer != null) {
                        messageProducer.close();
                    }
                    // 关闭session
                    if(session != null) {
                        session.close();
                    }
                    // 关闭connection
                    if(connection != null) {
                        connection.close();
                    }
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 消息消费者
     */
    public static class HelloActiveMQConsumer implements Runnable {

        @Override
        public void run() {
            while(true) {
                consumer();
            }
        }

        public static void consumer() {
            ActiveMQConnectionFactory connectionFactory = null;
            Connection connection = null;
            Session session = null;
            MessageConsumer messageConsumer = null;
            try {
                // 创建activemq工厂,连接到activemq
                connectionFactory = new ActiveMQConnectionFactory(ACTIVEMQ_CONNECTION_URL);

                // 创建连接
                connection = connectionFactory.createConnection();
                connection.start();

                // 创建session
                session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

                // 创建目标(主题或队列)
                Destination destination = session.createQueue(ACTIVEMQ_QUEUE_NAME);

                // 从会话到主题或队列创建MessageConsuer
                messageConsumer = session.createConsumer(destination);

                // 等待消息
                Message message = messageConsumer.receive(1000);
                if (message instanceof TextMessage) {
                    TextMessage textMessage = (TextMessage) message;
                    String text = textMessage.getText();
                    System.err.println("HelloConsumer Text Received: " + text);
                } else {
                    System.err.println("HelloConsumer Received: " + message);
                }
            } catch (JMSException e) {
                e.printStackTrace();
            } finally {
                try {
                    // 关闭messageConsumer
                    if(messageConsumer != null) {
                        messageConsumer.close();
                    }
                    // 关闭session
                    if(session != null) {
                        session.close();
                    }
                    // 关闭connection
                    if(connection != null) {
                        connection.close();
                    }
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * main
     * @param args
     */
    public static void main(String[] args) {
        Thread thread1 = new Thread(new HelloActiveMQProducer());
        Thread thread2 = new Thread(new HelloActiveMQConsumer());

        // 生产消息
        thread1.start();
        // 消费消息
        thread2.start();
    }

}