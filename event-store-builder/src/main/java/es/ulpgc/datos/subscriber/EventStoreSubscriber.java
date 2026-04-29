package es.ulpgc.datos.subscriber;

import es.ulpgc.datos.config.ActiveMqConfig;
import es.ulpgc.datos.store.EventStoreWriter;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

public class EventStoreSubscriber implements AutoCloseable {
    private final EventStoreWriter writer;
    private final Connection connection;
    private final Session session;
    private final MessageConsumer consumer;

    public EventStoreSubscriber(EventStoreWriter writer) {
        this.writer = writer;

        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(ActiveMqConfig.BROKER_URL);

            this.connection = factory.createConnection();
            this.connection.setClientID(ActiveMqConfig.CLIENT_ID);
            this.connection.start();

            this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic(ActiveMqConfig.TOPIC_NAME);

            this.consumer = session.createDurableSubscriber(
                    topic,
                    ActiveMqConfig.DURABLE_SUBSCRIPTION_NAME
            );
        } catch (JMSException e) {
            throw new IllegalStateException("Error creating ActiveMQ subscriber", e);
        }
    }

    public void start() {
        try {
            consumer.setMessageListener(message -> {
                if (message instanceof TextMessage textMessage) {
                    try {
                        String eventJson = textMessage.getText();
                        writer.append(ActiveMqConfig.TOPIC_NAME, eventJson);
                    } catch (JMSException e) {
                        System.err.println("Error reading message from ActiveMQ: " + e.getMessage());
                    }
                }
            });

            System.out.println("Event store subscriber started.");
        } catch (JMSException e) {
            throw new IllegalStateException("Error starting ActiveMQ subscriber", e);
        }
    }

    @Override
    public void close() {
        try {
            consumer.close();
            session.close();
            connection.close();
        } catch (JMSException e) {
            System.err.println("Error closing ActiveMQ subscriber: " + e.getMessage());
        }
    }
}