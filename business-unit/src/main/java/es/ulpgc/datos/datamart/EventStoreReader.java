package es.ulpgc.datos.datamart;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import es.ulpgc.datos.subscriber.EventProcessor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class EventStoreReader {
    private final String baseEventStoreDir;

    public EventStoreReader(String baseEventStoreDir) {
        this.baseEventStoreDir = baseEventStoreDir;
    }

    // Un record interno para ayudar a ordenar los eventos
    private record TimedEvent(String ts, String rawJson) {}

    public void restoreDatamart(EventProcessor processor) {
        System.out.println("[Business-Unit] Iniciando reconstrucción desde el Event Store...");
        List<TimedEvent> allEvents = new ArrayList<>();

        Path basePath = Paths.get(baseEventStoreDir);
        if (!Files.exists(basePath)) {
            System.out.println("[Business-Unit] Aviso: El directorio Event Store no existe aún. Omitiendo reconstrucción.");
            return;
        }

        try (Stream<Path> paths = Files.walk(basePath)) {
            // 1. Buscar todos los archivos .events en cualquier subcarpeta
            List<Path> eventFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".events"))
                    .toList();

            // 2. Leer todas las líneas y extraer el timestamp para poder ordenar
            for (Path file : eventFiles) {
                List<String> lines = Files.readAllLines(file);
                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;
                    try {
                        JsonObject json = JsonParser.parseString(line).getAsJsonObject();
                        String ts = json.get("ts").getAsString();
                        allEvents.add(new TimedEvent(ts, line));
                    } catch (Exception e) {
                        System.err.println("Fallo al parsear línea histórica: " + line);
                    }
                }
            }

            // 3. ¡La Magia! Ordenar todos los eventos de todas las fuentes por fecha y hora exactas
            allEvents.sort(Comparator.comparing(TimedEvent::ts));

            System.out.println("[Business-Unit] Se han encontrado y ordenado " + allEvents.size() + " eventos históricos.");

            // 4. Enviar los eventos al Datamart en orden estricto (Viaje en el tiempo)
            for (TimedEvent event : allEvents) {
                processor.processEvent(event.rawJson());
            }

            System.out.println("[Business-Unit] Reconstrucción histórica completada con éxito.");

        } catch (IOException e) {
            System.err.println("Error crítico leyendo el Event Store: " + e.getMessage());
        }
    }
}