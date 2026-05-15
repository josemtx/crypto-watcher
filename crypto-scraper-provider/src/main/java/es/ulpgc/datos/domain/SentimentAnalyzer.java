package es.ulpgc.datos.domain;

public interface SentimentAnalyzer {
    SentimentResult analyze(String text);

    // Usamos un Record interno para devolver la puntuación y la etiqueta
    record SentimentResult(double score, String label) {}
}