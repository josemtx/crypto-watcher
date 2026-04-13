package es.ulpgc.datos.subscriber;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.nio.file.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class ActiveMQSubscriber {

    private static final String BROKER_URL = "tcp://localhost:61616";
    // El ClientID es OBLIGATORIO para que la suscripción sea duradera
    private static final String CLIENT_ID = "EventStoreBuilderApp";
    private static final String BASE_DIR = "eventstore";

    public void startListening(String topicName) {
        try {
            ConnectionFactory factory = new ActiveMQConnectionFactory(BROKER_URL);
            Connection connection = factory.createConnection();
            connection.setClientID(CLIENT_ID);
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic(topicName);

            // Crear el suscriptor duradero [cite: 71]
            MessageConsumer consumer = session.createDurableSubscriber(topic, topicName + "-Subscription");

            System.out.println("Suscrito duraderamente al topic: " + topicName + ". Esperando eventos...");

            // Escuchador asíncrono: se dispara automáticamente cuando llega un mensaje
            consumer.setMessageListener(message -> {
                if (message instanceof TextMessage textMessage) {
                    try {
                        String jsonEvent = textMessage.getText();
                        processAndSaveEvent(topicName, jsonEvent);
                    } catch (JMSException e) {
                        System.err.println("Error al extraer el texto del mensaje: " + e.getMessage());
                    }
                }
            });

        } catch (JMSException e) {
            System.err.println("Error crítico al conectar con ActiveMQ: " + e.getMessage());
        }
    }

    private void processAndSaveEvent(String topicName, String jsonEvent) {
        try {
            // 1. Parsear el JSON para extraer 'ts' (timestamp) y 'ss' (source)
            JsonObject jsonObject = JsonParser.parseString(jsonEvent).getAsJsonObject();
            String ss = jsonObject.get("ss").getAsString();
            String ts = jsonObject.get("ts").getAsString();

            // 2. Formatear la fecha a YYYYMMDD (ej: 20260413) [cite: 48, 78]
            String dateStr = Instant.parse(ts)
                    .atZone(ZoneId.of("UTC"))
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            // 3. Construir la ruta: eventstore/{topic}/{ss}/{YYYYMMDD}.events [cite: 46-48, 77-78]
            Path directoryPath = Paths.get(BASE_DIR, topicName, ss);
            Files.createDirectories(directoryPath);

            Path filePath = directoryPath.resolve(dateStr + ".events");

            // 4. Guardar en el archivo añadiendo al final (append) con salto de línea (NDJSON)
            String lineToSave = jsonEvent + System.lineSeparator();
            Files.writeString(filePath, lineToSave, StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            System.out.println("Evento guardado en: " + filePath.toString());

        } catch (Exception e) {
            System.err.println("Error al procesar y guardar el evento en disco: " + e.getMessage());
        }
    }
}