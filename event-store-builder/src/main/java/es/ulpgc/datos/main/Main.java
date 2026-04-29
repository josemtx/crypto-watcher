package es.ulpgc.datos.main;

import es.ulpgc.datos.store.EventStoreWriter;
import es.ulpgc.datos.subscriber.EventStoreSubscriber;

public class Main {
    public static void main(String[] args) {
        EventStoreWriter writer = new EventStoreWriter();
        EventStoreSubscriber subscriber = new EventStoreSubscriber(writer);

        subscriber.start();

        Runtime.getRuntime().addShutdownHook(new Thread(subscriber::close));
    }
}