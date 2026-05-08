package es.ulpgc.datos.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.ulpgc.datos.domain.CryptoPriceEvent;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

public class ActiveMqEventPublisher implements AutoCloseable {
    private final ObjectMapper objectMapper;
    private final Connection connection;
    private final Session session;
    private final MessageProducer producer;

    public ActiveMqEventPublisher(String brokerUrl, String topicName) {
        try {
            this.objectMapper = new ObjectMapper();

            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
            this.connection = factory.createConnection();
            this.connection.start();

            this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic(topicName);
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