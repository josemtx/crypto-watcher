package es.ulpgc.datos;

import es.ulpgc.datos.application.CryptoController;
import es.ulpgc.datos.application.CryptoFeeder;
import es.ulpgc.datos.application.CryptoPriceStore;
import es.ulpgc.datos.domain.CryptoPriceEventMapper;
import es.ulpgc.datos.infrastructure.ActiveMqEventPublisher;
import es.ulpgc.datos.infrastructure.CoinGeckoFeeder;
import es.ulpgc.datos.infrastructure.DatabaseCryptoPriceStore;

import java.util.concurrent.TimeUnit;

public class Main {
    private static final String DEFAULT_BROKER_URL = "tcp://localhost:61616";
    private static final String DEFAULT_TOPIC_NAME = "CryptoPrice";
    private static final String DEFAULT_SOURCE_ID = "crypto-api-provider";
    private static final String DEFAULT_VS_CURRENCY = "usd";
    private static final String DEFAULT_COIN_IDS = "bitcoin,ethereum";
    private static final long DEFAULT_CAPTURE_PERIOD = 1L;
    private static final TimeUnit DEFAULT_CAPTURE_TIME_UNIT = TimeUnit.HOURS;
    private static final String DEFAULT_DATABASE_PATH = "crypto_data.db";

    public static void main(String[] args) {
        String brokerUrl = getArgumentOrDefault(args, 0, DEFAULT_BROKER_URL);
        String topicName = getArgumentOrDefault(args, 1, DEFAULT_TOPIC_NAME);
        String sourceId = getArgumentOrDefault(args, 2, DEFAULT_SOURCE_ID);
        String vsCurrency = getArgumentOrDefault(args, 3, DEFAULT_VS_CURRENCY);
        String coinIds = getArgumentOrDefault(args, 4, DEFAULT_COIN_IDS);
        long capturePeriod = getLongArgumentOrDefault(args, 5, DEFAULT_CAPTURE_PERIOD);
        TimeUnit captureTimeUnit = getTimeUnitArgumentOrDefault(args, 6, DEFAULT_CAPTURE_TIME_UNIT);
        String databasePath = getArgumentOrDefault(args, 7, DEFAULT_DATABASE_PATH);

        printConfiguration(
                brokerUrl,
                topicName,
                sourceId,
                vsCurrency,
                coinIds,
                capturePeriod,
                captureTimeUnit,
                databasePath
        );

        CryptoFeeder feeder = new CoinGeckoFeeder(vsCurrency, coinIds);
        CryptoPriceStore store = new DatabaseCryptoPriceStore(databasePath);
        CryptoPriceEventMapper eventMapper = new CryptoPriceEventMapper(sourceId);
        ActiveMqEventPublisher publisher = new ActiveMqEventPublisher(brokerUrl, topicName);

        CryptoController controller = new CryptoController(
                feeder,
                store,
                eventMapper,
                publisher,
                capturePeriod,
                captureTimeUnit
        );

        controller.startPeriodicCapture();
    }

    private static void printConfiguration(String brokerUrl,
                                           String topicName,
                                           String sourceId,
                                           String vsCurrency,
                                           String coinIds,
                                           long capturePeriod,
                                           TimeUnit captureTimeUnit,
                                           String databasePath) {
        System.out.println("Starting crypto-api-provider with configuration:");
        System.out.println("  Broker URL     : " + brokerUrl);
        System.out.println("  Topic          : " + topicName);
        System.out.println("  Source ID      : " + sourceId);
        System.out.println("  VS Currency    : " + vsCurrency);
        System.out.println("  Coin IDs       : " + coinIds);
        System.out.println("  Capture Period : " + capturePeriod + " " + captureTimeUnit);
        System.out.println("  Database Path  : " + databasePath);
    }

    private static String getArgumentOrDefault(String[] args, int index, String defaultValue) {
        if (args.length > index && args[index] != null && !args[index].isBlank()) {
            return args[index];
        }
        return defaultValue;
    }

    private static long getLongArgumentOrDefault(String[] args, int index, long defaultValue) {
        if (args.length <= index || args[index] == null || args[index].isBlank()) {
            return defaultValue;
        }

        try {
            return Long.parseLong(args[index].trim());
        } catch (NumberFormatException e) {
            System.err.println("Invalid numeric argument at index " + index + ". Using default value: " + defaultValue);
            return defaultValue;
        }
    }

    private static TimeUnit getTimeUnitArgumentOrDefault(String[] args, int index, TimeUnit defaultValue) {
        if (args.length <= index || args[index] == null || args[index].isBlank()) {
            return defaultValue;
        }

        try {
            return TimeUnit.valueOf(args[index].trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid time unit at index " + index + ". Using default value: " + defaultValue);
            return defaultValue;
        }
    }
}