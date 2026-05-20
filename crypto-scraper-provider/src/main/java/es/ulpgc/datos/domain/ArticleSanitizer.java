package es.ulpgc.datos.domain;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

public class ArticleSanitizer {

    private final Set<String> processedUrls = ConcurrentHashMap.newKeySet();

    public List<NewsArticle> sanitize(List<NewsArticle> rawArticles) {
        return rawArticles.stream()
                .filter(a -> a.title().split(" ").length > 4)
                .filter(a -> a.url().matches(".*\\d{4}/\\d{2}/\\d{2}.*")) // Obliga a que la URL tenga una fecha YYYY/MM/DD
                .distinct()
                .filter(a -> processedUrls.add(a.url()))
                .collect(Collectors.toList());
    }
}