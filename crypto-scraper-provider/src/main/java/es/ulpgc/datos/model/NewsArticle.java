// Archivo: NewsArticle.java
package es.ulpgc.datos.model;

import java.time.Instant;

public record NewsArticle(
        String title,
        String url,
        Instant capturedAt
) {
}