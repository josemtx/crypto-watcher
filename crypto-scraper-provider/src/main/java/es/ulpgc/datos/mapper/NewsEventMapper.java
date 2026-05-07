package es.ulpgc.datos.mapper;

import es.ulpgc.datos.analyzer.SentimentAnalyzer.SentimentResult;
import es.ulpgc.datos.event.CryptoNewsEvent;
import es.ulpgc.datos.model.NewsArticle;

public class NewsEventMapper {

    public CryptoNewsEvent toEvent(NewsArticle article, String sourceId, SentimentResult sentiment) {
        return new CryptoNewsEvent(
                article.capturedAt().toString(),
                sourceId,
                article.title(),
                article.url(),
                sentiment.score(),
                sentiment.label()
        );
    }
}