package es.ulpgc.datos.domain;

public class CryptoPriceEventMapper {
    private final String sourceId;

    public CryptoPriceEventMapper(String sourceId) {
        this.sourceId = sourceId;
    }

    public CryptoPriceEvent toEvent(CryptoPrice price) {
        return new CryptoPriceEvent(
                price.capturedAt().toString(),
                sourceId,
                price.coinId(),
                price.symbol(),
                price.name(),
                price.priceUsd(),
                price.marketCap(),
                price.volume24h()
        );
    }
}