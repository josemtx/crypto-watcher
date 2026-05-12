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
        app.get("/api/alerts", this::getAlerts);
        app.get("/api/news", this::getNews);
        app.get("/api/summary", this::getSummary);
    }

    private void getCoins(Context ctx) {
        // Busca dinámicamente qué monedas estamos trackeando
        String sql = "SELECT DISTINCT coin_id FROM crypto_timeline ORDER BY coin_id";
        ctx.result(gson.toJson(executeQuery(sql))).contentType("application/json");
    }

    private void getTimeline(Context ctx) {
        String coin = ctx.queryParamAsClass("coin", String.class).getOrDefault("ethereum");
        String sql = "SELECT time_window, close_price, max_price, min_price FROM crypto_timeline WHERE coin_id = ? ORDER BY time_window DESC LIMIT 24";
        ctx.result(gson.toJson(executeQuery(sql, coin))).contentType("application/json");
    }

    private void getAlerts(Context ctx) {
        String coin = ctx.queryParamAsClass("coin", String.class).getOrDefault("ethereum");
        String sql = "SELECT time_window, news_volume, average_sentiment_score, is_high_volatility, hype_warning FROM market_hype_alerts WHERE coin_id = ? ORDER BY time_window DESC LIMIT 24";
        ctx.result(gson.toJson(executeQuery(sql, coin))).contentType("application/json");
    }

    private void getNews(Context ctx) {
        // Las noticias son globales (afectan a todo el mercado)
        String sql = "SELECT published_at, title, url, sentiment_label FROM news_feed ORDER BY published_at DESC LIMIT 20";
        ctx.result(gson.toJson(executeQuery(sql))).contentType("application/json");
    }

    private void getSummary(Context ctx) {
        String coin = ctx.queryParamAsClass("coin", String.class).getOrDefault("ethereum");
        Map<String, Object> summary = new HashMap<>();

        // 1. Hora de mayor riesgo hoy (máxima volatilidad)
        String sqlRisk = "SELECT time_window, (max_price - min_price) as oscilacion FROM crypto_timeline WHERE coin_id = ? ORDER BY oscilacion DESC LIMIT 1";
        List<Map<String, Object>> riskResult = executeQuery(sqlRisk, coin);
        summary.put("highest_risk_hour", riskResult.isEmpty() ? "N/A" : riskResult.get(0).get("time_window"));

        // 2. Termómetro del mercado (Moda del sentimiento en las últimas 20 noticias)
        String sqlThermometer = "SELECT sentiment_label, COUNT(*) as frec FROM news_feed GROUP BY sentiment_label ORDER BY frec DESC LIMIT 1";
        List<Map<String, Object>> thermoResult = executeQuery(sqlThermometer);
        summary.put("market_thermometer", thermoResult.isEmpty() ? "NEUTRAL" : thermoResult.get(0).get("sentiment_label"));

        // 3. Alerta de Ballenas Silenciosas (Alta volatilidad pero 0 noticias)
        String sqlWhales = "SELECT COUNT(*) as silent_moves FROM market_hype_alerts WHERE coin_id = ? AND is_high_volatility = 1 AND news_volume = 0";
        List<Map<String, Object>> whalesResult = executeQuery(sqlWhales, coin);
        summary.put("silent_whales_detected", whalesResult.isEmpty() ? 0 : whalesResult.get(0).get("silent_moves"));

        ctx.result(gson.toJson(summary)).contentType("application/json");
    }

    // Ejecutor seguro con parámetros para evitar SQL Injection
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