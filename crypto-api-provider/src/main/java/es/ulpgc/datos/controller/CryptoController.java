package es.ulpgc.datos.controller;

import es.ulpgc.datos.event.CryptoPriceEvent;
import es.ulpgc.datos.feeder.CryptoFeeder;
import es.ulpgc.datos.model.CryptoPrice;
import es.ulpgc.datos.mapper.CryptoPriceEventMapper;
import es.ulpgc.datos.publisher.ActiveMqEventPublisher;
import es.ulpgc.datos.serializer.CryptoPriceSerializer;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CryptoController {
    private final CryptoFeeder feeder;
    private final CryptoPriceSerializer serializer;
    private final CryptoPriceEventMapper eventMapper;
    private final ActiveMqEventPublisher publisher;
    private final ScheduledExecutorService scheduler;

    public CryptoController(CryptoFeeder feeder,
                            CryptoPriceSerializer serializer,
                            CryptoPriceEventMapper eventMapper,
                            ActiveMqEventPublisher publisher) {
        this.feeder = feeder;
        this.serializer = serializer;
        this.eventMapper = eventMapper;
        this.publisher = publisher;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void runOnce() {
        List<CryptoPrice> prices = feeder.fetchPrices();

        if (prices.isEmpty()) {
            System.out.println("No crypto prices fetched.");
            return;
        }

        serializer.save(prices);

        for (CryptoPrice price : prices) {
            CryptoPriceEvent event = eventMapper.toEvent(price);
            publisher.publish(event);
        }

        System.out.println("Saved and published " + prices.size() + " crypto prices.");
    }

    public void startPeriodicCapture() {
        runOnce();
        scheduler.scheduleAtFixedRate(this::runOnce, 1, 1, TimeUnit.HOURS);
    }

    public void stop() {
        scheduler.shutdown();
        publisher.close();
    }
}