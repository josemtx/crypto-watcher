package es.ulpgc.datos.application;

import es.ulpgc.datos.domain.CryptoPrice;
import es.ulpgc.datos.domain.CryptoPriceEvent;
import es.ulpgc.datos.domain.CryptoPriceEventMapper;
import es.ulpgc.datos.infrastructure.ActiveMqEventPublisher;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CryptoController {
    private final CryptoFeeder feeder;
    private final CryptoPriceStore store;
    private final CryptoPriceEventMapper eventMapper;
    private final ActiveMqEventPublisher publisher;
    private final ScheduledExecutorService scheduler;
    private final long capturePeriod;
    private final TimeUnit captureTimeUnit;

    public CryptoController(CryptoFeeder feeder,
                            CryptoPriceStore store,
                            CryptoPriceEventMapper eventMapper,
                            ActiveMqEventPublisher publisher,
                            long capturePeriod,
                            TimeUnit captureTimeUnit) {
        this.feeder = feeder;
        this.store = store;
        this.eventMapper = eventMapper;
        this.publisher = publisher;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.capturePeriod = capturePeriod;
        this.captureTimeUnit = captureTimeUnit;
    }

    public void runOnce() {
        List<CryptoPrice> prices = feeder.fetchPrices();

        if (prices.isEmpty()) {
            System.out.println("No crypto prices fetched.");
            return;
        }

        System.out.println("Fetched " + prices.size() + " crypto prices.");
        store.save(prices);
        System.out.println("Saved " + prices.size() + " crypto prices to SQLite.");

        int publishedEvents = 0;
        for (CryptoPrice price : prices) {
            CryptoPriceEvent event = eventMapper.toEvent(price);
            publisher.publish(event);
            publishedEvents++;
        }

        System.out.println("Published " + publishedEvents + " crypto price events.");
    }

    public void startPeriodicCapture() {
        System.out.println("Starting periodic capture every " + capturePeriod + " " + captureTimeUnit + ".");
        runOnce();
        scheduler.scheduleAtFixedRate(this::runOnce, capturePeriod, capturePeriod, captureTimeUnit);
    }

    public void stop() {
        scheduler.shutdown();
        publisher.close();
    }
}