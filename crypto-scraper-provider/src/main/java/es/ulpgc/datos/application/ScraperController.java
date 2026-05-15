package es.ulpgc.datos.application;

import es.ulpgc.datos.domain.SentimentAnalyzer;
import es.ulpgc.datos.domain.SentimentAnalyzer.SentimentResult;
import es.ulpgc.datos.domain.CryptoNewsEvent;
import es.ulpgc.datos.domain.NewsFeeder;
import es.ulpgc.datos.domain.NewsEventMapper;
import es.ulpgc.datos.domain.NewsArticle;
import es.ulpgc.datos.infrastructure.ActiveMQPublisher;
import es.ulpgc.datos.domain.ArticleSanitizer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScraperController {
    private final NewsFeeder feeder;
    private final ArticleSanitizer sanitizer;
    private final SentimentAnalyzer analyzer;
    private final NewsEventMapper eventMapper;
    private final ActiveMQPublisher publisher;
    private final ScheduledExecutorService scheduler;
    private final String sourceId;

    // Constructor actualizado con todas las inyecciones
    public ScraperController(NewsFeeder feeder, ArticleSanitizer sanitizer, SentimentAnalyzer analyzer,
                             NewsEventMapper eventMapper, ActiveMQPublisher publisher, String sourceId) {
        this.feeder = feeder;
        this.sanitizer = sanitizer;
        this.analyzer = analyzer;
        this.eventMapper = eventMapper;
        this.publisher = publisher;
        this.sourceId = sourceId;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void runOnce() {
        System.out.println("\n[" + LocalDateTime.now() + "] Ejecutando captura de noticias...");
        List<NewsArticle> rawArticles = feeder.fetchNews();

        // 1. Limpiar basura
        List<NewsArticle> cleanArticles = sanitizer.sanitize(rawArticles);

        if (cleanArticles.isEmpty()) {
            System.out.println("No hay noticias nuevas válidas en esta iteración.");
            return;
        }

        int publishedCount = 0;
        for (NewsArticle article : cleanArticles) {
            // 2. Analizar sentimiento del título
            SentimentResult sentiment = analyzer.analyze(article.title());

            // 3. Mapear con todos los datos
            CryptoNewsEvent event = eventMapper.toEvent(article, sourceId, sentiment);

            // 4. Publicar
            publisher.publish(event);
            publishedCount++;
        }

        System.out.println("Éxito: Se han procesado y publicado " + publishedCount + " noticias.");
    }

    public void startPeriodicCapture() {
        runOnce();
        scheduler.scheduleAtFixedRate(this::runOnce, 1, 1, TimeUnit.HOURS);
    }

    public void stop() {
        scheduler.shutdown();
    }
}