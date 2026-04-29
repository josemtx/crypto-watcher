package es.ulpgc.datos.config;

public final class ActiveMqConfig {
    public static final String BROKER_URL = "tcp://localhost:61616";
    public static final String TOPIC_NAME = "CryptoPrice";
    public static final String SOURCE_ID = "crypto-api-provider";

    private ActiveMqConfig() {
    }
}