package es.ulpgc.datos.util;

import es.ulpgc.datos.model.NewsArticle;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

public class ArticleSanitizer {

    // Un Set para recordar las URLs que ya hemos procesado y no repetir
    private final Set<String> processedUrls = ConcurrentHashMap.newKeySet();

    public List<NewsArticle> sanitize(List<NewsArticle> rawArticles) {
        return rawArticles.stream()
                // 1. Filtrar basura (títulos muy cortos o URLs genéricas)
                .filter(a -> a.title().split(" ").length > 4)
                .filter(a -> a.url().matches(".*\\d{4}/\\d{2}/\\d{2}.*")) // Obliga a que la URL tenga una fecha YYYY/MM/DD
                // 2. Filtrar duplicados en la misma iteración
                .distinct()
                // 3. Filtrar noticias que ya procesamos en iteraciones anteriores
                .filter(a -> processedUrls.add(a.url()))
                .collect(Collectors.toList());
    }
}