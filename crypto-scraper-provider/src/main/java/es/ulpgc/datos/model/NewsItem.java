package es.ulpgc.datos.model;

import java.time.LocalDateTime;

public record NewsItem(String title, String url, LocalDateTime capturedAt) {}