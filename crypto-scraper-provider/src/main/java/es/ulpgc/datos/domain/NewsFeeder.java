package es.ulpgc.datos.domain;

import java.util.List;

public interface NewsFeeder {
    List<NewsArticle> fetchNews();
}