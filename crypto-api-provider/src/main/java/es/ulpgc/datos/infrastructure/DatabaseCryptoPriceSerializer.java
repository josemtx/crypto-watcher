package es.ulpgc.datos.infrastructure;

import es.ulpgc.datos.domain.CryptoPrice;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

public class DatabaseCryptoPriceSerializer implements CryptoPriceSerializer {
    private static final String DEFAULT_DB_URL = "jdbc:sqlite:crypto_data.db";

    private final String dbUrl;

    public DatabaseCryptoPriceSerializer() {
        this(DEFAULT_DB_URL);
    }

    public DatabaseCryptoPriceSerializer(String dbUrl) {
        this.dbUrl = normalizeDbUrl(dbUrl);
        createTableIfNotExists();
    }

    @Override
    public void save(List<CryptoPrice> prices) {
        if (prices == null || prices.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO crypto_prices (
                    coin_id, symbol, name, price, market_cap, volume_24h, captured_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            for (CryptoPrice price : prices) {
                statement.setString(1, price.coinId());
                statement.setString(2, price.symbol());
                statement.setString(3, price.name());
                statement.setDouble(4, price.price());
                setNullableDouble(statement, 5, price.marketCap());
                setNullableDouble(statement, 6, price.volume24h());
                statement.setString(7, price.capturedAt().toString());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error saving crypto prices: " + e.getMessage());
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    private void createTableIfNotExists() {
        String sql = """
                CREATE TABLE IF NOT EXISTS crypto_prices (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    coin_id TEXT NOT NULL,
                    symbol TEXT NOT NULL,
                    name TEXT NOT NULL,
                    price REAL NOT NULL,
                    market_cap REAL,
                    volume_24h REAL,
                    captured_at TEXT NOT NULL
                )
                """;

        try (Connection connection = connect();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error creating crypto_prices table: " + e.getMessage());
        }
    }

    private void setNullableDouble(PreparedStatement statement, int parameterIndex, Double value) throws SQLException {
        if (value != null) {
            statement.setDouble(parameterIndex, value);
        } else {
            statement.setNull(parameterIndex, Types.REAL);
        }
    }

    private String normalizeDbUrl(String dbUrl) {
        if (dbUrl == null || dbUrl.isBlank()) {
            return DEFAULT_DB_URL;
        }

        String trimmed = dbUrl.trim();
        if (trimmed.startsWith("jdbc:sqlite:")) {
            return trimmed;
        }

        return "jdbc:sqlite:" + trimmed;
    }
}