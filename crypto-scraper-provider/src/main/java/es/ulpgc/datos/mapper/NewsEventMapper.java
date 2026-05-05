// Archivo: NewsEventMapper.java
package es.ulpgc.datos.mapper;

import es.ulpgc.datos.config.ActiveMqConfig;
import es.ulpgc.datos.event.CryptoNewsEvent;
import es.ulpgc.datos.model.NewsArticle;

public class NewsEventMapper {
    public CryptoNewsEvent toEvent(NewsArticle article) {
        return new CryptoNewsEvent(
                article.capturedAt().toString(),
                ActiveMqConfig.SOURCE_ID,
                article.title(),
                article.url()
        );
    }
}