// Archivo: NewsSerializer.java
package es.ulpgc.datos.serializer;

import es.ulpgc.datos.model.NewsArticle;
import java.util.List;

public interface NewsSerializer {
    void save(List<NewsArticle> newsList);
}