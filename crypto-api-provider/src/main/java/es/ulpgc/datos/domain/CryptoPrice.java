package es.ulpgc.datos.domain;

import java.time.Instant;

public record CryptoPrice(
        String coinId,
        String symbol,
        String name,
        String vsCurrency,
        double price,
        Double marketCap,
        Double volume24h,
        Instant capturedAt
) {
}