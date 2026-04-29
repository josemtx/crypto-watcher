package es.ulpgc.datos.mapper;

import es.ulpgc.datos.event.CryptoPriceEvent;
import es.ulpgc.datos.model.CryptoPrice;

public class CryptoPriceEventMapper {
    private static final String SOURCE = "crypto-api-provider";

    public CryptoPriceEvent toEvent(CryptoPrice price) {
        return new CryptoPriceEvent(
                price.capturedAt().toString(),
                SOURCE,
                price.coinId(),
                price.symbol(),
                price.name(),
                price.priceUsd(),
                price.marketCap(),
                price.volume24h()
        );
    }
}