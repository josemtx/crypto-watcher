// Archivo: ScraperController.java
package es.ulpgc.datos.controller;

import es.ulpgc.datos.event.CryptoNewsEvent;
import es.ulpgc.datos.feeder.NewsFeeder;
import es.ulpgc.datos.mapper.NewsEventMapper;
import es.ulpgc.datos.model.NewsArticle;
import es.ulpgc.datos.publisher.ActiveMQPublisher;
import es.ulpgc.datos.serializer.NewsSerializer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScraperController {
    private final NewsFeeder feeder;
    private final NewsSerializer serializer;
    private final NewsEventMapper eventMapper;
    private final ActiveMQPublisher publisher;
    private final ScheduledExecutorService scheduler;

    public ScraperController(NewsFeeder feeder, NewsSerializer serializer, NewsEventMapper eventMapper, ActiveMQPublisher publisher) {
        this.feeder = feeder;
        this.serializer = serializer;
        this.eventMapper = eventMapper;
        this.publisher = publisher;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void runOnce() {
        System.out.println("\n[" + LocalDateTime.now() + "] Ejecutando captura de noticias...");
        List<NewsArticle> articles = feeder.fetchNews();

        if (articles.isEmpty()) {
            System.out.println("No se han capturado noticias en esta iteración.");
            return;
        }

        // 1. Persistencia local
        serializer.save(articles);

        // 2. Publicación en la red
        int publishedCount = 0;
        for (NewsArticle article : articles) {
            CryptoNewsEvent event = eventMapper.toEvent(article);
            publisher.publish(event);
            publishedCount++;
        }

        System.out.println("Éxito: Se han procesado y publicado " + publishedCount + " noticias.");
    }

    public void startPeriodicCapture() {
        runOnce(); // Ejecución inmediata la primera vez
        scheduler.scheduleAtFixedRate(this::runOnce, 1, 1, TimeUnit.HOURS);
    }

    public void stop() {
        scheduler.shutdown();
        publisher.close(); // Cierre limpio de los recursos de red
    }
}