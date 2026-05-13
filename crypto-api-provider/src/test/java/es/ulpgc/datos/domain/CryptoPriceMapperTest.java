package es.ulpgc.datos.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CryptoPriceMapperTest {

    @Test
    void shouldMapJsonToCryptoPriceList() throws Exception {
        String json = """
                [
                  {
                    "id": "bitcoin",
                    "symbol": "btc",
                    "name": "Bitcoin",
                    "current_price": 80473.0,
                    "market_cap": 1.61260018592E12,
                    "total_volume": 3.9957536844E10
                  },
                  {
                    "id": "ethereum",
                    "symbol": "eth",
                    "name": "Ethereum",
                    "current_price": 2301.08,
                    "market_cap": 2.77712941742E11,
                    "total_volume": 1.3081514137E10
                  }
                ]
                """;

        Instant capturedAt = Instant.parse("2026-05-13T12:00:52.975351300Z");
        CryptoPriceMapper mapper = new CryptoPriceMapper(new ObjectMapper(), "usd");

        List<CryptoPrice> prices = mapper.fromJson(json, capturedAt);

        assertEquals(2, prices.size());

        CryptoPrice bitcoin = prices.get(0);
        assertEquals("bitcoin", bitcoin.coinId());
        assertEquals("btc", bitcoin.symbol());
        assertEquals("Bitcoin", bitcoin.name());
        assertEquals("usd", bitcoin.vsCurrency());
        assertEquals(80473.0, bitcoin.price());
        assertEquals(1.61260018592E12, bitcoin.marketCap());
        assertEquals(3.9957536844E10, bitcoin.volume24h());
        assertEquals(capturedAt, bitcoin.capturedAt());

        CryptoPrice ethereum = prices.get(1);
        assertEquals("ethereum", ethereum.coinId());
        assertEquals("eth", ethereum.symbol());
        assertEquals("Ethereum", ethereum.name());
        assertEquals("usd", ethereum.vsCurrency());
        assertEquals(2301.08, ethereum.price());
        assertEquals(2.77712941742E11, ethereum.marketCap());
        assertEquals(1.3081514137E10, ethereum.volume24h());
        assertEquals(capturedAt, ethereum.capturedAt());

        assertNotNull(prices);
    }
}