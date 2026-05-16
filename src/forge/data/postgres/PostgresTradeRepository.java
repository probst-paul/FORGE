package forge.data.postgres;

import forge.data.catalog.ContractDataSummary;
import forge.data.importing.DataImportPlan;
import forge.data.importing.ImportCheckpoint;
import forge.data.importing.TradeRow;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
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
                            quoteIdentifier("priceTicks") + " BIGINT NOT NULL, " +
                            quoteIdentifier("bidPriceTicks") + " BIGINT, " +
                            quoteIdentifier("askPriceTicks") + " BIGINT, " +
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
                            " ADD COLUMN IF NOT EXISTS " + quoteIdentifier("priceTicks") + " BIGINT NOT NULL DEFAULT 0"
            );
            statement.executeUpdate(
                    "ALTER TABLE " + quoteIdentifier(tableName) +
                            " ALTER COLUMN " + quoteIdentifier("priceTicks") + " DROP DEFAULT"
            );
            statement.executeUpdate(
                    "ALTER TABLE " + quoteIdentifier(tableName) +
                            " ADD COLUMN IF NOT EXISTS " + quoteIdentifier("bidPriceTicks") + " BIGINT"
            );
            statement.executeUpdate(
                    "ALTER TABLE " + quoteIdentifier(tableName) +
                            " ADD COLUMN IF NOT EXISTS " + quoteIdentifier("askPriceTicks") + " BIGINT"
            );
            statement.executeUpdate(
                    "ALTER TABLE " + quoteIdentifier(tableName) +
                            " DROP COLUMN IF EXISTS price"
            );
            statement.executeUpdate(
                    "ALTER TABLE " + quoteIdentifier(tableName) +
                            " DROP COLUMN IF EXISTS " + quoteIdentifier("bidPrice")
            );
            statement.executeUpdate(
                    "ALTER TABLE " + quoteIdentifier(tableName) +
                            " DROP COLUMN IF EXISTS " + quoteIdentifier("askPrice")
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
            statement.executeUpdate(
                    "ALTER TABLE " + quoteIdentifier(IMPORT_CHECKPOINT_TABLE) +
                            " ADD COLUMN IF NOT EXISTS " + quoteIdentifier("firstTradeDateTime") + " TIMESTAMPTZ"
            );
            statement.executeUpdate(
                    "ALTER TABLE " + quoteIdentifier(IMPORT_CHECKPOINT_TABLE) +
                            " ADD COLUMN IF NOT EXISTS " + quoteIdentifier("lastTradeDateTime") + " TIMESTAMPTZ"
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

    public List<ContractDataSummary> listImportedContractData() {
        ensureDatabaseExists();
        ensureImportCheckpointTableExists();

        try (Connection connection = DriverManager.getConnection(
                settings.primaryJdbcUrl(),
                settings.getUsername(),
                settings.getPassword()
        )) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT " + quoteIdentifier("tableName") + ", " +
                            quoteIdentifier("firstTradeDateTime") + ", " +
                            quoteIdentifier("lastTradeDateTime") +
                            " FROM " + quoteIdentifier(IMPORT_CHECKPOINT_TABLE) +
                            " WHERE status = ?" +
                            " AND " + quoteIdentifier("rowsInserted") + " > 0" +
                            " AND " + quoteIdentifier("firstTradeDateTime") + " IS NOT NULL" +
                            " AND " + quoteIdentifier("lastTradeDateTime") + " IS NOT NULL" +
                            " ORDER BY " + quoteIdentifier("tableName")
            )) {
                statement.setString(1, "COMPLETE");
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<ContractDataSummary> summaries = new ArrayList<>();
                    while (resultSet.next()) {
                        String tableName = resultSet.getString(1);
                        if (!isContractTableName(tableName) || !contractTableExists(connection, tableName)) {
                            continue;
                        }
                        summaries.add(new ContractDataSummary(
                                tableName,
                                resultSet.getTimestamp(2).toInstant().atZone(ZoneOffset.UTC).toLocalDate(),
                                resultSet.getTimestamp(3).toInstant().atZone(ZoneOffset.UTC).toLocalDate()
                        ));
                    }
                    return summaries;
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load available instruments from PostgreSQL metadata", exception);
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

        String copySql = "COPY " + quoteIdentifier(tableName) + " (" +
                quoteIdentifier("tradeDateTime") + ", " +
                quoteIdentifier("priceTicks") + ", " +
                quoteIdentifier("bidPriceTicks") + ", " +
                quoteIdentifier("askPriceTicks") + ", " +
                "quantity, " +
                "side, " +
                quoteIdentifier("numTrades") + ", " +
                quoteIdentifier("sourceFileName") + ", " +
                quoteIdentifier("scidRecordIndex") +
                ") FROM STDIN WITH (FORMAT text, DELIMITER E'\\t')";

        try (Connection connection = DriverManager.getConnection(
                settings.primaryJdbcUrl(),
                settings.getUsername(),
                settings.getPassword()
        );
             StringReader copyData = new StringReader(toCopyText(sourceFileName, trades))) {
            connection.setAutoCommit(false);
            CopyManager copyManager = connection.unwrap(PGConnection.class).getCopyAPI();
            int importedRows = Math.toIntExact(copyManager.copyIn(copySql, copyData));
            updateImportCheckpoint(connection, tableName, sourceFileName, nextRecordIndex, importedRows, trades);
            connection.commit();
            return importedRows;
        } catch (SQLException | IOException exception) {
            throw new IllegalStateException("Could not insert trades into PostgreSQL table '" + tableName + "'", exception);
        }
    }

    public void advanceImportCheckpoint(String tableName, String sourceFileName, long nextRecordIndex) {
        try (Connection connection = DriverManager.getConnection(
                settings.primaryJdbcUrl(),
                settings.getUsername(),
                settings.getPassword()
        );
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE " + quoteIdentifier(IMPORT_CHECKPOINT_TABLE) +
                             " SET " + quoteIdentifier("nextRecordIndex") + " = ?, " +
                             "status = ?, " +
                             quoteIdentifier("updatedAt") + " = ?" +
                             " WHERE " + quoteIdentifier("tableName") + " = ?"
             )) {
            statement.setLong(1, nextRecordIndex);
            statement.setString(2, "IN_PROGRESS");
            statement.setTimestamp(3, Timestamp.from(Instant.now()));
            statement.setString(4, tableName);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not advance PostgreSQL import checkpoint for '" + tableName + "'", exception);
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
                        " SET " + quoteIdentifier("sourceFileName") + " = ?, " +
                        quoteIdentifier("fileSizeBytes") + " = ?, " +
                        quoteIdentifier("lastModifiedMillis") + " = ?, " +
                        quoteIdentifier("nextRecordIndex") + " = ?, " +
                        quoteIdentifier("rowsInserted") + " = ?, " +
                        quoteIdentifier("firstTradeDateTime") + " = ?, " +
                        quoteIdentifier("lastTradeDateTime") + " = ?, " +
                        "status = ?, " +
                        quoteIdentifier("startedAt") + " = ?, " +
                        quoteIdentifier("updatedAt") + " = ?" +
                        " WHERE " + quoteIdentifier("tableName") + " = ?"
        )) {
            Instant now = Instant.now();
            statement.setString(1, sourceFileName);
            statement.setLong(2, fileSizeBytes);
            statement.setLong(3, lastModifiedMillis);
            statement.setLong(4, 1);
            statement.setLong(5, 0);
            statement.setNull(6, Types.TIMESTAMP_WITH_TIMEZONE);
            statement.setNull(7, Types.TIMESTAMP_WITH_TIMEZONE);
            statement.setString(8, "IN_PROGRESS");
            statement.setTimestamp(9, Timestamp.from(now));
            statement.setTimestamp(10, Timestamp.from(now));
            statement.setString(11, tableName);
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

    private boolean isContractTableName(String tableName) {
        return tableName != null && tableName.toUpperCase().matches("[A-Z]{1,3}[FGHJKMNQUVXZ][0-9]{1,2}");
    }

    private void updateImportCheckpoint(
            Connection connection,
            String tableName,
            String sourceFileName,
            long nextRecordIndex,
            int importedRows,
            List<TradeRow> importedTrades
    ) throws SQLException {
        TradeDateTimeBounds bounds = findTradeDateTimeBounds(importedTrades);
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE " + quoteIdentifier(IMPORT_CHECKPOINT_TABLE) +
                        " SET " + quoteIdentifier("nextRecordIndex") + " = ?, " +
                        quoteIdentifier("rowsInserted") + " = " + quoteIdentifier("rowsInserted") + " + ?, " +
                        quoteIdentifier("firstTradeDateTime") + " = CASE WHEN CAST(? AS TIMESTAMPTZ) IS NULL THEN " + quoteIdentifier("firstTradeDateTime") +
                        " WHEN " + quoteIdentifier("firstTradeDateTime") + " IS NULL THEN CAST(? AS TIMESTAMPTZ)" +
                        " ELSE LEAST(" + quoteIdentifier("firstTradeDateTime") + ", CAST(? AS TIMESTAMPTZ)) END, " +
                        quoteIdentifier("lastTradeDateTime") + " = CASE WHEN CAST(? AS TIMESTAMPTZ) IS NULL THEN " + quoteIdentifier("lastTradeDateTime") +
                        " WHEN " + quoteIdentifier("lastTradeDateTime") + " IS NULL THEN CAST(? AS TIMESTAMPTZ)" +
                        " ELSE GREATEST(" + quoteIdentifier("lastTradeDateTime") + ", CAST(? AS TIMESTAMPTZ)) END, " +
                        "status = ?, " +
                        quoteIdentifier("updatedAt") + " = ?" +
                        " WHERE " + quoteIdentifier("tableName") + " = ?"
        )) {
            statement.setLong(1, nextRecordIndex);
            statement.setInt(2, importedRows);
            setNullableTimestamp(statement, 3, bounds.getFirstTradeDateTime());
            setNullableTimestamp(statement, 4, bounds.getFirstTradeDateTime());
            setNullableTimestamp(statement, 5, bounds.getFirstTradeDateTime());
            setNullableTimestamp(statement, 6, bounds.getLastTradeDateTime());
            setNullableTimestamp(statement, 7, bounds.getLastTradeDateTime());
            setNullableTimestamp(statement, 8, bounds.getLastTradeDateTime());
            statement.setString(9, "IN_PROGRESS");
            statement.setTimestamp(10, Timestamp.from(Instant.now()));
            statement.setString(11, tableName);
            statement.executeUpdate();
        }
    }

    private TradeDateTimeBounds findTradeDateTimeBounds(List<TradeRow> trades) {
        if (trades == null || trades.isEmpty()) {
            return TradeDateTimeBounds.empty();
        }
        Instant firstTradeDateTime = null;
        Instant lastTradeDateTime = null;
        for (TradeRow trade : trades) {
            Instant tradeDateTime = trade.getTradeDateTime();
            if (firstTradeDateTime == null || tradeDateTime.isBefore(firstTradeDateTime)) {
                firstTradeDateTime = tradeDateTime;
            }
            if (lastTradeDateTime == null || tradeDateTime.isAfter(lastTradeDateTime)) {
                lastTradeDateTime = tradeDateTime;
            }
        }
        return new TradeDateTimeBounds(firstTradeDateTime, lastTradeDateTime);
    }

    private void setNullableTimestamp(PreparedStatement statement, int index, Instant value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.TIMESTAMP_WITH_TIMEZONE);
            return;
        }
        statement.setTimestamp(index, Timestamp.from(value));
    }

    private static class TradeDateTimeBounds {
        private final Instant firstTradeDateTime;
        private final Instant lastTradeDateTime;

        private TradeDateTimeBounds(Instant firstTradeDateTime, Instant lastTradeDateTime) {
            this.firstTradeDateTime = firstTradeDateTime;
            this.lastTradeDateTime = lastTradeDateTime;
        }

        public static TradeDateTimeBounds empty() {
            return new TradeDateTimeBounds(null, null);
        }

        public Instant getFirstTradeDateTime() {
            return firstTradeDateTime;
        }

        public Instant getLastTradeDateTime() {
            return lastTradeDateTime;
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

    private String toCopyText(String sourceFileName, List<TradeRow> trades) {
        StringBuilder builder = new StringBuilder(trades.size() * 128);
        for (TradeRow trade : trades) {
            appendCopyText(builder, trade.getTradeDateTime().toString());
            builder.append('\t');
            builder.append(trade.getPriceTicks());
            builder.append('\t');
            appendNullableLong(builder, trade.getBidPriceTicks());
            builder.append('\t');
            appendNullableLong(builder, trade.getAskPriceTicks());
            builder.append('\t');
            builder.append(trade.getQuantity());
            builder.append('\t');
            appendNullableInteger(builder, trade.getSide());
            builder.append('\t');
            builder.append(trade.getNumTrades());
            builder.append('\t');
            appendCopyText(builder, sourceFileName);
            builder.append('\t');
            builder.append(trade.getScidRecordIndex());
            builder.append('\n');
        }
        return builder.toString();
    }

    private void appendNullableLong(StringBuilder builder, Long value) {
        if (value == null) {
            builder.append("\\N");
            return;
        }
        builder.append(value);
    }

    private void appendNullableInteger(StringBuilder builder, Integer value) {
        if (value == null) {
            builder.append("\\N");
            return;
        }
        builder.append(value);
    }

    private void appendCopyText(StringBuilder builder, String value) {
        if (value == null) {
            builder.append("\\N");
            return;
        }
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '\\':
                    builder.append("\\\\");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                default:
                    builder.append(character);
                    break;
            }
        }
    }

}
