package es.ulpgc.datos.subscriber;

import es.ulpgc.datos.config.ActiveMqConfig;
import es.ulpgc.datos.store.EventStoreWriter;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.lang.IllegalStateException;
import java.util.ArrayList;
import java.util.List;

public class EventStoreSubscriber implements AutoCloseable {
    private final EventStoreWriter writer;
    private final Connection connection;
    private final Session session;
    private final List<MessageConsumer> consumers;

    public EventStoreSubscriber(EventStoreWriter writer) {
        this.writer = writer;
        this.consumers = new ArrayList<>();

        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(ActiveMqConfig.BROKER_URL);

            this.connection = factory.createConnection();
            this.connection.setClientID(ActiveMqConfig.CLIENT_ID);
            this.connection.start();

            this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // ¡NUEVO! Bucle para suscribirse a todos los Topics definidos en la config
            for (String topicName : ActiveMqConfig.TOPICS) {
                Topic topic = session.createTopic(topicName);
                MessageConsumer consumer = session.createDurableSubscriber(
                        topic,
                        ActiveMqConfig.DURABLE_SUBSCRIPTION_NAME + "-" + topicName
                );
                consumers.add(consumer);
            }
        } catch (JMSException e) {
            throw new IllegalStateException("Error creating ActiveMQ subscriber", e);
        }
    }

    public void start() {
        try {
            // ¡NUEVO! Bucle para asignarle el listener a cada consumidor
            for (int i = 0; i < consumers.size(); i++) {
                MessageConsumer consumer = consumers.get(i);
                String topicName = ActiveMqConfig.TOPICS[i]; // Recuperamos el nombre del Topic para pasárselo al Writer

                consumer.setMessageListener(message -> {
                    if (message instanceof TextMessage textMessage) {
                        try {
                            String eventJson = textMessage.getText();
                            writer.append(topicName, eventJson);
                        } catch (JMSException e) {
                            System.err.println("Error reading message from ActiveMQ: " + e.getMessage());
                        }
                    }
                });
            }

            System.out.println("Event store subscriber started for topics: " + String.join(", ", ActiveMqConfig.TOPICS));
        } catch (JMSException e) {
            throw new IllegalStateException("Error starting ActiveMQ subscriber", e);
        }
    }

    @Override
    public void close() {
        try {
            for (MessageConsumer consumer : consumers) {
                consumer.close();
            }
            session.close();
            connection.close();
        } catch (JMSException e) {
            System.err.println("Error closing ActiveMQ subscriber: " + e.getMessage());
        }
    }
}