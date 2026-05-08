package es.ulpgc.datos.domain;

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