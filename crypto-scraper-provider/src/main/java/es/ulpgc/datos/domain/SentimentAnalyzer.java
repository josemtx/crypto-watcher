package es.ulpgc.datos.domain;

public interface SentimentAnalyzer {
    SentimentResult analyze(String text);
    record SentimentResult(double score, String label) {}
}