package es.ulpgc.datos.store;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class EventStoreWriter {
    private static final String BASE_DIRECTORY = "eventstore";
    private static final DateTimeFormatter FILE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    private final ObjectMapper objectMapper;

    public EventStoreWriter() {
        this.objectMapper = new ObjectMapper();
    }

    public void append(String topic, String eventJson) {
        try {
            JsonNode event = objectMapper.readTree(eventJson);

            String ts = event.get("ts").asText();
            String ss = event.get("ss").asText();

            String fileName = buildFileName(ts);
            Path filePath = Path.of(BASE_DIRECTORY, topic, ss, fileName);

            Files.createDirectories(filePath.getParent());
            Files.writeString(
                    filePath,
                    eventJson + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

            System.out.println("Stored event in: " + filePath);
        } catch (IOException e) {
            System.err.println("Error writing event to store: " + e.getMessage());
        }
    }

    private String buildFileName(String ts) {
        Instant instant = Instant.parse(ts);
        return FILE_DATE_FORMAT.format(instant) + ".events";
    }
}