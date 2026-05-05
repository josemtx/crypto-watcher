// Archivo: Main.java
package es.ulpgc.datos.main;

import es.ulpgc.datos.controller.ScraperController;
import es.ulpgc.datos.feeder.CoinDeskFeeder;
import es.ulpgc.datos.feeder.NewsFeeder;
import es.ulpgc.datos.mapper.NewsEventMapper;
import es.ulpgc.datos.publisher.ActiveMQPublisher;
import es.ulpgc.datos.serializer.DatabaseNewsSerializer;
import es.ulpgc.datos.serializer.NewsSerializer;

public class Main {
    public static void main(String[] args) {
        System.out.println("Inicializando Crypto Scraper Provider...");

        // 1. Instanciar las herramientas concretas
        NewsFeeder feeder = new CoinDeskFeeder();
        NewsSerializer serializer = new DatabaseNewsSerializer();
        NewsEventMapper mapper = new NewsEventMapper();
        ActiveMQPublisher publisher = new ActiveMQPublisher();

        // 2. Inyectarlas en el orquestador
        ScraperController controller = new ScraperController(
                feeder,
                serializer,
                mapper,
                publisher
        );

        // 3. Garantizar un apagado seguro
        Runtime.getRuntime().addShutdownHook(new Thread(controller::stop));

        // 4. Arrancar el motor
        controller.startPeriodicCapture();
    }
}