package es.ulpgc.datos.domain;

public record CryptoPriceEvent(
        String ts,
        String ss,
        String coinId,
        String symbol,
        String name,
        String vsCurrency,
        double price,
        Double marketCap,
        Double volume24h
) {
}