package es.ulpgc.datos.subscriber;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

public class ActiveMQSubscriber implements AutoCloseable {
    private final Connection connection;
    private final Session session;
    private final MessageConsumer priceConsumer;
    private final MessageConsumer newsConsumer;

    public ActiveMQSubscriber(String brokerUrl, String clientId) throws JMSException {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
        this.connection = factory.createConnection();
        // El ClientID es OBLIGATORIO para suscripciones durables
        this.connection.setClientID(clientId);
        this.connection.start();

        this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        // Suscripción durable a los dos topics
        Topic priceTopic = session.createTopic("CryptoPrice");
        this.priceConsumer = session.createDurableSubscriber(priceTopic, "BusinessUnitPriceSub");

        Topic newsTopic = session.createTopic("CryptoNews");
        this.newsConsumer = session.createDurableSubscriber(newsTopic, "BusinessUnitNewsSub");
    }

    // Pasamos un objeto (processor) que sabrá qué hacer cuando llegue un mensaje
    public void startListening(EventProcessor processor) throws JMSException {
        MessageListener listener = message -> {
            try {
                if (message instanceof TextMessage textMessage) {
                    String json = textMessage.getText();
                    // Enviamos el JSON en bruto al procesador
                    processor.processEvent(json);
                }
            } catch (Exception e) {
                System.err.println("Error procesando mensaje: " + e.getMessage());
            }
        };

        priceConsumer.setMessageListener(listener);
        newsConsumer.setMessageListener(listener);
        System.out.println("Escuchando eventos en ActiveMQ...");
    }

    @Override
    public void close() throws Exception {
        priceConsumer.close();
        newsConsumer.close();
        session.close();
        connection.close();
    }
}