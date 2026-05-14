package es.ulpgc.datos;

import es.ulpgc.datos.api.DatamartApiController;
import es.ulpgc.datos.datamart.DatamartInitializer;
import es.ulpgc.datos.datamart.DatamartUpdater;
import es.ulpgc.datos.datamart.EventStoreReader;
import es.ulpgc.datos.subscriber.ActiveMQSubscriber;

public class Main {
    private static final int DEFAULT_API_PORT = 8080;
    private static final String SUBSCRIBER_ID = "BusinessUnitApp";

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Uso: java -jar business-unit.jar <broker_url> <sqlite_db_path> <eventstore_path>");
            System.err.println("Ejemplo: tcp://localhost:61616 datamart.db ./eventstore");
            System.exit(1);
        }

        String brokerUrl = args[0];
        String dbPath = args[1];
        String eventStorePath = args[2];

        System.out.println("Iniciando Business Unit...");
        System.out.println("  Broker URL     : " + brokerUrl);
        System.out.println("  SQLite DB Path : " + dbPath);
        System.out.println("  Event Store    : " + eventStorePath);
        System.out.println("  API Port       : " + DEFAULT_API_PORT);

        DatamartInitializer datamartInitializer = new DatamartInitializer(dbPath);
        datamartInitializer.initialize();

        DatamartUpdater datamartUpdater = new DatamartUpdater(dbPath);

        EventStoreReader historyReader = new EventStoreReader(eventStorePath);
        historyReader.restoreDatamart(datamartUpdater);

        try {
            ActiveMQSubscriber subscriber = new ActiveMQSubscriber(brokerUrl, SUBSCRIBER_ID);
            subscriber.startListening(datamartUpdater);

            DatamartApiController apiController = new DatamartApiController(dbPath);
            apiController.start(DEFAULT_API_PORT);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Cerrando Business Unit...");
                try {
                    subscriber.close();
                } catch (Exception ignored) {
                }
                apiController.stop();
            }));

        } catch (Exception e) {
            System.err.println("Error crítico arrancando la Business Unit: " + e.getMessage());
            e.printStackTrace();
        }
    }
}