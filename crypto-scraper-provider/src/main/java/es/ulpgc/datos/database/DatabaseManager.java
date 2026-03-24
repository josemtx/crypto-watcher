package es.ulpgc.datos.database;

import es.ulpgc.datos.model.NewsItem;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class DatabaseManager {

    // La base de datos se creará en la raíz de tu proyecto
    private static final String DB_URL = "jdbc:sqlite:crypto_data.db";

    public DatabaseManager() {
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = """
                CREATE TABLE IF NOT EXISTS news (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    url TEXT NOT NULL UNIQUE,
                    captured_at TEXT NOT NULL
                );
                """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error al crear la tabla de noticias: " + e.getMessage());
        }
    }

    public void insertNews(List<NewsItem> newsList) {
        // INSERT OR IGNORE evita que se dupliquen noticias si la URL ya existe
        String sql = "INSERT OR IGNORE INTO news (title, url, captured_at) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            int insertedCount = 0;

            for (NewsItem item : newsList) {
                pstmt.setString(1, item.title());
                pstmt.setString(2, item.url());
                pstmt.setString(3, item.capturedAt().toString());

                // executeUpdate devuelve 1 si insertó, 0 si fue ignorado por el UNIQUE
                insertedCount += pstmt.executeUpdate();
            }

            System.out.println("Se han guardado " + insertedCount + " noticias nuevas en SQLite.");

        } catch (SQLException e) {
            System.err.println("Error al insertar las noticias: " + e.getMessage());
        }
    }
}