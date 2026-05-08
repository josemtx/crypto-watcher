package es.ulpgc.datos;

import es.ulpgc.datos.application.CryptoController;
import es.ulpgc.datos.domain.CryptoFeeder;
import es.ulpgc.datos.domain.CryptoPriceEventMapper;
import es.ulpgc.datos.infrastructure.ActiveMqEventPublisher;
import es.ulpgc.datos.infrastructure.CoinGeckoFeeder;
import es.ulpgc.datos.infrastructure.CryptoPriceSerializer;
import es.ulpgc.datos.infrastructure.DatabaseCryptoPriceSerializer;

public class Main {
    private static final String DEFAULT_BROKER_URL = "tcp://localhost:61616";
    private static final String DEFAULT_TOPIC_NAME = "CryptoPrice";
    private static final String DEFAULT_SOURCE_ID = "crypto-api-provider";

    public static void main(String[] args) {
        String brokerUrl = getArgumentOrDefault(args, 0, DEFAULT_BROKER_URL);
        String topicName = getArgumentOrDefault(args, 1, DEFAULT_TOPIC_NAME);
        String sourceId = getArgumentOrDefault(args, 2, DEFAULT_SOURCE_ID);

        CryptoFeeder feeder = new CoinGeckoFeeder();
        CryptoPriceSerializer serializer = new DatabaseCryptoPriceSerializer();
        CryptoPriceEventMapper eventMapper = new CryptoPriceEventMapper(sourceId);
        ActiveMqEventPublisher publisher = new ActiveMqEventPublisher(brokerUrl, topicName);

        CryptoController controller = new CryptoController(
                feeder,
                serializer,
                eventMapper,
                publisher
        );

        controller.startPeriodicCapture();
    }

    private static String getArgumentOrDefault(String[] args, int index, String defaultValue) {
        if (args.length > index && args[index] != null && !args[index].isBlank()) {
            return args[index];
        }
        return defaultValue;
    }
}