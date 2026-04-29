package es.ulpgc.datos.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.ulpgc.datos.model.CryptoPrice;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class CryptoPriceMapper {
    private final ObjectMapper objectMapper;

    public CryptoPriceMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<CryptoPrice> fromJson(String responseBody, Instant capturedAt) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        List<CryptoPrice> prices = new ArrayList<>();

        for (JsonNode coinNode : root) {
            prices.add(new CryptoPrice(
                    coinNode.get("id").asText(),
                    coinNode.get("symbol").asText(),
                    coinNode.get("name").asText(),
                    coinNode.get("current_price").asDouble(),
                    getNullableDouble(coinNode, "market_cap"),
                    getNullableDouble(coinNode, "total_volume"),
                    capturedAt
            ));
        }

        return prices;
    }

    private Double getNullableDouble(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return null;
        }
        return field.asDouble();
    }
}