package es.ulpgc.datos.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CryptoPriceEventMapperTest {

    @Test
    void shouldMapCryptoPriceToCryptoPriceEvent() {
        CryptoPrice price = new CryptoPrice(
                "bitcoin",
                "btc",
                "Bitcoin",
                "usd",
                80473.0,
                1.61260018592E12,
                3.9957536844E10,
                Instant.parse("2026-05-13T12:00:52.975351300Z")
        );

        CryptoPriceEventMapper mapper = new CryptoPriceEventMapper("crypto-api-provider");

        CryptoPriceEvent event = mapper.toEvent(price);

        assertEquals("2026-05-13T12:00:52.975351300Z", event.ts());
        assertEquals("crypto-api-provider", event.ss());
        assertEquals("bitcoin", event.coinId());
        assertEquals("btc", event.symbol());
        assertEquals("Bitcoin", event.name());
        assertEquals("usd", event.vsCurrency());
        assertEquals(80473.0, event.price());
        assertEquals(1.61260018592E12, event.marketCap());
        assertEquals(3.9957536844E10, event.volume24h());
    }
}