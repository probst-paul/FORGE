package forge.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

public class PostgresTradeRepository {
    private static final String IMPORT_CHECKPOINT_TABLE = "forge_contract_imports";

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
                            quoteIdentifier("bidPrice") + " FLOAT4, " +
                            quoteIdentifier("askPrice") + " FLOAT4, " +
                            "quantity BIGINT NOT NULL, " +
                            "side INT, " +
                            quoteIdentifier("numTrades") + " BIGINT NOT NULL, " +
                            quoteIdentifier("sourceFileName") + " TEXT NOT NULL, " +
                            quoteIdentifier("scidRecordIndex") + " BIGINT NOT NULL" +
                            ")"
            );
            statement.executeUpdate(
                    "ALTER TABLE " + quoteIdentifier(tableName) +
                            " ALTER COLUMN quantity TYPE BIGINT"
            );
            statement.executeUpdate(
                    "ALTER TABLE " + quoteIdentifier(tableName) +
                            " ALTER COLUMN side DROP NOT NULL"
            );
            statement.executeUpdate(
                    "ALTER TABLE " + quoteIdentifier(tableName) +
                            " ADD COLUMN IF NOT EXISTS " + quoteIdentifier("bidPrice") + " FLOAT4"
            );
            statement.executeUpdate(
                    "ALTER TABLE " + quoteIdentifier(tableName) +
                            " ADD COLUMN IF NOT EXISTS " + quoteIdentifier("askPrice") + " FLOAT4"
            );
            statement.executeUpdate(
                    "ALTER TABLE " + quoteIdentifier(tableName) +
                            " ADD COLUMN IF NOT EXISTS " + quoteIdentifier("numTrades") + " BIGINT NOT NULL DEFAULT 1"
            );
            statement.executeUpdate(
                    "ALTER TABLE " + quoteIdentifier(tableName) +
                            " ALTER COLUMN " + quoteIdentifier("numTrades") + " TYPE BIGINT"
            );
            statement.executeUpdate(
                    "ALTER TABLE " + quoteIdentifier(tableName) +
                            " ADD COLUMN IF NOT EXISTS " + quoteIdentifier("sourceFileName") + " TEXT"
            );
            statement.executeUpdate(
                    "ALTER TABLE " + quoteIdentifier(tableName) +
                            " ADD COLUMN IF NOT EXISTS " + quoteIdentifier("scidRecordIndex") + " BIGINT"
            );
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not prepare PostgreSQL trades table '" + tableName + "'", exception);
        }
    }

    public void ensureContractRecordUniqueIndex(String tableName) {
        try (Connection connection = DriverManager.getConnection(
                settings.primaryJdbcUrl(),
                settings.getUsername(),
                settings.getPassword()
        );
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " + quoteIdentifier(tableName + "_record_uidx") +
                            " ON " + quoteIdentifier(tableName) +
                            " (" + quoteIdentifier("scidRecordIndex") + ")" +
                            " WHERE " + quoteIdentifier("scidRecordIndex") + " IS NOT NULL"
            );
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not prepare PostgreSQL unique index for '" + tableName + "'", exception);
        }
    }

    public void ensureImportCheckpointTableExists() {
        try (Connection connection = DriverManager.getConnection(
                settings.primaryJdbcUrl(),
                settings.getUsername(),
                settings.getPassword()
        );
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS " + quoteIdentifier(IMPORT_CHECKPOINT_TABLE) + " (" +
                            quoteIdentifier("tableName") + " TEXT NOT NULL, " +
                            quoteIdentifier("sourceFileName") + " TEXT NOT NULL, " +
                            quoteIdentifier("fileSizeBytes") + " BIGINT NOT NULL, " +
                            quoteIdentifier("lastModifiedMillis") + " BIGINT NOT NULL, " +
                            quoteIdentifier("nextRecordIndex") + " BIGINT NOT NULL, " +
                            quoteIdentifier("rowsInserted") + " BIGINT NOT NULL, " +
                            "status TEXT NOT NULL, " +
                            quoteIdentifier("startedAt") + " TIMESTAMPTZ NOT NULL, " +
                            quoteIdentifier("updatedAt") + " TIMESTAMPTZ NOT NULL, " +
                            "PRIMARY KEY (" + quoteIdentifier("tableName") + ")" +
                            ")"
            );
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not prepare PostgreSQL import checkpoint table", exception);
        }
    }

    public DataImportPlan planImport(String contractSymbol, String tableName) {
        ensureImportCheckpointTableExists();

        try (Connection connection = DriverManager.getConnection(
                settings.primaryJdbcUrl(),
                settings.getUsername(),
                settings.getPassword()
        )) {
            boolean tableExists = contractTableExists(connection, tableName);
            long existingRows = tableExists ? countRows(connection, tableName) : 0;
            CheckpointMetadata metadata = findCheckpointMetadata(connection, tableName);
            return new DataImportPlan(
                    contractSymbol,
                    tableName,
                    tableExists,
                    existingRows,
                    metadata == null ? null : metadata.getSourceFileName(),
                    metadata == null ? null : metadata.getStatus()
            );
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not inspect PostgreSQL import state for '" + tableName + "'", exception);
        }
    }

    public ImportCheckpoint prepareImportCheckpoint(
            String tableName,
            String sourceFileName,
            long fileSizeBytes,
            long lastModifiedMillis,
            boolean rebuildExistingContract
    ) {
        ensureImportCheckpointTableExists();

        try (Connection connection = DriverManager.getConnection(
                settings.primaryJdbcUrl(),
                settings.getUsername(),
                settings.getPassword()
        )) {
            boolean tableExists = contractTableExists(connection, tableName);
            long existingRows = tableExists ? countRows(connection, tableName) : 0;
            ImportCheckpoint existingCheckpoint = findImportCheckpoint(connection, tableName);
            CheckpointMetadata existingMetadata = findCheckpointMetadata(connection, tableName);

            if (existingRows > 0 && !rebuildExistingContract) {
                if (existingMetadata != null
                        && "IN_PROGRESS".equals(existingMetadata.getStatus())
                        && existingMetadata.matchesSource(sourceFileName, fileSizeBytes, lastModifiedMillis)) {
                    markImportInProgress(connection, tableName);
                    return existingCheckpoint;
                }
                throw new IllegalStateException("Contract table '" + tableName + "' already contains data and rebuild was not confirmed");
            }

            if (rebuildExistingContract) {
                truncateContractTable(connection, tableName);
                if (existingCheckpoint == null) {
                    insertImportCheckpoint(connection, tableName, sourceFileName, fileSizeBytes, lastModifiedMillis);
                } else {
                    resetImportCheckpoint(connection, tableName, sourceFileName, fileSizeBytes, lastModifiedMillis);
                }
                return new ImportCheckpoint(tableName, sourceFileName, 1);
            }

            if (existingCheckpoint == null) {
                insertImportCheckpoint(connection, tableName, sourceFileName, fileSizeBytes, lastModifiedMillis);
                return new ImportCheckpoint(tableName, sourceFileName, 1);
            }

            if (checkpointMatchesSourceFile(connection, tableName, sourceFileName, fileSizeBytes, lastModifiedMillis)) {
                markImportInProgress(connection, tableName);
                return existingCheckpoint;
            }

            resetImportCheckpoint(connection, tableName, sourceFileName, fileSizeBytes, lastModifiedMillis);
            return new ImportCheckpoint(tableName, sourceFileName, 1);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load PostgreSQL import checkpoint", exception);
        }
    }

    public void markImportComplete(String tableName, String sourceFileName) {
        try (Connection connection = DriverManager.getConnection(
                settings.primaryJdbcUrl(),
                settings.getUsername(),
                settings.getPassword()
        );
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE " + quoteIdentifier(IMPORT_CHECKPOINT_TABLE) +
                             " SET status = ?, " + quoteIdentifier("updatedAt") + " = ?" +
                             " WHERE " + quoteIdentifier("tableName") + " = ?"
             )) {
            statement.setString(1, "COMPLETE");
            statement.setTimestamp(2, Timestamp.from(Instant.now()));
            statement.setString(3, tableName);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not mark PostgreSQL import checkpoint complete", exception);
        }
    }

    public int insertTradesAndAdvanceCheckpoint(
            String tableName,
            String sourceFileName,
            List<TradeRow> trades,
            long nextRecordIndex
    ) {
        if (trades == null || trades.isEmpty()) {
            return 0;
        }

        String sql = "INSERT INTO " + quoteIdentifier(tableName) + " (" +
                quoteIdentifier("tradeDateTime") + ", " +
                "price, " +
                quoteIdentifier("bidPrice") + ", " +
                quoteIdentifier("askPrice") + ", " +
                "quantity, " +
                "side, " +
                quoteIdentifier("numTrades") + ", " +
                quoteIdentifier("sourceFileName") + ", " +
                quoteIdentifier("scidRecordIndex") +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (" + quoteIdentifier("scidRecordIndex") + ") " +
                "WHERE " + quoteIdentifier("scidRecordIndex") + " IS NOT NULL " +
                "DO NOTHING";

        try (Connection connection = DriverManager.getConnection(
                settings.primaryJdbcUrl(),
                settings.getUsername(),
                settings.getPassword()
        );
             PreparedStatement statement = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false);
            for (TradeRow trade : trades) {
                statement.setTimestamp(1, Timestamp.from(trade.getTradeDateTime()));
                statement.setFloat(2, trade.getPrice());
                setNullableFloat(statement, 3, trade.getBidPrice());
                setNullableFloat(statement, 4, trade.getAskPrice());
                statement.setLong(5, trade.getQuantity());
                setNullableInteger(statement, 6, trade.getSide());
                statement.setLong(7, trade.getNumTrades());
                statement.setString(8, sourceFileName);
                statement.setLong(9, trade.getScidRecordIndex());
                statement.addBatch();
            }

            int importedRows = 0;
            for (int rowCount : statement.executeBatch()) {
                if (rowCount > 0) {
                    importedRows += rowCount;
                }
            }
            updateImportCheckpoint(connection, tableName, sourceFileName, nextRecordIndex, importedRows);
            connection.commit();
            return importedRows;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not insert trades into PostgreSQL table '" + tableName + "'", exception);
        }
    }

    public String getDatabaseName() {
        return settings.getDatabaseName();
    }

    private ImportCheckpoint findImportCheckpoint(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT " + quoteIdentifier("sourceFileName") + ", " + quoteIdentifier("nextRecordIndex") +
                        " FROM " + quoteIdentifier(IMPORT_CHECKPOINT_TABLE) +
                        " WHERE " + quoteIdentifier("tableName") + " = ?"
        )) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return new ImportCheckpoint(tableName, resultSet.getString(1), resultSet.getLong(2));
            }
        }
    }

    private CheckpointMetadata findCheckpointMetadata(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT " + quoteIdentifier("sourceFileName") + ", " +
                        quoteIdentifier("fileSizeBytes") + ", " +
                        quoteIdentifier("lastModifiedMillis") + ", " +
                        "status" +
                        " FROM " + quoteIdentifier(IMPORT_CHECKPOINT_TABLE) +
                        " WHERE " + quoteIdentifier("tableName") + " = ?"
        )) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return new CheckpointMetadata(
                        resultSet.getString(1),
                        resultSet.getLong(2),
                        resultSet.getLong(3),
                        resultSet.getString(4)
                );
            }
        }
    }

    private boolean checkpointMatchesSourceFile(
            Connection connection,
            String tableName,
            String sourceFileName,
            long fileSizeBytes,
            long lastModifiedMillis
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM " + quoteIdentifier(IMPORT_CHECKPOINT_TABLE) +
                        " WHERE " + quoteIdentifier("tableName") + " = ?" +
                        " AND " + quoteIdentifier("fileSizeBytes") + " = ?" +
                        " AND " + quoteIdentifier("lastModifiedMillis") + " = ?" +
                        " AND " + quoteIdentifier("sourceFileName") + " = ?"
        )) {
            statement.setString(1, tableName);
            statement.setLong(2, fileSizeBytes);
            statement.setLong(3, lastModifiedMillis);
            statement.setString(4, sourceFileName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void insertImportCheckpoint(
            Connection connection,
            String tableName,
            String sourceFileName,
            long fileSizeBytes,
            long lastModifiedMillis
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO " + quoteIdentifier(IMPORT_CHECKPOINT_TABLE) + " (" +
                        quoteIdentifier("tableName") + ", " +
                        quoteIdentifier("sourceFileName") + ", " +
                        quoteIdentifier("fileSizeBytes") + ", " +
                        quoteIdentifier("lastModifiedMillis") + ", " +
                        quoteIdentifier("nextRecordIndex") + ", " +
                        quoteIdentifier("rowsInserted") + ", " +
                        "status, " +
                        quoteIdentifier("startedAt") + ", " +
                        quoteIdentifier("updatedAt") +
                        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
        )) {
            Instant now = Instant.now();
            statement.setString(1, tableName);
            statement.setString(2, sourceFileName);
            statement.setLong(3, fileSizeBytes);
            statement.setLong(4, lastModifiedMillis);
            statement.setLong(5, 1);
            statement.setLong(6, 0);
            statement.setString(7, "IN_PROGRESS");
            statement.setTimestamp(8, Timestamp.from(now));
            statement.setTimestamp(9, Timestamp.from(now));
            statement.executeUpdate();
        }
    }

    private void markImportInProgress(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE " + quoteIdentifier(IMPORT_CHECKPOINT_TABLE) +
                        " SET status = ?, " + quoteIdentifier("updatedAt") + " = ?" +
                        " WHERE " + quoteIdentifier("tableName") + " = ?"
        )) {
            statement.setString(1, "IN_PROGRESS");
            statement.setTimestamp(2, Timestamp.from(Instant.now()));
            statement.setString(3, tableName);
            statement.executeUpdate();
        }
    }

    private void resetImportCheckpoint(
            Connection connection,
            String tableName,
            String sourceFileName,
            long fileSizeBytes,
            long lastModifiedMillis
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE " + quoteIdentifier(IMPORT_CHECKPOINT_TABLE) +
                        " SET " + quoteIdentifier("fileSizeBytes") + " = ?, " +
                        quoteIdentifier("lastModifiedMillis") + " = ?, " +
                        quoteIdentifier("nextRecordIndex") + " = ?, " +
                        quoteIdentifier("rowsInserted") + " = ?, " +
                        "status = ?, " +
                        quoteIdentifier("startedAt") + " = ?, " +
                        quoteIdentifier("updatedAt") + " = ?" +
                        " WHERE " + quoteIdentifier("tableName") + " = ?"
        )) {
            Instant now = Instant.now();
            statement.setLong(1, fileSizeBytes);
            statement.setLong(2, lastModifiedMillis);
            statement.setLong(3, 1);
            statement.setLong(4, 0);
            statement.setString(5, "IN_PROGRESS");
            statement.setTimestamp(6, Timestamp.from(now));
            statement.setTimestamp(7, Timestamp.from(now));
            statement.setString(8, tableName);
            statement.executeUpdate();
        }
    }

    private void truncateContractTable(Connection connection, String tableName) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("TRUNCATE TABLE " + quoteIdentifier(tableName));
        }
    }

    private boolean contractTableExists(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = ?"
        )) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private long countRows(Connection connection, String tableName) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + quoteIdentifier(tableName))) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private void deleteCheckpoint(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM " + quoteIdentifier(IMPORT_CHECKPOINT_TABLE) +
                        " WHERE " + quoteIdentifier("tableName") + " = ?"
        )) {
            statement.setString(1, tableName);
            statement.executeUpdate();
        }
    }

    private void updateImportCheckpoint(
            Connection connection,
            String tableName,
            String sourceFileName,
            long nextRecordIndex,
            int importedRows
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE " + quoteIdentifier(IMPORT_CHECKPOINT_TABLE) +
                        " SET " + quoteIdentifier("nextRecordIndex") + " = ?, " +
                        quoteIdentifier("rowsInserted") + " = " + quoteIdentifier("rowsInserted") + " + ?, " +
                        "status = ?, " +
                        quoteIdentifier("updatedAt") + " = ?" +
                        " WHERE " + quoteIdentifier("tableName") + " = ?"
        )) {
            statement.setLong(1, nextRecordIndex);
            statement.setInt(2, importedRows);
            statement.setString(3, "IN_PROGRESS");
            statement.setTimestamp(4, Timestamp.from(Instant.now()));
            statement.setString(5, tableName);
            statement.executeUpdate();
        }
    }

    private static class CheckpointMetadata {
        private final String sourceFileName;
        private final long fileSizeBytes;
        private final long lastModifiedMillis;
        private final String status;

        private CheckpointMetadata(String sourceFileName, long fileSizeBytes, long lastModifiedMillis, String status) {
            this.sourceFileName = sourceFileName;
            this.fileSizeBytes = fileSizeBytes;
            this.lastModifiedMillis = lastModifiedMillis;
            this.status = status;
        }

        public String getSourceFileName() {
            return sourceFileName;
        }

        public String getStatus() {
            return status;
        }

        public boolean matchesSource(String sourceFileName, long fileSizeBytes, long lastModifiedMillis) {
            return this.sourceFileName.equals(sourceFileName)
                    && this.fileSizeBytes == fileSizeBytes
                    && this.lastModifiedMillis == lastModifiedMillis;
        }
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

    private void setNullableFloat(PreparedStatement statement, int index, Float value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.REAL);
            return;
        }
        statement.setFloat(index, value);
    }

    private void setNullableInteger(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.INTEGER);
            return;
        }
        statement.setInt(index, value);
    }

}
