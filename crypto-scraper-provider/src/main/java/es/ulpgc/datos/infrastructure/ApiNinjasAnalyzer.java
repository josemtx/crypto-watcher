package es.ulpgc.datos.infrastructure;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import es.ulpgc.datos.domain.SentimentAnalyzer;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class ApiNinjasAnalyzer implements SentimentAnalyzer {

    private final HttpClient client;
    private final String apiKey;
    private final String baseUrl = "https://api.api-ninjas.com/v1/sentiment?text=";

    public ApiNinjasAnalyzer(String apiKey) {
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.apiKey = apiKey;
    }

    @Override
    public SentimentResult analyze(String text) {
        try {
            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + encodedText))
                    .header("X-Api-Key", apiKey) // Aquí va tu llave
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("\n[!] Error HTTP " + response.statusCode() + " en API-Ninjas: " + response.body());
                return new SentimentResult(0.0, "NEUTRAL");
            }

            return parseResponse(response.body());

        } catch (Exception e) {
            System.err.println("\n[!] Fallo de red al contactar API-Ninjas: " + e.getMessage());
            return new SentimentResult(0.0, "NEUTRAL");
        }
    }

    private SentimentResult parseResponse(String jsonResponse) {
        try {
            JsonObject obj = JsonParser.parseString(jsonResponse).getAsJsonObject();

            double score = obj.has("score") ? obj.get("score").getAsDouble() : 0.0;
            String rawSentiment = obj.has("sentiment") ? obj.get("sentiment").getAsString().toUpperCase() : "NEUTRAL";

            String finalLabel = switch (rawSentiment) {
                case "WEAK_POSITIVE", "POSITIVE" -> "POSITIVE";
                case "WEAK_NEGATIVE", "NEGATIVE" -> "NEGATIVE";
                default -> "NEUTRAL";
            };

            return new SentimentResult(score, finalLabel);

        } catch (Exception e) {
            System.err.println("\n[!] Error parseando JSON de API-Ninjas: " + e.getMessage());
            return new SentimentResult(0.0, "NEUTRAL");
        }
    }
}