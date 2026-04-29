package es.ulpgc.datos.event;

public record CryptoPriceEvent(
        String ts,
        String ss,
        String coinId,
        String symbol,
        String name,
        double priceUsd,
        Double marketCap,
        Double volume24h
) {
}