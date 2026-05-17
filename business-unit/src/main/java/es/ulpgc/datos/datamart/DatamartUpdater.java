package es.ulpgc.datos.datamart;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import es.ulpgc.datos.subscriber.EventProcessor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DatamartUpdater implements EventProcessor {
    private static final int HYPE_NEWS_THRESHOLD = 5;
    private static final String GLOBAL_CONTEXT_ID = "__global__";

    private final String dbUrl;
    private final MarketSignalCalculator signalCalculator;

    public DatamartUpdater(String dbPath) {
        this.dbUrl = "jdbc:sqlite:" + dbPath;
        this.signalCalculator = new MarketSignalCalculator();
    }

    @Override
    public void processEvent(String json) {
        try {
            JsonObject event = JsonParser.parseString(json).getAsJsonObject();
            String ts = event.get("ts").getAsString();
            String timeWindow = ts.substring(0, 13) + ":00";

            try (Connection conn = DriverManager.getConnection(dbUrl)) {
                if (event.has("priceUsd") || event.has("price")) {
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
        double price = event.has("priceUsd") ? event.get("priceUsd").getAsDouble() : event.get("price").getAsDouble();
        double volume = event.has("volume24h") ? event.get("volume24h").getAsDouble() : 0.0;
        double marketCap = event.has("marketCap") ? event.get("marketCap").getAsDouble() : 0.0;

        String sqlTimeline = """
            INSERT INTO crypto_timeline (time_window, coin_id, close_price, min_price, max_price, volume_24h, market_cap)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(time_window, coin_id) DO UPDATE SET
                close_price = excluded.close_price,
                min_price = MIN(crypto_timeline.min_price, excluded.min_price),
                max_price = MAX(crypto_timeline.max_price, excluded.max_price),
                volume_24h = excluded.volume_24h,
                market_cap = excluded.market_cap;
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
            stmt.setDouble(6, volume);
            stmt.setDouble(7, marketCap);
            stmt.executeUpdate();
        }

        try (PreparedStatement stmt = conn.prepareStatement(sqlAlerts)) {
            stmt.setString(1, timeWindow);
            stmt.setString(2, coinId);
            stmt.executeUpdate();
        }

        updateMarketSignal(conn, timeWindow, coinId);
    }

    private void processNewsEvent(Connection conn, JsonObject event, String timeWindow) throws Exception {
        String title = event.get("title").getAsString();
        double score = event.get("sentimentScore").getAsDouble();
        String semanticLabel = getSemanticLabel(score);

        String sqlNews = """
            INSERT INTO news_feed (published_at, title, url, sentiment_label)
            VALUES (?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sqlNews)) {
            stmt.setString(1, event.get("ts").getAsString());
            stmt.setString(2, title);
            stmt.setString(3, event.get("url").getAsString());
            stmt.setString(4, semanticLabel);
            stmt.executeUpdate();
        }

        String sqlGlobalContext = """
            INSERT INTO market_hype_alerts (time_window, coin_id, news_volume, average_sentiment_score, is_high_volatility, hype_warning)
            VALUES (?, ?, 1, ?, 0, 0)
            ON CONFLICT(time_window, coin_id) DO UPDATE SET
                news_volume = news_volume + 1,
                average_sentiment_score = ((average_sentiment_score * news_volume) + excluded.average_sentiment_score) / (news_volume + 1);
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sqlGlobalContext)) {
            stmt.setString(1, timeWindow);
            stmt.setString(2, GLOBAL_CONTEXT_ID);
            stmt.setDouble(3, score);
            stmt.executeUpdate();
        }

        updateMarketSignalsForWindow(conn, timeWindow);
    }

    private void updateMarketSignalsForWindow(Connection conn, String timeWindow) throws Exception {
        String sqlCoins = """
            SELECT DISTINCT coin_id
            FROM crypto_timeline
            WHERE time_window = ?
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sqlCoins)) {
            stmt.setString(1, timeWindow);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String coinId = rs.getString("coin_id");
                    updateMarketSignal(conn, timeWindow, coinId);
                }
            }
        }
    }

    private void updateMarketSignal(Connection conn, String timeWindow, String coinId) throws Exception {
        MarketMetrics metrics = loadMetrics(conn, timeWindow, coinId);
        String signal = signalCalculator.calculateSignal(
                metrics.volatilityRatio(),
                metrics.newsVolume(),
                metrics.averageSentimentScore(),
                metrics.hypeWarning()
        );

        String sqlSignal = """
            INSERT INTO market_signal (time_window, coin_id, volatility_ratio, signal, hype_warning)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(time_window, coin_id) DO UPDATE SET
                volatility_ratio = excluded.volatility_ratio,
                signal = excluded.signal,
                hype_warning = excluded.hype_warning;
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sqlSignal)) {
            stmt.setString(1, timeWindow);
            stmt.setString(2, coinId);
            stmt.setDouble(3, metrics.volatilityRatio());
            stmt.setString(4, signal);
            stmt.setBoolean(5, metrics.hypeWarning());
            stmt.executeUpdate();
        }
    }

    private MarketMetrics loadMetrics(Connection conn, String timeWindow, String coinId) throws Exception {
        double minPrice = 0.0;
        double maxPrice = 0.0;
        int newsVolume = 0;
        double averageSentimentScore = 0.0;

        String sqlTimeline = """
            SELECT min_price, max_price
            FROM crypto_timeline
            WHERE time_window = ? AND coin_id = ?
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sqlTimeline)) {
            stmt.setString(1, timeWindow);
            stmt.setString(2, coinId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    minPrice = rs.getDouble("min_price");
                    maxPrice = rs.getDouble("max_price");
                }
            }
        }

        String sqlGlobalContext = """
            SELECT news_volume, average_sentiment_score
            FROM market_hype_alerts
            WHERE time_window = ? AND coin_id = ?
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sqlGlobalContext)) {
            stmt.setString(1, timeWindow);
            stmt.setString(2, GLOBAL_CONTEXT_ID);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    newsVolume = rs.getInt("news_volume");
                    averageSentimentScore = rs.getDouble("average_sentiment_score");
                }
            }
        }

        double volatilityRatio = signalCalculator.calculateVolatilityRatio(minPrice, maxPrice);
        boolean hypeWarning = signalCalculator.calculateHypeWarning(volatilityRatio, newsVolume);

        return new MarketMetrics(volatilityRatio, newsVolume, averageSentimentScore, hypeWarning);
    }

    private String getSemanticLabel(double score) {
        if (score > 0.6) return "MUY POSITIVO";
        if (score > 0.1) return "POSITIVO";
        if (score < -0.6) return "MUY NEGATIVO";
        if (score < -0.1) return "NEGATIVO";
        return "NEUTRAL";
    }

    private record MarketMetrics(
            double volatilityRatio,
            int newsVolume,
            double averageSentimentScore,
            boolean hypeWarning
    ) {
    }
}