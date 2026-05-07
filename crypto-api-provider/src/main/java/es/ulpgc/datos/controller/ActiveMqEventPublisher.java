package es.ulpgc.datos.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.ulpgc.datos.config.ActiveMqConfig;
import es.ulpgc.datos.event.CryptoPriceEvent;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import org.apache.activemq.ActiveMQConnectionFactory;

public class ActiveMqEventPublisher implements AutoCloseable {
    private final ObjectMapper objectMapper;
    private final Connection connection;
    private final Session session;
    private final MessageProducer producer;

    public ActiveMqEventPublisher() {
        try {
            this.objectMapper = new ObjectMapper();

            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(ActiveMqConfig.BROKER_URL);
            this.connection = factory.createConnection();
            this.connection.start();

            this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic(ActiveMqConfig.TOPIC_NAME);
            this.producer = session.createProducer(topic);
        } catch (JMSException e) {
            throw new IllegalStateException("Error creating ActiveMQ publisher", e);
        }
    }

    public void publish(CryptoPriceEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            TextMessage message = session.createTextMessage(json);
            producer.send(message);
            System.out.println("Published event: " + json);
        } catch (JsonProcessingException e) {
            System.err.println("Error serializing crypto event: " + e.getMessage());
        } catch (JMSException e) {
            System.err.println("Error publishing crypto event: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            producer.close();
            session.close();
            connection.close();
        } catch (JMSException e) {
            System.err.println("Error closing ActiveMQ resources: " + e.getMessage());
        }
    }
}