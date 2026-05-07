package es.ulpgc.datos;

import es.ulpgc.datos.controller.CryptoController;
import es.ulpgc.datos.feeder.CoinGeckoFeeder;
import es.ulpgc.datos.feeder.CryptoFeeder;
import es.ulpgc.datos.mapper.CryptoPriceEventMapper;
import es.ulpgc.datos.controller.ActiveMqEventPublisher;
import es.ulpgc.datos.controller.serializer.CryptoPriceSerializer;
import es.ulpgc.datos.controller.serializer.DatabaseCryptoPriceSerializer;

public class Main {
    public static void main(String[] args) {
        CryptoFeeder feeder = new CoinGeckoFeeder();
        CryptoPriceSerializer serializer = new DatabaseCryptoPriceSerializer();
        CryptoPriceEventMapper eventMapper = new CryptoPriceEventMapper();
        ActiveMqEventPublisher publisher = new ActiveMqEventPublisher();

        CryptoController controller = new CryptoController(
                feeder,
                serializer,
                eventMapper,
                publisher
        );

        controller.startPeriodicCapture();
    }
}