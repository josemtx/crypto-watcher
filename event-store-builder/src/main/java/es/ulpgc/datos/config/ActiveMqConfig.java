package es.ulpgc.datos.config;

public final class ActiveMqConfig {
    public static final String BROKER_URL = "tcp://localhost:61616";
    public static final String[] TOPICS = {"CryptoPrice", "CryptoNews"};
    public static final String CLIENT_ID = "event-store-builder-client";
    public static final String DURABLE_SUBSCRIPTION_NAME = "event-store-builder-subscription";

    private ActiveMqConfig() {
    }
}