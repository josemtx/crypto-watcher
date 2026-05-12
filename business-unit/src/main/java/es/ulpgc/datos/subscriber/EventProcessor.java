package es.ulpgc.datos.subscriber;

public interface EventProcessor {
    void processEvent(String json);
}