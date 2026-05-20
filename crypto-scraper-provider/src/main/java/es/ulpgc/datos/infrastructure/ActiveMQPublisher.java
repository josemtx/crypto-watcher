package es.ulpgc.datos.infrastructure;

import com.google.gson.Gson;
import es.ulpgc.datos.domain.CryptoNewsEvent;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

public class ActiveMQPublisher implements AutoCloseable {

    private final Gson gson;
    private final Connection connection;
    private final Session session;
    private final MessageProducer producer;

    public ActiveMQPublisher(String brokerUrl, String topicName) {
        this.gson = new Gson();

        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
            this.connection = factory.createConnection();
            this.connection.start();

            this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination topic = session.createTopic(topicName);
            this.producer = session.createProducer(topic);
            this.producer.setDeliveryMode(DeliveryMode.PERSISTENT);

        } catch (JMSException e) {
            throw new IllegalStateException("Error crítico al inicializar el publicador de ActiveMQ", e);
        }
    }

    public void publish(CryptoNewsEvent event) {
        try {
            String jsonEvent = gson.toJson(event);
            TextMessage message = session.createTextMessage(jsonEvent);
            producer.send(message);
            System.out.println("Evento publicado: " + jsonEvent);
        } catch (JMSException e) {
            System.err.println("Error publicando el evento de noticia: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            if (producer != null) producer.close();
            if (session != null) session.close();
            if (connection != null) connection.close();
            System.out.println("Conexión con ActiveMQ cerrada limpiamente.");
        } catch (JMSException e) {
            System.err.println("Error cerrando los recursos de ActiveMQ: " + e.getMessage());
        }
    }
}