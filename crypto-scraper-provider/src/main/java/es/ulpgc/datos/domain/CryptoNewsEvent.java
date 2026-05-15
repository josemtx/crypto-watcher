package es.ulpgc.datos.domain;

public record CryptoNewsEvent(
        String ts,
        String ss,
        String title,
        String url,
        double sentimentScore,
        String sentimentLabel
) {
}