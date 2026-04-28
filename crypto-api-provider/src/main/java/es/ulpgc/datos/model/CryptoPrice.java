package es.ulpgc.datos.model;

import java.time.Instant;

public record CryptoPrice(
        String coinId,
        String symbol,
        String name,
        double priceUsd,
        Double marketCap,
        Double volume24h,
        Instant capturedAt
) {
}