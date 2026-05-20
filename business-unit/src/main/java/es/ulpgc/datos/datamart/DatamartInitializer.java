package es.ulpgc.datos.datamart;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DatamartInitializer {
    private final String dbUrl;

    public DatamartInitializer(String dbPath) {
        this.dbUrl = "jdbc:sqlite:" + dbPath;
    }

    public void initialize() {
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            String createTimelineTable = """
                CREATE TABLE IF NOT EXISTS crypto_timeline (
                    time_window TEXT,
                    coin_id TEXT,
                    close_price REAL,
                    min_price REAL,
                    max_price REAL,
                    volume_24h REAL DEFAULT 0,
                    market_cap REAL DEFAULT 0,
                    PRIMARY KEY (time_window, coin_id)
                );
            """;
            stmt.execute(createTimelineTable);

            String createNewsTable = """
                CREATE TABLE IF NOT EXISTS news_feed (
                    published_at TEXT,
                    title TEXT,
                    url TEXT,
                    sentiment_label TEXT
                );
            """;
            stmt.execute(createNewsTable);

            String createAlertsTable = """
                CREATE TABLE IF NOT EXISTS market_hype_alerts (
                    time_window TEXT,
                    coin_id TEXT,
                    news_volume INTEGER DEFAULT 0,
                    average_sentiment_score REAL DEFAULT 0.0,
                    is_high_volatility INTEGER DEFAULT 0,
                    hype_warning INTEGER DEFAULT 0,
                    PRIMARY KEY (time_window, coin_id)
                );
            """;
            stmt.execute(createAlertsTable);

            String createSignalTable = """
                CREATE TABLE IF NOT EXISTS market_signal (
                    time_window TEXT,
                    coin_id TEXT,
                    volatility_ratio REAL DEFAULT 0,
                    signal TEXT,
                    hype_warning INTEGER DEFAULT 0,
                    PRIMARY KEY (time_window, coin_id)
                );
            """;
            stmt.execute(createSignalTable);

            System.out.println("Datamart (SQLite) inicializado correctamente con el nuevo esquema.");
        } catch (Exception e) {
            System.err.println("Error inicializando SQLite: " + e.getMessage());
        }
    }
}