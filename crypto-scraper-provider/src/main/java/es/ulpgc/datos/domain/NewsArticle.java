package es.ulpgc.datos.domain;

import java.time.Instant;

public record NewsArticle(
        String title,
        String url,
        Instant capturedAt
) {
}