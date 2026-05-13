package es.ulpgc.datos.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.ulpgc.datos.domain.CryptoFeeder;
import es.ulpgc.datos.domain.CryptoPrice;
import es.ulpgc.datos.domain.CryptoPriceMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CoinGeckoFeeder implements CryptoFeeder {
    private static final String BASE_URL = "https://api.coingecko.com/api/v3/coins/markets";
    private static final String DEFAULT_VS_CURRENCY = "usd";
    private static final String DEFAULT_COIN_IDS = "bitcoin,ethereum";

    private final HttpClient httpClient;
    private final CryptoPriceMapper mapper;
    private final String vsCurrency;
    private final String coinIds;

    public CoinGeckoFeeder() {
        this(HttpClient.newHttpClient(), DEFAULT_VS_CURRENCY, DEFAULT_COIN_IDS);
    }

    public CoinGeckoFeeder(String vsCurrency, String coinIds) {
        this(HttpClient.newHttpClient(), vsCurrency, coinIds);
    }

    public CoinGeckoFeeder(HttpClient httpClient, String vsCurrency, String coinIds) {
        this.httpClient = httpClient;
        this.vsCurrency = normalizeVsCurrency(vsCurrency);
        this.coinIds = normalizeCoinIds(coinIds);
        this.mapper = new CryptoPriceMapper(new ObjectMapper(), this.vsCurrency);
    }

    @Override
    public List<CryptoPrice> fetchPrices() {
        try {
            HttpRequest request = buildRequest();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("Error calling CoinGecko API. Status: " + response.statusCode());
                return List.of();
            }

            return mapper.fromJson(response.body(), Instant.now());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("CoinGecko request was interrupted.");
            return List.of();
        } catch (IOException e) {
            System.err.println("Error reading CoinGecko response: " + e.getMessage());
            return List.of();
        }
    }

    private HttpRequest buildRequest() {
        String apiUrl = buildApiUrl();
        System.out.println("CoinGecko request URL: " + apiUrl);

        return HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Accept", "application/json")
                .GET()
                .build();
    }

    private String buildApiUrl() {
        return BASE_URL
                + "?vs_currency=" + encode(vsCurrency)
                + "&ids=" + encode(coinIds)
                + "&sparkline=false";
    }

    private String normalizeVsCurrency(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_VS_CURRENCY;
        }
        return value.trim().toLowerCase();
    }

    private String normalizeCoinIds(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_COIN_IDS;
        }

        String normalized = Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .map(String::toLowerCase)
                .distinct()
                .collect(Collectors.joining(","));

        if (normalized.isBlank()) {
            return DEFAULT_COIN_IDS;
        }

        return normalized;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}