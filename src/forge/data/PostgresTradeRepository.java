package forge.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class PostgresTradeRepository {
    private final PostgresDatabaseSettings settings;

    public PostgresTradeRepository(PostgresDatabaseSettings settings) {
        if (settings == null) {
            throw new IllegalArgumentException("settings is required");
        }
        this.settings = settings;
    }

    public void ensureDatabaseExists() {
        try (Connection connection = DriverManager.getConnection(
                settings.maintenanceJdbcUrl(),
                settings.getUsername(),
                settings.getPassword()
        )) {
            if (!databaseExists(connection, settings.getDatabaseName())) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("CREATE DATABASE " + quoteIdentifier(settings.getDatabaseName()));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not prepare PostgreSQL database '" + settings.getDatabaseName() + "'", exception);
        }
    }

    public void ensureContractTradesTableExists(String tableName) {
        try (Connection connection = DriverManager.getConnection(
                settings.primaryJdbcUrl(),
                settings.getUsername(),
                settings.getPassword()
        );
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS " + quoteIdentifier(tableName) + " (" +
                            quoteIdentifier("tradeDateTime") + " TIMESTAMPTZ NOT NULL, " +
                            "price FLOAT4 NOT NULL, " +
                            "quantity INT NOT NULL, " +
                            "side INT NOT NULL" +
                            ")"
            );
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not prepare PostgreSQL trades table '" + tableName + "'", exception);
        }
    }

    public String getDatabaseName() {
        return settings.getDatabaseName();
    }

    private boolean databaseExists(Connection connection, String databaseName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM pg_database WHERE datname = ?"
        )) {
            statement.setString(1, databaseName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private String quoteIdentifier(String identifier) {
        if (identifier == null || !identifier.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Invalid PostgreSQL identifier: " + identifier);
        }
        return "\"" + identifier + "\"";
    }
}
