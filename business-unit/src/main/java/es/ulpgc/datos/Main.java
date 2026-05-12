package es.ulpgc.datos;

import es.ulpgc.datos.api.DatamartApiController;
import es.ulpgc.datos.datamart.DatamartInitializer;
import es.ulpgc.datos.datamart.DatamartUpdater;
import es.ulpgc.datos.datamart.EventStoreReader;
import es.ulpgc.datos.subscriber.ActiveMQSubscriber;

public class Main {
    public static void main(String[] args) {
        // Añadimos un tercer parámetro: la ruta a la carpeta 'eventstore'
        if (args.length < 3) {
            System.err.println("Uso: java -jar business-unit.jar <broker_url> <sqlite_db_path> <eventstore_path>");
            System.err.println("Ejemplo: tcp://localhost:61616 datamart.db ./eventstore");
            System.exit(1);
        }

        String brokerUrl = args[0];
        String dbPath = args[1];
        String eventStorePath = args[2]; // Capturamos la ruta

        System.out.println("Iniciando Business Unit...");

        // 1. Preparar la Base de Datos (Tablas vacías)
        DatamartInitializer dbInitializer = new DatamartInitializer(dbPath);
        dbInitializer.initialize();

        // Creamos el procesador que usaremos tanto para el pasado como para el futuro
        DatamartUpdater updater = new DatamartUpdater(dbPath);

        // 2. VIAJE AL PASADO: Leer y procesar archivos históricos antes de conectarnos a la red
        EventStoreReader historyReader = new EventStoreReader(eventStorePath);
        historyReader.restoreDatamart(updater);

        // 3. EL PRESENTE: Conectar a ActiveMQ para recibir lo nuevo
        try {
            ActiveMQSubscriber subscriber = new ActiveMQSubscriber(brokerUrl, "BusinessUnitApp");

            subscriber.startListening(updater);

            // 4. Iniciar el servidor API REST para el Dashboard
            DatamartApiController apiController = new DatamartApiController(dbPath);
            apiController.start(8080); // Levantamos el servidor en el puerto 8080

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Cerrando Business Unit...");
                try { subscriber.close(); } catch (Exception ignored) {}
                apiController.stop(); // Apagamos la API de forma segura
            }));

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Cerrando Business Unit...");
                try { subscriber.close(); } catch (Exception ignored) {}
            }));

        } catch (Exception e) {
            System.err.println("Error crítico arrancando el suscriptor: " + e.getMessage());
        }
    }
}