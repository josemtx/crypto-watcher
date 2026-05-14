package es.ulpgc.datos;

import es.ulpgc.datos.analyzer.ApiNinjasAnalyzer;
import es.ulpgc.datos.analyzer.SentimentAnalyzer;
import es.ulpgc.datos.controller.ScraperController;
import es.ulpgc.datos.feeder.CoinDeskFeeder;
import es.ulpgc.datos.feeder.NewsFeeder;
import es.ulpgc.datos.mapper.NewsEventMapper;
import es.ulpgc.datos.publisher.ActiveMQPublisher;
import es.ulpgc.datos.util.ArticleSanitizer;

public class Main {
    private static final String API_KEY_ENV_VAR = "API_NINJAS_KEY";

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Error: Faltan argumentos de configuración.");
            System.err.println("Uso esperado: java -jar app.jar <broker_url> <topic_name> <source_id>");
            System.err.println("La API key debe venir en la variable de entorno: " + API_KEY_ENV_VAR);
            System.err.println("Ejemplo: tcp://localhost:61616 CryptoNews CoinDeskScraper");
            System.exit(1);
        }

        String brokerUrl = args[0];
        String topicName = args[1];
        String sourceId = args[2];
        String apiKey = System.getenv(API_KEY_ENV_VAR);

        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("Error: No se ha encontrado la API key en la variable de entorno " + API_KEY_ENV_VAR);
            System.exit(1);
        }

        System.out.println("Inicializando Crypto Scraper Provider...");
        System.out.println("  Broker URL : " + brokerUrl);
        System.out.println("  Topic      : " + topicName);
        System.out.println("  Source ID  : " + sourceId);
        System.out.println("  API key    : cargada desde variable de entorno");

        NewsFeeder feeder = new CoinDeskFeeder();
        ArticleSanitizer sanitizer = new ArticleSanitizer();
        SentimentAnalyzer analyzer = new ApiNinjasAnalyzer(apiKey);
        NewsEventMapper mapper = new NewsEventMapper();
        ActiveMQPublisher publisher = new ActiveMQPublisher(brokerUrl, topicName);

        ScraperController controller = new ScraperController(
                feeder,
                sanitizer,
                analyzer,
                mapper,
                publisher,
                sourceId
        );

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