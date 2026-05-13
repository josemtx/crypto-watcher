package es.ulpgc.datos.datamart;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import es.ulpgc.datos.subscriber.EventProcessor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class DatamartUpdater implements EventProcessor {
    private final String dbUrl;

    public DatamartUpdater(String dbPath) {
        this.dbUrl = "jdbc:sqlite:" + dbPath;
    }

    @Override
    public void processEvent(String json) {
        try {
            System.out.println("\n[Business-Unit] Guardando en SQLite -> " + json);
            JsonObject event = JsonParser.parseString(json).getAsJsonObject();
            String ts = event.get("ts").getAsString();
            String timeWindow = ts.substring(0, 13) + ":00";

            try (Connection conn = DriverManager.getConnection(dbUrl)) {
                if (event.has("price")) {
                    processPriceEvent(conn, event, timeWindow);
                } else if (event.has("sentimentLabel")) {
                    processNewsEvent(conn, event, timeWindow);
                }
            }
        } catch (Exception e) {
            System.err.println("Error en el procesador: " + e.getMessage());
        }
    }

    private void processPriceEvent(Connection conn, JsonObject event, String timeWindow) throws Exception {
        String coinId = event.get("coinId").getAsString();
        double price = event.get("price").getAsDouble();

        String sqlTimeline = """
            INSERT INTO crypto_timeline (time_window, coin_id, close_price, min_price, max_price)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(time_window, coin_id) DO UPDATE SET
                close_price = excluded.close_price,
                min_price = MIN(crypto_timeline.min_price, excluded.min_price),
                max_price = MAX(crypto_timeline.max_price, excluded.max_price);
        """;

        String sqlAlerts = """
            INSERT INTO market_hype_alerts (time_window, coin_id, is_high_volatility, hype_warning)
            VALUES (?, ?, 0, 0)
            ON CONFLICT(time_window, coin_id) DO UPDATE SET
                is_high_volatility = (
                    SELECT CASE WHEN ((max_price - min_price) / min_price) > 0.02 THEN 1 ELSE 0 END
                    FROM crypto_timeline WHERE time_window = excluded.time_window AND coin_id = excluded.coin_id
                ),
                hype_warning = (
                    SELECT CASE WHEN market_hype_alerts.news_volume > 5 AND ((max_price - min_price) / min_price) > 0.02 THEN 1 ELSE 0 END
                    FROM crypto_timeline WHERE time_window = excluded.time_window AND coin_id = excluded.coin_id
                );
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sqlTimeline)) {
            stmt.setString(1, timeWindow);
            stmt.setString(2, coinId);
            stmt.setDouble(3, price);
            stmt.setDouble(4, price);
            stmt.setDouble(5, price);
            stmt.executeUpdate();
        }

        try (PreparedStatement stmt = conn.prepareStatement(sqlAlerts)) {
            stmt.setString(1, timeWindow);
            stmt.setString(2, coinId);
            stmt.executeUpdate();
        }
    }

    private void processNewsEvent(Connection conn, JsonObject event, String timeWindow) throws Exception {
        String title = event.get("title").getAsString();
        double score = event.get("sentimentScore").getAsDouble();
        String semanticLabel = getSemanticLabel(score);

        String sqlNews = "INSERT INTO news_feed (published_at, title, url, sentiment_label) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sqlNews)) {
            stmt.setString(1, event.get("ts").getAsString());
            stmt.setString(2, title);
            stmt.setString(3, event.get("url").getAsString());
            stmt.setString(4, semanticLabel);
            stmt.executeUpdate();
        }

        String sqlHype = """
            INSERT INTO market_hype_alerts (time_window, coin_id, news_volume, average_sentiment_score)
            VALUES (?, 'ethereum', 1, ?)
            ON CONFLICT(time_window, coin_id) DO UPDATE SET
                news_volume = news_volume + 1,
                average_sentiment_score = ((average_sentiment_score * news_volume) + excluded.average_sentiment_score) / (news_volume + 1),
                hype_warning = (CASE WHEN (news_volume + 1) > 5 AND is_high_volatility = 1 THEN 1 ELSE 0 END);
        """;
        try (PreparedStatement stmt = conn.prepareStatement(sqlHype)) {
            stmt.setString(1, timeWindow);
            stmt.setDouble(2, score);
            stmt.executeUpdate();
        }
    }

    private String getSemanticLabel(double score) {
        if (score > 0.6) return "MUY POSITIVO";
        if (score > 0.1) return "POSITIVO";
        if (score < -0.6) return "MUY NEGATIVO";
        if (score < -0.1) return "NEGATIVO";
        return "NEUTRAL";
    }
}