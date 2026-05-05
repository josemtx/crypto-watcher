// Archivo: ActiveMqConfig.java
package es.ulpgc.datos.config;

public final class ActiveMqConfig {
    public static final String BROKER_URL = "tcp://localhost:61616";
    public static final String TOPIC_NAME = "CryptoNews";
    public static final String SOURCE_ID = "CoinDeskScraper";

    private ActiveMqConfig() {
        // Constructor privado para evitar que la clase sea instanciada
    }
}