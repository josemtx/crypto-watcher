package es.ulpgc.datos.api;

import com.google.gson.Gson;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatamartApiController {
    private static final double LOW_VOLATILITY_THRESHOLD = 0.01;
    private static final double HIGH_VOLATILITY_THRESHOLD = 0.02;
    private static final String GLOBAL_CONTEXT_ID = "__global__";

    private final String dbUrl;
    private final Javalin app;
    private final Gson gson;

    public DatamartApiController(String dbPath) {
        this.dbUrl = "jdbc:sqlite:" + dbPath;
        this.gson = new Gson();

        this.app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
            config.staticFiles.add("/public");
        });

        setupRoutes();
    }

    public void start(int port) {
        app.start(port);
        System.out.println("[API] Servidor REST iniciado en http://localhost:" + port);
    }

    public void stop() {
        app.stop();
    }

    private void setupRoutes() {
        app.get("/api/coins", this::getCoins);
        app.get("/api/timeline", this::getTimeline);
        app.get("/api/context", this::getContext);
        app.get("/api/news", this::getNews);
        app.get("/api/summary", this::getSummary);
    }

    private void getCoins(Context ctx) {
        String sql = "SELECT DISTINCT coin_id FROM crypto_timeline ORDER BY coin_id";
        ctx.result(gson.toJson(executeQuery(sql))).contentType("application/json");
    }

    private void getTimeline(Context ctx) {
        String coin = ctx.queryParamAsClass("coin", String.class).getOrDefault("ethereum");
        String sql = """
                SELECT time_window, close_price, max_price, min_price
                FROM crypto_timeline
                WHERE coin_id = ?
                ORDER BY time_window DESC
                LIMIT 24
                """;
        ctx.result(gson.toJson(executeQuery(sql, coin))).contentType("application/json");
    }

    private void getContext(Context ctx) {
        String sql = """
                SELECT time_window, news_volume, average_sentiment_score, hype_warning
                FROM market_hype_alerts
                WHERE coin_id = ?
                ORDER BY time_window DESC
                LIMIT 24
                """;
        ctx.result(gson.toJson(executeQuery(sql, GLOBAL_CONTEXT_ID))).contentType("application/json");
    }

    private void getNews(Context ctx) {
        String sql = """
                SELECT published_at, title, url, sentiment_label
                FROM news_feed
                ORDER BY published_at DESC
                LIMIT 20
                """;
        ctx.result(gson.toJson(executeQuery(sql))).contentType("application/json");
    }

    private void getSummary(Context ctx) {
        String coin = ctx.queryParamAsClass("coin", String.class).getOrDefault("ethereum");
        Map<String, Object> summary = new HashMap<>();

        Map<String, Object> latestSignal = getLatestSignal(coin);
        Map<String, Object> latestGlobalContext = getLatestGlobalContext();

        Map<String, Object> latestTimelineStats = getLatestTimelineStats(coin);

        summary.put("coinId", coin);
        summary.put("signal", latestSignal.getOrDefault("signal", "Neutral"));
        summary.put("volatilityRatio", latestSignal.getOrDefault("volatility_ratio", 0.0));
        summary.put("volatilityLevel", toVolatilityLevel(asDouble(latestSignal.get("volatility_ratio"))));
        summary.put("hypeWarning", latestSignal.getOrDefault("hype_warning", false));
        summary.put("newsVolume", latestGlobalContext.getOrDefault("news_volume", 0));
        summary.put("averageSentimentScore", latestGlobalContext.getOrDefault("average_sentiment_score", 0.0));
        summary.put("sentimentLabel", toSentimentLabel(asDouble(latestGlobalContext.get("average_sentiment_score"))));

        // 2. Inyección de Volumen y Capitalización al JSON final
        summary.put("volume_24h", latestTimelineStats.getOrDefault("volume_24h", 0.0));
        summary.put("market_cap", latestTimelineStats.getOrDefault("market_cap", 0.0));

        ctx.result(gson.toJson(summary)).contentType("application/json");
    }


    private Map<String, Object> getLatestSignal(String coin) {
        String sql = """
                SELECT time_window, coin_id, volatility_ratio, signal, hype_warning
                FROM market_signal
                WHERE coin_id = ?
                ORDER BY time_window DESC
                LIMIT 1
                """;
        List<Map<String, Object>> result = executeQuery(sql, coin);
        return result.isEmpty() ? new HashMap<>() : result.get(0);
    }

    private Map<String, Object> getLatestGlobalContext() {
        String sql = """
                SELECT time_window, coin_id, news_volume, average_sentiment_score, hype_warning
                FROM market_hype_alerts
                WHERE coin_id = ?
                ORDER BY time_window DESC
                LIMIT 1
                """;
        List<Map<String, Object>> result = executeQuery(sql, GLOBAL_CONTEXT_ID);
        return result.isEmpty() ? new HashMap<>() : result.get(0);
    }

    private Map<String, Object> getLatestTimelineStats(String coin) {
        String sql = """
                SELECT volume_24h, market_cap
                FROM crypto_timeline
                WHERE coin_id = ?
                ORDER BY time_window DESC
                LIMIT 1
                """;
        List<Map<String, Object>> result = executeQuery(sql, coin);
        return result.isEmpty() ? new HashMap<>() : result.get(0);
    }

    private String toVolatilityLevel(double volatilityRatio) {
        if (volatilityRatio < LOW_VOLATILITY_THRESHOLD) {
            return "Baja";
        }
        if (volatilityRatio <= HIGH_VOLATILITY_THRESHOLD) {
            return "Media";
        }
        return "Alta";
    }

    private String toSentimentLabel(double score) {
        if (score > 0.1) {
            return "Positivo";
        }
        if (score < -0.1) {
            return "Negativo";
        }
        return "Neutral";
    }

    private double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 0.0;
    }

    private List<Map<String, Object>> executeQuery(String sql, Object... params) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                int columnCount = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(rs.getMetaData().getColumnName(i), rs.getObject(i));
                    }
                    resultList.add(row);
                }
            }
        } catch (Exception e) {
            System.err.println("[API] Error SQL: " + e.getMessage());
        }
        return resultList;
    }
}