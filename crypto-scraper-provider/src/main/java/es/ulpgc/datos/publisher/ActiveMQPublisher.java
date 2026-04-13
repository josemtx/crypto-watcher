package es.ulpgc.datos.publisher;

import com.google.gson.Gson;
import es.ulpgc.datos.model.NewsItem;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.util.List;

public class ActiveMQPublisher {

    // El puerto por defecto donde ActiveMQ está escuchando
    private static final String BROKER_URL = "tcp://localhost:61616";
    // El nombre del canal según el tipo de evento
    private static final String TOPIC_NAME = "CryptoNews";

    private final Gson gson;

    public ActiveMQPublisher() {
        this.gson = new Gson();
    }

    public void publishNews(List<NewsItem> newsList) {
        if (newsList == null || newsList.isEmpty()) {
            System.out.println("No hay noticias nuevas para publicar.");
            return;
        }

        try {
            // 1. Establecer conexión con el broker
            ConnectionFactory factory = new ActiveMQConnectionFactory(BROKER_URL);
            Connection connection = factory.createConnection();
            connection.start();

            // 2. Crear una sesión (sin transacciones complejas)
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // 3. Crear o apuntar al Topic "CryptoNews"
            Destination topic = session.createTopic(TOPIC_NAME);

            // 4. Crear el productor de mensajes
            MessageProducer producer = session.createProducer(topic);
            // PERSISTENT indica que ActiveMQ debe guardar el mensaje en disco por si se apaga
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            // 5. Convertir a JSON y enviar
            int sentCount = 0;
            for (NewsItem item : newsList) {
                String jsonEvent = gson.toJson(item); // Magia de Gson: Objeto -> JSON
                TextMessage message = session.createTextMessage(jsonEvent);
                producer.send(message);
                sentCount++;
            }

            System.out.println("Éxito: Se han publicado " + sentCount + " eventos en el Topic '" + TOPIC_NAME + "'.");

            // 6. Limpieza: cerrar la conexión
            producer.close();
            session.close();
            connection.close();

        } catch (JMSException e) {
            System.err.println("Error crítico al intentar publicar en ActiveMQ: " + e.getMessage());
        }
    }
}