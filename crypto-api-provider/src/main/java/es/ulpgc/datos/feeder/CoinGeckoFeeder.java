package es.ulpgc.datos.feeder;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.ulpgc.datos.mapper.CryptoPriceMapper;
import es.ulpgc.datos.model.CryptoPrice;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;

public class CoinGeckoFeeder implements CryptoFeeder {
    private static final String API_URL =
            "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&ids=bitcoin,ethereum&sparkline=false";

    private final HttpClient httpClient;
    private final CryptoPriceMapper mapper;

    public CoinGeckoFeeder() {
        this(HttpClient.newHttpClient(), new CryptoPriceMapper(new ObjectMapper()));
    }

    public CoinGeckoFeeder(HttpClient httpClient, CryptoPriceMapper mapper) {
        this.httpClient = httpClient;
        this.mapper = mapper;
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
        return HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Accept", "application/json")
                .GET()
                .build();
    }
}