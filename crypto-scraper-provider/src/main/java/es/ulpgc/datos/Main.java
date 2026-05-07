package es.ulpgc.datos;

import es.ulpgc.datos.analyzer.ApiNinjasAnalyzer; // <- NUEVO IMPORT
import es.ulpgc.datos.analyzer.SentimentAnalyzer;
import es.ulpgc.datos.controller.ScraperController;
import es.ulpgc.datos.feeder.CoinDeskFeeder;
import es.ulpgc.datos.feeder.NewsFeeder;
import es.ulpgc.datos.mapper.NewsEventMapper;
import es.ulpgc.datos.publisher.ActiveMQPublisher;
import es.ulpgc.datos.util.ArticleSanitizer;

public class Main {
    public static void main(String[] args) {
        // 1. Validación estricta de argumentos
        if (args.length < 4) {
            System.err.println("Error: Faltan argumentos de configuración.");
            System.err.println("Uso esperado: java -jar app.jar <broker_url> <topic_name> <source_id> <api_ninjas_key>");
            System.err.println("Ejemplo: tcp://localhost:61616 CryptoNews CoinDeskScraper tu_api_key_aqui");
            System.exit(1);
        }

        // 2. Extracción de los argumentos
        String brokerUrl = args[0];
        String topicName = args[1];
        String sourceId = args[2];
        String apiKey = args[3]; // Ahora guardamos la llave de API-Ninjas

        System.out.println("Inicializando Crypto Scraper Provider para el topic: " + topicName);

        // 3. Instanciar todas las dependencias
        NewsFeeder feeder = new CoinDeskFeeder();
        ArticleSanitizer sanitizer = new ArticleSanitizer();

        // ¡CAMBIO CLAVE AQUÍ! Usamos la nueva clase pasándole la API Key
        SentimentAnalyzer analyzer = new ApiNinjasAnalyzer(apiKey);

        NewsEventMapper mapper = new NewsEventMapper();
        ActiveMQPublisher publisher = new ActiveMQPublisher(brokerUrl, topicName);

        // 4. Orquestar el controlador (El controlador no cambia, recibe la interfaz)
        ScraperController controller = new ScraperController(
                feeder,
                sanitizer,
                analyzer,
                mapper,
                publisher,
                sourceId
        );

        // 5. Apagado seguro de los recursos
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nApagando sistema de captura...");
            controller.stop();
            try {
                publisher.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));

        controller.startPeriodicCapture();
    }
}