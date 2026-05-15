package es.ulpgc.datos.domain;

import es.ulpgc.datos.domain.SentimentAnalyzer.SentimentResult;

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