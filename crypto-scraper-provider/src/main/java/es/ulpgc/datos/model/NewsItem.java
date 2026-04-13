package es.ulpgc.datos.model;

import java.time.Instant;

public record NewsItem(String ts, String ss, String title, String url) {
    // Constructor de conveniencia para autocompletar ts y ss
    public NewsItem(String title, String url) {
        // Instant.now() genera el formato UTC requerido (ej: 2026-04-13T17:30:00Z) [cite: 884]
        this(Instant.now().toString(), "CoinDeskScraper", title, url);
    }
}