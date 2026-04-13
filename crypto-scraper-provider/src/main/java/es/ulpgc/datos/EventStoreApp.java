package es.ulpgc.datos;

import es.ulpgc.datos.subscriber.ActiveMQSubscriber;

public class EventStoreApp {
    public static void main(String[] args) {
        System.out.println("Iniciando Event Store Builder...");
        ActiveMQSubscriber subscriber = new ActiveMQSubscriber();

        // Nos suscribimos al Topic de noticias de tu módulo
        subscriber.startListening("CryptoNews");

        // Cuando tu compañero termine su parte, bastará con añadir otra línea aquí:
        // subscriber.startListening("CryptoPrices");
    }
}