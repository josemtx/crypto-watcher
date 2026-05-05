// Archivo: NewsFeeder.java
package es.ulpgc.datos.feeder;

import es.ulpgc.datos.model.NewsArticle;
import java.util.List;

public interface NewsFeeder {
    List<NewsArticle> fetchNews();
}