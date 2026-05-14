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

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS crypto_timeline (
                    time_window TEXT,
                    coin_id TEXT,
                    close_price REAL,
                    min_price REAL,
                    max_price REAL,
                    PRIMARY KEY (time_window, coin_id)
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS news_feed (
                    news_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    published_at TEXT,
                    title TEXT,
                    url TEXT,
                    sentiment_label TEXT
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS market_hype_alerts (
                    time_window TEXT,
                    coin_id TEXT,
                    news_volume INTEGER DEFAULT 0,
                    average_sentiment_score REAL DEFAULT 0.0,
                    is_high_volatility BOOLEAN DEFAULT 0,
                    hype_warning BOOLEAN DEFAULT 0,
                    PRIMARY KEY (time_window, coin_id)
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS market_signal (
                    time_window TEXT,
                    coin_id TEXT,
                    volatility_ratio REAL DEFAULT 0.0,
                    signal TEXT NOT NULL,
                    hype_warning BOOLEAN DEFAULT 0,
                    PRIMARY KEY (time_window, coin_id)
                );
            """);

            System.out.println("Datamart (SQLite) inicializado correctamente.");

        } catch (Exception e) {
            throw new RuntimeException("Error inicializando el Datamart", e);
        }
    }
}