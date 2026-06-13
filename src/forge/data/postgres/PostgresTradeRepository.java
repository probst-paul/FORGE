package forge.data.postgres;

import forge.data.catalog.ContractDataSummary;
import forge.data.importing.DataImportPlan;
import forge.data.contract.FuturesContractCode;
import forge.data.importing.ImportCheckpoint;
import forge.data.importing.TradeRow;
import forge.data.market.ContractTradeWindow;
import forge.engine.eventstatistics.EventStatisticsDetail;
import forge.engine.eventstatistics.EventStatisticsResult;
import forge.event.EventSide;
import forge.event.MarketEventOccurrence;
import forge.feature.SessionRangeFeature;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PostgresTradeRepository {
    private static final String IMPORT_CHECKPOINT_TABLE = "forge_contract_imports";
    private static final String SESSION_RANGE_TABLE = "forge_session_ranges";
    private static final String MARKET_EVENT_TABLE = "forge_market_events";
    private static final String DERIVED_BUILD_TABLE = "forge_derived_builds";
    private static final String BUILD_TYPE_SESSION_RANGE = "SESSION_RANGE";
    private static final String BUILD_TYPE_MARKET_EVENT = "MARKET_EVENT";
    private static final String WIPE_LOCK_TIMEOUT = "5s";
    private static final String WIPE_STATEMENT_TIMEOUT = "120s";

    private final PostgresDatabaseSettings settings;

    public PostgresTradeRepository(PostgresDatabaseSettings settings) {
        /*
         * Intent: Create a repository for PostgreSQL trade, import, and derived-data storage.
         * Precondition: Database settings must be valid.
         * Returns: A constructed PostgresTradeRepository instance.
         * Postcondition: Repository methods use the supplied database settings.
         */
        if (settings == null) {
            throw new IllegalArgumentException("settings is required");
        }
        this.settings = settings;
    }

    public void ensureDatabaseExists() {
        /*
         * Intent: Create the primary FORGE PostgreSQL database if it does not already exist.
         * Precondition: Maintenance database connection settings must be valid.
         * Returns: Nothing.
         * Postcondition: Primary database exists or an exception explains why it could not be prepared.
         */
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
        /*
         * Intent: Prepare a contract-specific authoritative trade table and migrate older compatible schemas.
         * Precondition: Table name must be a validated contract symbol from upstream import code.
         * Returns: Nothing.
         * Postcondition: Contract table stores prices in tick-space and permits null side values.
         */
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
        /*
         * Intent: Enforce one stored row per SCID record index inside a contract table.
         * Precondition: Contract table must exist.
         * Returns: Nothing.
         * Postcondition: Unique index protects against duplicate imported records.
         */
        try (Connection connection = DriverManager.getConnection(
                settings.primaryJdbcUrl(),
                settings.getUsername(),
                settings.getPassword()
        );
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "DROP INDEX IF EXISTS " + quoteIdentifier(tableName + "_record_uidx")
            );
            statement.executeUpdate(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " + quoteIdentifier(tableName + "_trade_uidx") +
                            " ON " + quoteIdentifier(tableName) +
                            " (" +
                            quoteIdentifier("tradeDateTime") + ", " +
                            quoteIdentifier("priceTicks") + ", " +
                            "COALESCE(" + quoteIdentifier("bidPriceTicks") + ", -9223372036854775808), " +
                            "COALESCE(" + quoteIdentifier("askPriceTicks") + ", -9223372036854775808), " +
                            "quantity, " +
                            "COALESCE(side, -2147483648), " +
                            quoteIdentifier("numTrades") +
                            ")"
            );
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not prepare PostgreSQL unique index for '" + tableName + "'", exception);
        }
    }

    public void ensureImportCheckpointTableExists() {
        /*
         * Intent: Prepare the contract-level import checkpoint/metadata table.
         * Precondition: Primary database must exist.
         * Returns: Nothing.
         * Postcondition: Import metadata can track source file, resume index, row count, and date bounds.
         */
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

    public void ensureDerivedDataTablesExist() {
        /*
         * Intent: Prepare tables used to cache derived session features and condition occurrences.
         * Precondition: Primary database must exist.
         * Returns: Nothing.
         * Postcondition: Derived-data tables and build marker table exist.
         */
        try (Connection connection = DriverManager.getConnection(
                settings.primaryJdbcUrl(),
                settings.getUsername(),
                settings.getPassword()
        );
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS " + quoteIdentifier(SESSION_RANGE_TABLE) + " (" +
                            quoteIdentifier("contractSymbol") + " TEXT NOT NULL, " +
                            quoteIdentifier("sessionDate") + " DATE NOT NULL, " +
                            quoteIdentifier("featureVersion") + " INT NOT NULL, " +
                            quoteIdentifier("overnightLowTicks") + " BIGINT NOT NULL, " +
                            quoteIdentifier("overnightHighTicks") + " BIGINT NOT NULL, " +
                            quoteIdentifier("firstHourLowTicks") + " BIGINT NOT NULL, " +
                            quoteIdentifier("firstHourHighTicks") + " BIGINT NOT NULL, " +
                            quoteIdentifier("rthLowTicks") + " BIGINT NOT NULL, " +
                            quoteIdentifier("rthHighTicks") + " BIGINT NOT NULL, " +
                            quoteIdentifier("overnightVolume") + " BIGINT NOT NULL DEFAULT 0, " +
                            quoteIdentifier("firstHourVolume") + " BIGINT NOT NULL DEFAULT 0, " +
                            quoteIdentifier("rthVolume") + " BIGINT NOT NULL DEFAULT 0, " +
                            quoteIdentifier("overnightTradeCount") + " BIGINT NOT NULL DEFAULT 0, " +
                            quoteIdentifier("firstHourTradeCount") + " BIGINT NOT NULL DEFAULT 0, " +
                            quoteIdentifier("rthTradeCount") + " BIGINT NOT NULL DEFAULT 0, " +
                            quoteIdentifier("createdAt") + " TIMESTAMPTZ NOT NULL, " +
                            "PRIMARY KEY (" + quoteIdentifier("contractSymbol") + ", " +
                            quoteIdentifier("sessionDate") + ", " +
                            quoteIdentifier("featureVersion") + ")" +
                            ")"
            );
            addSessionRangeMeasurementColumns(statement);
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS " + quoteIdentifier(MARKET_EVENT_TABLE) + " (" +
                            quoteIdentifier("contractSymbol") + " TEXT NOT NULL, " +
                            quoteIdentifier("sessionDate") + " DATE NOT NULL, " +
                            quoteIdentifier("eventName") + " TEXT NOT NULL, " +
                            quoteIdentifier("eventVersion") + " INT NOT NULL, " +
                            "side TEXT NOT NULL, " +
                            quoteIdentifier("eventTime") + " TIMESTAMPTZ NOT NULL, " +
                            quoteIdentifier("eventPriceTicks") + " BIGINT NOT NULL, " +
                            quoteIdentifier("createdAt") + " TIMESTAMPTZ NOT NULL, " +
                            "PRIMARY KEY (" + quoteIdentifier("contractSymbol") + ", " +
                            quoteIdentifier("sessionDate") + ", " +
                            quoteIdentifier("eventName") + ", " +
                            quoteIdentifier("eventVersion") + ")" +
                            ")"
            );
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS " + quoteIdentifier(DERIVED_BUILD_TABLE) + " (" +
                            quoteIdentifier("buildType") + " TEXT NOT NULL, " +
                            "name TEXT NOT NULL, " +
                            quoteIdentifier("contractSymbol") + " TEXT NOT NULL, " +
                            quoteIdentifier("startDate") + " DATE NOT NULL, " +
                            quoteIdentifier("endDate") + " DATE NOT NULL, " +
                            quoteIdentifier("builtAt") + " TIMESTAMPTZ NOT NULL, " +
                            "PRIMARY KEY (" + quoteIdentifier("buildType") + ", name, " +
                            quoteIdentifier("contractSymbol") + ", " +
                            quoteIdentifier("startDate") + ", " +
                            quoteIdentifier("endDate") + ")" +
                            ")"
            );
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not prepare PostgreSQL derived data tables", exception);
        }
    }

    public DataImportPlan planImport(String contractSymbol, String tableName) {
        /*
         * Intent: Inspect existing table/checkpoint state before deciding whether an import should rebuild.
         * Precondition: Contract symbol and table name must describe the same contract.
         * Returns: DataImportPlan with table existence, row count, source file, and status.
         * Postcondition: No contract trade rows are changed.
         */
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
        /*
         * Intent: List completed imported contract tables from metadata for catalog construction.
         * Precondition: Database and import checkpoint table must be available.
         * Returns: Contract summaries with imported date bounds.
         * Postcondition: Incomplete or invalid contract table metadata is ignored.
         */
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

    private void addSessionRangeMeasurementColumns(Statement statement) throws SQLException {
        /*
         * Intent: Migrate older session-range tables with range-only columns to include activity measurements.
         * Precondition: Session range table exists or was just created.
         * Returns: Nothing.
         * Postcondition: Volume/trade-count columns are available for joins and aggregate event statistics.
         */
        addBigIntColumn(statement, SESSION_RANGE_TABLE, "overnightVolume");
        addBigIntColumn(statement, SESSION_RANGE_TABLE, "firstHourVolume");
        addBigIntColumn(statement, SESSION_RANGE_TABLE, "rthVolume");
        addBigIntColumn(statement, SESSION_RANGE_TABLE, "overnightTradeCount");
        addBigIntColumn(statement, SESSION_RANGE_TABLE, "firstHourTradeCount");
        addBigIntColumn(statement, SESSION_RANGE_TABLE, "rthTradeCount");
    }

    private void addBigIntColumn(Statement statement, String tableName, String columnName) throws SQLException {
        statement.executeUpdate(
                "ALTER TABLE " + quoteIdentifier(tableName) +
                        " ADD COLUMN IF NOT EXISTS " + quoteIdentifier(columnName) + " BIGINT NOT NULL DEFAULT 0"
        );
    }

    public int wipeDatabase() {
        /*
         * Intent: Remove all FORGE-owned imported and derived-data tables from the configured database.
         * Precondition: Caller must have confirmed this destructive admin action.
         * Returns: Number of tables dropped.
         * Postcondition: Contract tables and forge_* metadata/cache tables are removed from the current schema.
         */
        ensureDatabaseExists();

        try (Connection connection = DriverManager.getConnection(
                settings.primaryJdbcUrl(),
                settings.getUsername(),
                settings.getPassword()
        );
             Statement statement = connection.createStatement()) {
            configureWipeSession(statement);
            terminateOtherCurrentDatabaseSessions(statement);
            List<String> tableNames = listForgeOwnedTables(connection);
            for (String tableName : tableNames) {
                statement.executeUpdate("DROP TABLE IF EXISTS " + quoteIdentifier(tableName) + " CASCADE");
            }
            return tableNames.size();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not wipe PostgreSQL database '" + settings.getDatabaseName() + "'", exception);
        }
    }

    public DataImportPlan planImport(
            String contractSymbol,
            FuturesContractCode contractCode,
            String tableName,
            Instant fileFirstTradeDateTime,
            Instant fileLastTradeDateTime
    ) {
        /*
         * Intent: Inspect existing contract data and selected file overlap before importing.
         * Precondition: Contract metadata must be valid and file bounds may be null for an empty import file.
         * Returns: DataImportPlan with contract identity, stored coverage, file coverage, and overlap count.
         * Postcondition: No trade rows are changed.
         */
        ensureImportCheckpointTableExists();

        try (Connection connection = DriverManager.getConnection(
                settings.primaryJdbcUrl(),
                settings.getUsername(),
                settings.getPassword()
        )) {
            boolean tableExists = contractTableExists(connection, tableName);
            long existingRows = tableExists ? countRows(connection, tableName) : 0;
            TradeDateTimeBounds existingBounds = tableExists ? findTableDateTimeBounds(connection, tableName) : TradeDateTimeBounds.empty();
            long overlappingRows = tableExists
                    ? countRowsBetween(connection, tableName, fileFirstTradeDateTime, fileLastTradeDateTime)
                    : 0;
            CheckpointMetadata metadata = findCheckpointMetadata(connection, tableName);
            return new DataImportPlan(
                    contractSymbol,
                    contractCode,
                    tableName,
                    tableExists,
                    existingRows,
                    existingBounds.getFirstTradeDateTime(),
                    existingBounds.getLastTradeDateTime(),
                    fileFirstTradeDateTime,
                    fileLastTradeDateTime,
                    overlappingRows,
                    metadata == null ? null : metadata.getSourceFileName(),
                    metadata == null ? null : metadata.getStatus()
            );
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not inspect PostgreSQL import state for '" + tableName + "'", exception);
        }
    }

    private void configureWipeSession(Statement statement) throws SQLException {
        /*
         * Intent: Prevent database wipe from waiting forever on PostgreSQL locks or long-running statements.
         * Precondition: statement must belong to the database connection used for the wipe.
         * Returns: Nothing.
         * Postcondition: DROP statements fail with a useful exception instead of hanging indefinitely.
         */
        statement.execute("SET lock_timeout = '" + WIPE_LOCK_TIMEOUT + "'");
        statement.execute("SET statement_timeout = '" + WIPE_STATEMENT_TIMEOUT + "'");
    }

    private void terminateOtherCurrentDatabaseSessions(Statement statement) throws SQLException {
        /*
         * Intent: Release locks held by other sessions connected to the same database before dropping tables.
         * Precondition: statement must be connected to the primary FORGE database.
         * Returns: Nothing.
         * Postcondition: Other sessions are terminated where PostgreSQL permissions allow it.
         */
        try {
            statement.execute(
                    "SELECT pg_terminate_backend(pid) " +
                            "FROM pg_stat_activity " +
                            "WHERE datname = current_database() " +
                            "AND pid <> pg_backend_pid()"
            );
        } catch (SQLException exception) {
            // Some PostgreSQL users cannot terminate other sessions. The lock timeout still prevents an indefinite wait.
        }
    }

    public ImportCheckpoint prepareImportCheckpoint(
            String tableName,
            String sourceFileName,
            long fileSizeBytes,
            long lastModifiedMillis,
            boolean rebuildExistingContract
    ) {
        /*
         * Intent: Prepare import metadata for scanning the selected file from the beginning.
         * Precondition: Contract table/checkpoint table must exist and source metadata must describe selected file.
         * Returns: ImportCheckpoint indicating the next SCID record index to read.
         * Postcondition: Existing trade rows are not removed by checkpoint preparation.
         */
        ensureImportCheckpointTableExists();

        try (Connection connection = DriverManager.getConnection(
                settings.primaryJdbcUrl(),
                settings.getUsername(),
                settings.getPassword()
        )) {
            ImportCheckpoint existingCheckpoint = findImportCheckpoint(connection, tableName);

            if (existingCheckpoint == null) {
                insertImportCheckpoint(connection, tableName, sourceFileName, fileSizeBytes, lastModifiedMillis);
                return new ImportCheckpoint(tableName, sourceFileName, 1);
            }

            resetImportCheckpoint(connection, tableName, sourceFileName, fileSizeBytes, lastModifiedMillis);
            return new ImportCheckpoint(tableName, sourceFileName, 1);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load PostgreSQL import checkpoint", exception);
        }
    }

    public void markImportComplete(String tableName, String sourceFileName) {
        /*
         * Intent: Mark a contract import checkpoint as complete after all SCID records are processed.
         * Precondition: Checkpoint row must exist for the table/source.
         * Returns: Nothing.
         * Postcondition: Import status is COMPLETE and update timestamp is refreshed.
         */
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
        /*
         * Intent: Insert one batch of trades with PostgreSQL COPY and atomically advance the import checkpoint.
         * Precondition: Trades must belong to the target contract table and nextRecordIndex must follow the batch.
         * Returns: Number of rows inserted by COPY.
         * Postcondition: Trade rows and checkpoint update commit together or fail together.
         */
        if (trades == null || trades.isEmpty()) {
            return 0;
        }

        String temporaryTableName = "forge_import_batch_" + Long.toUnsignedString(System.nanoTime());
        String copySql = "COPY " + quoteIdentifier(temporaryTableName) + " (" +
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
            createTemporaryImportTable(connection, temporaryTableName);
            CopyManager copyManager = connection.unwrap(PGConnection.class).getCopyAPI();
            copyManager.copyIn(copySql, copyData);
            int importedRows = insertFromTemporaryImportTable(connection, tableName, temporaryTableName);
            updateImportCheckpoint(connection, tableName, sourceFileName, nextRecordIndex, importedRows, trades);
            connection.commit();
            return importedRows;
        } catch (SQLException | IOException exception) {
            throw new IllegalStateException("Could not insert trades into PostgreSQL table '" + tableName + "'", exception);
        }
    }

    public long deleteRowsBetween(String tableName, Instant firstTradeDateTime, Instant lastTradeDateTime) {
        /*
         * Intent: Delete stored contract rows that overlap the selected import file range.
         * Precondition: Table must exist; bounds may be null for an empty file range.
         * Returns: Number of deleted rows.
         * Postcondition: Only rows between the supplied timestamps are removed.
         */
        if (firstTradeDateTime == null || lastTradeDateTime == null) {
            return 0;
        }
        try (Connection connection = DriverManager.getConnection(
                settings.primaryJdbcUrl(),
                settings.getUsername(),
                settings.getPassword()
        );
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM " + quoteIdentifier(tableName) +
                             " WHERE " + quoteIdentifier("tradeDateTime") + " >= ?" +
                             " AND " + quoteIdentifier("tradeDateTime") + " <= ?"
             )) {
            statement.setTimestamp(1, Timestamp.from(firstTradeDateTime));
            statement.setTimestamp(2, Timestamp.from(lastTradeDateTime));
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not delete overlapping PostgreSQL rows from '" + tableName + "'", exception);
        }
    }

    public void advanceImportCheckpoint(String tableName, String sourceFileName, long nextRecordIndex) {
        /*
         * Intent: Advance import metadata when a batch produces no stored rows after filtering.
         * Precondition: Checkpoint row must exist for the table.
         * Returns: Nothing.
         * Postcondition: Resume point moves forward even though row count is unchanged.
         */
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

    public boolean areSessionRangesBuilt(List<ContractTradeWindow> windows) {
        /*
         * Intent: Check whether cached session range features exist for all selected windows.
         * Precondition: Windows may be empty; empty windows are treated as already built.
         * Returns: True when all requested windows have build markers.
         * Postcondition: Database data is unchanged.
         */
        return areDerivedRowsBuilt(BUILD_TYPE_SESSION_RANGE, sessionRangeBuildName(), windows);
    }

    public List<SessionRangeFeature> loadSessionRanges(List<ContractTradeWindow> windows) {
        /*
         * Intent: Load cached session range features for selected contract windows.
         * Precondition: Derived-data tables must be available; empty windows are allowed.
         * Returns: Immutable list of matching SessionRangeFeature rows.
         * Postcondition: Database data is unchanged.
         */
        ensureDerivedDataTablesExist();
        if (windows == null || windows.isEmpty()) {
            return Collections.emptyList();
        }
        List<SessionRangeFeature> features = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(
                settings.primaryJdbcUrl(),
                settings.getUsername(),
                settings.getPassword()
        )) {
            for (ContractTradeWindow window : windows) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT " + quoteIdentifier("contractSymbol") + ", " +
                                quoteIdentifier("sessionDate") + ", " +
                                quoteIdentifier("overnightLowTicks") + ", " +
                                quoteIdentifier("overnightHighTicks") + ", " +
                                quoteIdentifier("firstHourLowTicks") + ", " +
                                quoteIdentifier("firstHourHighTicks") + ", " +
                                quoteIdentifier("rthLowTicks") + ", " +
                                quoteIdentifier("rthHighTicks") + ", " +
                                quoteIdentifier("overnightVolume") + ", " +
                                quoteIdentifier("firstHourVolume") + ", " +
                                quoteIdentifier("rthVolume") + ", " +
                                quoteIdentifier("overnightTradeCount") + ", " +
                                quoteIdentifier("firstHourTradeCount") + ", " +
                                quoteIdentifier("rthTradeCount") +
                                " FROM " + quoteIdentifier(SESSION_RANGE_TABLE) +
                                " WHERE " + quoteIdentifier("contractSymbol") + " = ?" +
                                " AND " + quoteIdentifier("featureVersion") + " = ?" +
                                " AND " + quoteIdentifier("sessionDate") + " >= ?" +
                                " AND " + quoteIdentifier("sessionDate") + " <= ?" +
                                " ORDER BY " + quoteIdentifier("contractSymbol") + ", " + quoteIdentifier("sessionDate")
                )) {
                    statement.setString(1, window.getContractSymbol());
                    statement.setInt(2, SessionRangeFeature.FEATURE_VERSION);
                    statement.setDate(3, Date.valueOf(window.getStartDate()));
                    statement.setDate(4, Date.valueOf(window.getEndDate()));
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            features.add(new SessionRangeFeature(
                                    resultSet.getString(1),
                                    resultSet.getDate(2).toLocalDate(),
                                    resultSet.getLong(3),
                                    resultSet.getLong(4),
                                    resultSet.getLong(5),
                                    resultSet.getLong(6),
                                    resultSet.getLong(7),
                                    resultSet.getLong(8),
                                    resultSet.getLong(9),
                                    resultSet.getLong(10),
                                    resultSet.getLong(11),
                                    resultSet.getLong(12),
                                    resultSet.getLong(13),
                                    resultSet.getLong(14)
                            ));
                        }
                    }
                }
            }
            return Collections.unmodifiableList(features);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load session range features from PostgreSQL", exception);
        }
    }

    public void saveSessionRanges(Collection<SessionRangeFeature> sessionRangeFeatures) {
        /*
         * Intent: Upsert computed session range features into the derived-data cache.
         * Precondition: Feature collection may be null/empty; non-empty features must be valid.
         * Returns: Nothing.
         * Postcondition: Existing feature rows for the same contract/session/version are updated.
         */
        ensureDerivedDataTablesExist();
        if (sessionRangeFeatures == null || sessionRangeFeatures.isEmpty()) {
            return;
        }
        try (Connection connection = DriverManager.getConnection(
                settings.primaryJdbcUrl(),
                settings.getUsername(),
                settings.getPassword()
        );
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO " + quoteIdentifier(SESSION_RANGE_TABLE) + " (" +
                             quoteIdentifier("contractSymbol") + ", " +
                             quoteIdentifier("sessionDate") + ", " +
                             quoteIdentifier("featureVersion") + ", " +
                             quoteIdentifier("overnightLowTicks") + ", " +
                             quoteIdentifier("overnightHighTicks") + ", " +
                             quoteIdentifier("firstHourLowTicks") + ", " +
                             quoteIdentifier("firstHourHighTicks") + ", " +
                             quoteIdentifier("rthLowTicks") + ", " +
                             quoteIdentifier("rthHighTicks") + ", " +
                             quoteIdentifier("overnightVolume") + ", " +
                             quoteIdentifier("firstHourVolume") + ", " +
                             quoteIdentifier("rthVolume") + ", " +
                             quoteIdentifier("overnightTradeCount") + ", " +
                             quoteIdentifier("firstHourTradeCount") + ", " +
                             quoteIdentifier("rthTradeCount") + ", " +
                             quoteIdentifier("createdAt") +
                             ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" +
                             " ON CONFLICT (" + quoteIdentifier("contractSymbol") + ", " +
                             quoteIdentifier("sessionDate") + ", " +
                             quoteIdentifier("featureVersion") + ") DO UPDATE SET " +
                             quoteIdentifier("overnightLowTicks") + " = EXCLUDED." + quoteIdentifier("overnightLowTicks") + ", " +
                             quoteIdentifier("overnightHighTicks") + " = EXCLUDED." + quoteIdentifier("overnightHighTicks") + ", " +
                             quoteIdentifier("firstHourLowTicks") + " = EXCLUDED." + quoteIdentifier("firstHourLowTicks") + ", " +
                             quoteIdentifier("firstHourHighTicks") + " = EXCLUDED." + quoteIdentifier("firstHourHighTicks") + ", " +
                             quoteIdentifier("rthLowTicks") + " = EXCLUDED." + quoteIdentifier("rthLowTicks") + ", " +
                             quoteIdentifier("rthHighTicks") + " = EXCLUDED." + quoteIdentifier("rthHighTicks") + ", " +
                             quoteIdentifier("overnightVolume") + " = EXCLUDED." + quoteIdentifier("overnightVolume") + ", " +
                             quoteIdentifier("firstHourVolume") + " = EXCLUDED." + quoteIdentifier("firstHourVolume") + ", " +
                             quoteIdentifier("rthVolume") + " = EXCLUDED." + quoteIdentifier("rthVolume") + ", " +
                             quoteIdentifier("overnightTradeCount") + " = EXCLUDED." + quoteIdentifier("overnightTradeCount") + ", " +
                             quoteIdentifier("firstHourTradeCount") + " = EXCLUDED." + quoteIdentifier("firstHourTradeCount") + ", " +
                             quoteIdentifier("rthTradeCount") + " = EXCLUDED." + quoteIdentifier("rthTradeCount")
             )) {
            for (SessionRangeFeature feature : sessionRangeFeatures) {
                statement.setString(1, feature.getContractSymbol());
                statement.setDate(2, Date.valueOf(feature.getSessionDate()));
                statement.setInt(3, SessionRangeFeature.FEATURE_VERSION);
                statement.setLong(4, feature.getOvernightLowTicks());
                statement.setLong(5, feature.getOvernightHighTicks());
                statement.setLong(6, feature.getFirstHourLowTicks());
                statement.setLong(7, feature.getFirstHourHighTicks());
                statement.setLong(8, feature.getRthLowTicks());
                statement.setLong(9, feature.getRthHighTicks());
                statement.setLong(10, feature.getOvernightVolume());
                statement.setLong(11, feature.getFirstHourVolume());
                statement.setLong(12, feature.getRthVolume());
                statement.setLong(13, feature.getOvernightTradeCount());
                statement.setLong(14, feature.getFirstHourTradeCount());
                statement.setLong(15, feature.getRthTradeCount());
                statement.setTimestamp(16, Timestamp.from(Instant.now()));
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save session range features to PostgreSQL", exception);
        }
    }

    public void markSessionRangesBuilt(List<ContractTradeWindow> windows) {
        /*
         * Intent: Mark session range features as built for selected windows.
         * Precondition: Windows should describe the derived-data coverage that was persisted.
         * Returns: Nothing.
         * Postcondition: Future build plans can skip session ranges unless rebuild is requested.
         */
        markDerivedRowsBuilt(BUILD_TYPE_SESSION_RANGE, sessionRangeBuildName(), windows);
    }

    public void clearSessionRanges(List<ContractTradeWindow> windows) {
        /*
         * Intent: Delete cached session range features and build markers for selected windows.
         * Precondition: Windows may be null/empty.
         * Returns: Nothing.
         * Postcondition: Selected session range cache is removed when windows are supplied.
         */
        ensureDerivedDataTablesExist();
        if (windows == null || windows.isEmpty()) {
            return;
        }
        try (Connection connection = DriverManager.getConnection(
                settings.primaryJdbcUrl(),
                settings.getUsername(),
                settings.getPassword()
        )) {
            for (ContractTradeWindow window : windows) {
                deleteSessionRanges(connection, window);
                deleteDerivedBuild(connection, BUILD_TYPE_SESSION_RANGE, sessionRangeBuildName(), window);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not clear PostgreSQL session range features", exception);
        }
    }

    public boolean areMarketEventOccurrencesBuilt(List<ContractTradeWindow> windows, String eventName) {
        /*
         * Intent: Check whether cached market event occurrences exist for all selected windows.
         * Precondition: Event name should identify a supported derived condition.
         * Returns: True when all requested windows have build markers for the event.
         * Postcondition: Database data is unchanged.
         */
        return areDerivedRowsBuilt(BUILD_TYPE_MARKET_EVENT, eventName, windows);
    }

    public List<MarketEventOccurrence> loadMarketEventOccurrences(List<ContractTradeWindow> windows, String eventName) {
        /*
         * Intent: Load cached market event occurrences for selected windows and event name.
         * Precondition: Derived-data tables must be available; empty windows are allowed.
         * Returns: Immutable list of matching occurrence rows.
         * Postcondition: Database data is unchanged.
         */
        ensureDerivedDataTablesExist();
        if (windows == null || windows.isEmpty()) {
            return Collections.emptyList();
        }
        List<MarketEventOccurrence> events = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(
                settings.primaryJdbcUrl(),
                settings.getUsername(),
                settings.getPassword()
        )) {
            for (ContractTradeWindow window : windows) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT " + quoteIdentifier("contractSymbol") + ", " +
                                quoteIdentifier("sessionDate") + ", " +
                                quoteIdentifier("eventName") + ", " +
                                quoteIdentifier("eventVersion") + ", " +
                                "side, " +
                                quoteIdentifier("eventTime") + ", " +
                                quoteIdentifier("eventPriceTicks") +
                                " FROM " + quoteIdentifier(MARKET_EVENT_TABLE) +
                                " WHERE " + quoteIdentifier("contractSymbol") + " = ?" +
                                " AND " + quoteIdentifier("eventName") + " = ?" +
                                " AND " + quoteIdentifier("sessionDate") + " >= ?" +
                                " AND " + quoteIdentifier("sessionDate") + " <= ?" +
                                " ORDER BY " + quoteIdentifier("contractSymbol") + ", " + quoteIdentifier("sessionDate")
                )) {
                    statement.setString(1, window.getContractSymbol());
                    statement.setString(2, eventName);
                    statement.setDate(3, Date.valueOf(window.getStartDate()));
                    statement.setDate(4, Date.valueOf(window.getEndDate()));
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            events.add(new MarketEventOccurrence(
                                    resultSet.getString(1),
                                    resultSet.getDate(2).toLocalDate(),
                                    resultSet.getString(3),
                                    resultSet.getInt(4),
                                    parseEventSide(resultSet.getString(5)),
                                    resultSet.getTimestamp(6).toInstant(),
                                    resultSet.getLong(7)
                            ));
                        }
                    }
                }
            }
            return Collections.unmodifiableList(events);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load market events from PostgreSQL", exception);
        }
    }

    public void saveMarketEventOccurrences(Collection<MarketEventOccurrence> marketEvents) {
        /*
         * Intent: Upsert computed market event occurrences into the derived-data cache.
         * Precondition: Event collection may be null/empty; non-empty events must be valid.
         * Returns: Nothing.
         * Postcondition: Existing event rows for the same contract/session/name/version are updated.
         */
        ensureDerivedDataTablesExist();
        if (marketEvents == null || marketEvents.isEmpty()) {
            return;
        }
        try (Connection connection = DriverManager.getConnection(
                settings.primaryJdbcUrl(),
                settings.getUsername(),
                settings.getPassword()
        );
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO " + quoteIdentifier(MARKET_EVENT_TABLE) + " (" +
                             quoteIdentifier("contractSymbol") + ", " +
                             quoteIdentifier("sessionDate") + ", " +
                             quoteIdentifier("eventName") + ", " +
                             quoteIdentifier("eventVersion") + ", " +
                             "side, " +
                             quoteIdentifier("eventTime") + ", " +
                             quoteIdentifier("eventPriceTicks") + ", " +
                             quoteIdentifier("createdAt") +
                             ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)" +
                             " ON CONFLICT (" + quoteIdentifier("contractSymbol") + ", " +
                             quoteIdentifier("sessionDate") + ", " +
                             quoteIdentifier("eventName") + ", " +
                             quoteIdentifier("eventVersion") + ") DO UPDATE SET " +
                             "side = EXCLUDED.side, " +
                             quoteIdentifier("eventTime") + " = EXCLUDED." + quoteIdentifier("eventTime") + ", " +
                             quoteIdentifier("eventPriceTicks") + " = EXCLUDED." + quoteIdentifier("eventPriceTicks")
             )) {
            for (MarketEventOccurrence event : marketEvents) {
                statement.setString(1, event.getContractSymbol());
                statement.setDate(2, Date.valueOf(event.getSessionDate()));
                statement.setString(3, event.getEventName());
                statement.setInt(4, event.getEventVersion());
                statement.setString(5, event.getSide().name());
                statement.setTimestamp(6, Timestamp.from(event.getEventTime()));
                statement.setLong(7, event.getEventPriceTicks());
                statement.setTimestamp(8, Timestamp.from(Instant.now()));
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save market events to PostgreSQL", exception);
        }
    }

    public void markMarketEventOccurrencesBuilt(List<ContractTradeWindow> windows, String eventName) {
        /*
         * Intent: Mark market event occurrences as built for selected windows and event name.
         * Precondition: Windows and event name should describe the data that was persisted.
         * Returns: Nothing.
         * Postcondition: Future build plans/statistics can use cached event occurrences.
         */
        markDerivedRowsBuilt(BUILD_TYPE_MARKET_EVENT, eventName, windows);
    }

    public void clearMarketEventOccurrences(List<ContractTradeWindow> windows, String eventName) {
        /*
         * Intent: Delete cached market event occurrences and build markers for selected windows.
         * Precondition: Windows may be null/empty; event name should match the cached event family.
         * Returns: Nothing.
         * Postcondition: Selected market-event cache is removed when windows are supplied.
         */
        ensureDerivedDataTablesExist();
        if (windows == null || windows.isEmpty()) {
            return;
        }
        try (Connection connection = DriverManager.getConnection(
                settings.primaryJdbcUrl(),
                settings.getUsername(),
                settings.getPassword()
        )) {
            for (ContractTradeWindow window : windows) {
                deleteMarketEventOccurrences(connection, eventName, window);
                deleteDerivedBuild(connection, BUILD_TYPE_MARKET_EVENT, eventName, window);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not clear PostgreSQL market events", exception);
        }
    }

    public List<EventStatisticsDetail> loadEventStatisticsDetails(
            List<ContractTradeWindow> windows,
            String eventName
    ) {
        /*
         * Intent: Load event occurrence details joined to derived session range/activity context.
         * Precondition: Windows and event name must identify built derived event data.
         * Returns: Detail rows ordered by contract, session date, and event time.
         * Postcondition: Database data is unchanged.
         */
        ensureDerivedDataTablesExist();
        if (windows == null || windows.isEmpty()) {
            return Collections.emptyList();
        }
        List<EventStatisticsDetail> details = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(
                settings.primaryJdbcUrl(),
                settings.getUsername(),
                settings.getPassword()
        )) {
            for (ContractTradeWindow window : windows) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT e." + quoteIdentifier("contractSymbol") + ", " +
                                "e." + quoteIdentifier("sessionDate") + ", " +
                                "e." + quoteIdentifier("eventName") + ", " +
                                "e.side, " +
                                "e." + quoteIdentifier("eventTime") + ", " +
                                "e." + quoteIdentifier("eventPriceTicks") + ", " +
                                "r." + quoteIdentifier("overnightLowTicks") + ", " +
                                "r." + quoteIdentifier("overnightHighTicks") + ", " +
                                "r." + quoteIdentifier("firstHourLowTicks") + ", " +
                                "r." + quoteIdentifier("firstHourHighTicks") + ", " +
                                "r." + quoteIdentifier("rthLowTicks") + ", " +
                                "r." + quoteIdentifier("rthHighTicks") + ", " +
                                "r." + quoteIdentifier("overnightVolume") + ", " +
                                "r." + quoteIdentifier("firstHourVolume") + ", " +
                                "r." + quoteIdentifier("rthVolume") + ", " +
                                "r." + quoteIdentifier("overnightTradeCount") + ", " +
                                "r." + quoteIdentifier("firstHourTradeCount") + ", " +
                                "r." + quoteIdentifier("rthTradeCount") +
                                " FROM " + quoteIdentifier(MARKET_EVENT_TABLE) + " e" +
                                " JOIN " + quoteIdentifier(SESSION_RANGE_TABLE) + " r" +
                                " ON e." + quoteIdentifier("contractSymbol") + " = r." + quoteIdentifier("contractSymbol") +
                                " AND e." + quoteIdentifier("sessionDate") + " = r." + quoteIdentifier("sessionDate") +
                                " WHERE e." + quoteIdentifier("contractSymbol") + " = ?" +
                                " AND e." + quoteIdentifier("eventName") + " = ?" +
                                " AND r." + quoteIdentifier("featureVersion") + " = ?" +
                                " AND e." + quoteIdentifier("sessionDate") + " >= ?" +
                                " AND e." + quoteIdentifier("sessionDate") + " <= ?" +
                                " ORDER BY e." + quoteIdentifier("contractSymbol") + ", " +
                                "e." + quoteIdentifier("sessionDate") + ", " +
                                "e." + quoteIdentifier("eventTime")
                )) {
                    statement.setString(1, window.getContractSymbol());
                    statement.setString(2, eventName);
                    statement.setInt(3, SessionRangeFeature.FEATURE_VERSION);
                    statement.setDate(4, Date.valueOf(window.getStartDate()));
                    statement.setDate(5, Date.valueOf(window.getEndDate()));
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            details.add(new EventStatisticsDetail(
                                    resultSet.getString(1),
                                    resultSet.getDate(2).toLocalDate(),
                                    resultSet.getString(3),
                                    parseEventSide(resultSet.getString(4)),
                                    resultSet.getTimestamp(5).toInstant(),
                                    resultSet.getLong(6),
                                    resultSet.getLong(7),
                                    resultSet.getLong(8),
                                    resultSet.getLong(9),
                                    resultSet.getLong(10),
                                    resultSet.getLong(11),
                                    resultSet.getLong(12),
                                    resultSet.getLong(13),
                                    resultSet.getLong(14),
                                    resultSet.getLong(15),
                                    resultSet.getLong(16),
                                    resultSet.getLong(17),
                                    resultSet.getLong(18)
                            ));
                        }
                    }
                }
            }
            return Collections.unmodifiableList(details);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load event statistics details from PostgreSQL", exception);
        }
    }

    public List<EventStatisticsResult> loadEventStatisticsContractResults(
            List<ContractTradeWindow> windows,
            String eventName
    ) {
        /*
         * Intent: Aggregate event counts and supporting session measurements in PostgreSQL.
         * Precondition: Session ranges and event occurrences should be built for selected windows.
         * Returns: Contract-scoped EventStatisticsResult rows.
         * Postcondition: Database data is unchanged.
         */
        ensureDerivedDataTablesExist();
        if (windows == null || windows.isEmpty()) {
            return Collections.emptyList();
        }
        List<EventStatisticsResult> results = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(
                settings.primaryJdbcUrl(),
                settings.getUsername(),
                settings.getPassword()
        )) {
            for (ContractTradeWindow window : windows) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT r." + quoteIdentifier("contractSymbol") + ", " +
                                "COUNT(*) AS sessionsAnalyzed, " +
                                "SUM(CASE WHEN e.side IN ('HIGH', 'LONG') THEN 1 ELSE 0 END) AS highEvents, " +
                                "SUM(CASE WHEN e.side IN ('LOW', 'SHORT') THEN 1 ELSE 0 END) AS lowEvents, " +
                                "AVG(r." + quoteIdentifier("overnightHighTicks") + " - r." + quoteIdentifier("overnightLowTicks") + ") AS avgOvernightRangeTicks, " +
                                "AVG(r." + quoteIdentifier("firstHourHighTicks") + " - r." + quoteIdentifier("firstHourLowTicks") + ") AS avgFirstHourRangeTicks, " +
                                "AVG(r." + quoteIdentifier("rthHighTicks") + " - r." + quoteIdentifier("rthLowTicks") + ") AS avgRthRangeTicks, " +
                                "AVG(r." + quoteIdentifier("overnightVolume") + ") AS avgOvernightVolume, " +
                                "AVG(r." + quoteIdentifier("firstHourVolume") + ") AS avgFirstHourVolume, " +
                                "AVG(r." + quoteIdentifier("rthVolume") + ") AS avgRthVolume" +
                                " FROM " + quoteIdentifier(SESSION_RANGE_TABLE) + " r" +
                                " LEFT JOIN " + quoteIdentifier(MARKET_EVENT_TABLE) + " e" +
                                " ON e." + quoteIdentifier("contractSymbol") + " = r." + quoteIdentifier("contractSymbol") +
                                " AND e." + quoteIdentifier("sessionDate") + " = r." + quoteIdentifier("sessionDate") +
                                " AND e." + quoteIdentifier("eventName") + " = ?" +
                                " WHERE r." + quoteIdentifier("contractSymbol") + " = ?" +
                                " AND r." + quoteIdentifier("featureVersion") + " = ?" +
                                " AND r." + quoteIdentifier("sessionDate") + " >= ?" +
                                " AND r." + quoteIdentifier("sessionDate") + " <= ?" +
                                " GROUP BY r." + quoteIdentifier("contractSymbol") +
                                " ORDER BY r." + quoteIdentifier("contractSymbol")
                )) {
                    statement.setString(1, eventName);
                    statement.setString(2, window.getContractSymbol());
                    statement.setInt(3, SessionRangeFeature.FEATURE_VERSION);
                    statement.setDate(4, Date.valueOf(window.getStartDate()));
                    statement.setDate(5, Date.valueOf(window.getEndDate()));
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            results.add(new EventStatisticsResult(
                                    resultSet.getString(1),
                                    eventName,
                                    resultSet.getLong(2),
                                    resultSet.getLong(3),
                                    resultSet.getLong(4),
                                    resultSet.getDouble(5),
                                    resultSet.getDouble(6),
                                    resultSet.getDouble(7),
                                    resultSet.getDouble(8),
                                    resultSet.getDouble(9),
                                    resultSet.getDouble(10)
                            ));
                        }
                    }
                }
            }
            return Collections.unmodifiableList(results);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not aggregate event statistics from PostgreSQL", exception);
        }
    }

    public String getDatabaseName() {
        return settings.getDatabaseName();
    }

    private String sessionRangeBuildName() {
        /*
         * Intent: Create the build-marker name for the active session range feature schema.
         * Precondition: SessionRangeFeature constants must describe the current derived-data version.
         * Returns: Stable build name containing feature name and version.
         * Postcondition: Repository state is unchanged.
         */
        return SessionRangeFeature.FEATURE_NAME + "_v" + SessionRangeFeature.FEATURE_VERSION;
    }

    private EventSide parseEventSide(String side) {
        /*
         * Intent: Convert stored event-side text into the current high/low event terminology.
         * Precondition: side should be a persisted market-event side value.
         * Returns: EventSide using HIGH/LOW, with legacy LONG/SHORT rows mapped for compatibility.
         * Postcondition: Database data is unchanged.
         */
        if ("LONG".equals(side)) {
            return EventSide.HIGH;
        }
        if ("SHORT".equals(side)) {
            return EventSide.LOW;
        }
        return EventSide.valueOf(side);
    }

    private ImportCheckpoint findImportCheckpoint(Connection connection, String tableName) throws SQLException {
        /*
         * Intent: Load current import resume metadata for a contract table.
         * Precondition: Connection must be open and checkpoint table must exist.
         * Returns: ImportCheckpoint when metadata exists, otherwise null.
         * Postcondition: Database state is unchanged.
         */
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

    private boolean areDerivedRowsBuilt(String buildType, String name, List<ContractTradeWindow> windows) {
        /*
         * Intent: Check build markers for every requested derived-data window.
         * Precondition: buildType/name must identify a derived-data product.
         * Returns: True when each selected window has a matching build marker.
         * Postcondition: Database state is unchanged.
         */
        ensureDerivedDataTablesExist();
        if (windows == null || windows.isEmpty()) {
            return true;
        }
        try (Connection connection = DriverManager.getConnection(
                settings.primaryJdbcUrl(),
                settings.getUsername(),
                settings.getPassword()
        )) {
            for (ContractTradeWindow window : windows) {
                if (!derivedBuildExists(connection, buildType, name, window)) {
                    return false;
                }
            }
            return true;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not inspect PostgreSQL derived data build state", exception);
        }
    }

    private boolean derivedBuildExists(
            Connection connection,
            String buildType,
            String name,
            ContractTradeWindow window
    ) throws SQLException {
        /*
         * Intent: Check whether one contract window has a specific derived-data build marker.
         * Precondition: Connection must be open and window must be non-null.
         * Returns: True when the marker row exists.
         * Postcondition: Database state is unchanged.
         */
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM " + quoteIdentifier(DERIVED_BUILD_TABLE) +
                        " WHERE " + quoteIdentifier("buildType") + " = ?" +
                        " AND name = ?" +
                        " AND " + quoteIdentifier("contractSymbol") + " = ?" +
                        " AND " + quoteIdentifier("startDate") + " = ?" +
                        " AND " + quoteIdentifier("endDate") + " = ?"
        )) {
            statement.setString(1, buildType);
            statement.setString(2, name);
            statement.setString(3, window.getContractSymbol());
            statement.setDate(4, Date.valueOf(window.getStartDate()));
            statement.setDate(5, Date.valueOf(window.getEndDate()));
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void deleteSessionRanges(Connection connection, ContractTradeWindow window) throws SQLException {
        /*
         * Intent: Remove cached session range rows for one contract/date window.
         * Precondition: Connection must be open and window must identify an imported contract range.
         * Returns: Nothing.
         * Postcondition: Matching session range feature rows are removed.
         */
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM " + quoteIdentifier(SESSION_RANGE_TABLE) +
                        " WHERE " + quoteIdentifier("contractSymbol") + " = ?" +
                        " AND " + quoteIdentifier("featureVersion") + " = ?" +
                        " AND " + quoteIdentifier("sessionDate") + " >= ?" +
                        " AND " + quoteIdentifier("sessionDate") + " <= ?"
        )) {
            statement.setString(1, window.getContractSymbol());
            statement.setInt(2, SessionRangeFeature.FEATURE_VERSION);
            statement.setDate(3, Date.valueOf(window.getStartDate()));
            statement.setDate(4, Date.valueOf(window.getEndDate()));
            statement.executeUpdate();
        }
    }

    private void deleteMarketEventOccurrences(
            Connection connection,
            String eventName,
            ContractTradeWindow window
    ) throws SQLException {
        /*
         * Intent: Remove cached market event rows for one event and contract/date window.
         * Precondition: Connection must be open and eventName/window must identify cached event data.
         * Returns: Nothing.
         * Postcondition: Matching market event occurrence rows are removed.
         */
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM " + quoteIdentifier(MARKET_EVENT_TABLE) +
                        " WHERE " + quoteIdentifier("contractSymbol") + " = ?" +
                        " AND " + quoteIdentifier("eventName") + " = ?" +
                        " AND " + quoteIdentifier("sessionDate") + " >= ?" +
                        " AND " + quoteIdentifier("sessionDate") + " <= ?"
        )) {
            statement.setString(1, window.getContractSymbol());
            statement.setString(2, eventName);
            statement.setDate(3, Date.valueOf(window.getStartDate()));
            statement.setDate(4, Date.valueOf(window.getEndDate()));
            statement.executeUpdate();
        }
    }

    private void deleteDerivedBuild(
            Connection connection,
            String buildType,
            String name,
            ContractTradeWindow window
    ) throws SQLException {
        /*
         * Intent: Remove a derived-data build marker for one contract/date window.
         * Precondition: Connection must be open and marker identifiers must match the derived product.
         * Returns: Nothing.
         * Postcondition: Matching build marker row is removed.
         */
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM " + quoteIdentifier(DERIVED_BUILD_TABLE) +
                        " WHERE " + quoteIdentifier("buildType") + " = ?" +
                        " AND name = ?" +
                        " AND " + quoteIdentifier("contractSymbol") + " = ?" +
                        " AND " + quoteIdentifier("startDate") + " = ?" +
                        " AND " + quoteIdentifier("endDate") + " = ?"
        )) {
            statement.setString(1, buildType);
            statement.setString(2, name);
            statement.setString(3, window.getContractSymbol());
            statement.setDate(4, Date.valueOf(window.getStartDate()));
            statement.setDate(5, Date.valueOf(window.getEndDate()));
            statement.executeUpdate();
        }
    }

    private void markDerivedRowsBuilt(String buildType, String name, List<ContractTradeWindow> windows) {
        /*
         * Intent: Upsert build markers after derived data has been persisted.
         * Precondition: windows must describe the contract/date ranges successfully built.
         * Returns: Nothing.
         * Postcondition: Each window has a current builtAt marker for the derived product.
         */
        ensureDerivedDataTablesExist();
        if (windows == null || windows.isEmpty()) {
            return;
        }
        try (Connection connection = DriverManager.getConnection(
                settings.primaryJdbcUrl(),
                settings.getUsername(),
                settings.getPassword()
        );
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO " + quoteIdentifier(DERIVED_BUILD_TABLE) + " (" +
                             quoteIdentifier("buildType") + ", " +
                             "name, " +
                             quoteIdentifier("contractSymbol") + ", " +
                             quoteIdentifier("startDate") + ", " +
                             quoteIdentifier("endDate") + ", " +
                             quoteIdentifier("builtAt") +
                             ") VALUES (?, ?, ?, ?, ?, ?)" +
                             " ON CONFLICT (" + quoteIdentifier("buildType") + ", name, " +
                             quoteIdentifier("contractSymbol") + ", " +
                             quoteIdentifier("startDate") + ", " +
                             quoteIdentifier("endDate") + ") DO UPDATE SET " +
                             quoteIdentifier("builtAt") + " = EXCLUDED." + quoteIdentifier("builtAt")
             )) {
            for (ContractTradeWindow window : windows) {
                statement.setString(1, buildType);
                statement.setString(2, name);
                statement.setString(3, window.getContractSymbol());
                statement.setDate(4, Date.valueOf(window.getStartDate()));
                statement.setDate(5, Date.valueOf(window.getEndDate()));
                statement.setTimestamp(6, Timestamp.from(Instant.now()));
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not mark PostgreSQL derived data rows built", exception);
        }
    }

    private CheckpointMetadata findCheckpointMetadata(Connection connection, String tableName) throws SQLException {
        /*
         * Intent: Load source-file metadata used to decide whether an import can resume.
         * Precondition: Connection must be open and checkpoint table must exist.
         * Returns: CheckpointMetadata when present, otherwise null.
         * Postcondition: Database state is unchanged.
         */
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
        /*
         * Intent: Check whether stored checkpoint metadata still matches the selected source file.
         * Precondition: Connection must be open and metadata values must come from the current file.
         * Returns: True when the checkpoint belongs to the selected file version.
         * Postcondition: Database state is unchanged.
         */
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
        /*
         * Intent: Create initial import checkpoint metadata for a contract table.
         * Precondition: Connection must be open and no checkpoint row should exist for the table.
         * Returns: Nothing.
         * Postcondition: Import can start at the first SCID record with IN_PROGRESS status.
         */
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
        /*
         * Intent: Mark an existing checkpoint as actively importing.
         * Precondition: Connection must be open and checkpoint row must exist.
         * Returns: Nothing.
         * Postcondition: Checkpoint status and updated timestamp reflect an active import.
         */
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
        /*
         * Intent: Reset checkpoint metadata when importing a different source file for the same contract.
         * Precondition: Connection must be open and table must already have checkpoint metadata.
         * Returns: Nothing.
         * Postcondition: Import resumes from record one with cleared date bounds and row count.
         */
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

    private boolean contractTableExists(Connection connection, String tableName) throws SQLException {
        /*
         * Intent: Check whether a contract-specific trade table exists in the current schema.
         * Precondition: Connection must be open and tableName must be normalized.
         * Returns: True when the table exists.
         * Postcondition: Database state is unchanged.
         */
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
        /*
         * Intent: Count all rows currently stored in a contract table.
         * Precondition: Connection must be open and tableName must refer to a validated table.
         * Returns: Total row count.
         * Postcondition: Database state is unchanged.
         */
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + quoteIdentifier(tableName))) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private long countRowsBetween(
            Connection connection,
            String tableName,
            Instant firstTradeDateTime,
            Instant lastTradeDateTime
    ) throws SQLException {
        /*
         * Intent: Count stored rows that overlap the selected import file timestamp range.
         * Precondition: Connection must be open and bounds may be null for empty importable files.
         * Returns: Number of rows between the inclusive timestamp bounds.
         * Postcondition: Database state is unchanged.
         */
        if (firstTradeDateTime == null || lastTradeDateTime == null) {
            return 0;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM " + quoteIdentifier(tableName) +
                        " WHERE " + quoteIdentifier("tradeDateTime") + " >= ?" +
                        " AND " + quoteIdentifier("tradeDateTime") + " <= ?"
        )) {
            statement.setTimestamp(1, Timestamp.from(firstTradeDateTime));
            statement.setTimestamp(2, Timestamp.from(lastTradeDateTime));
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private TradeDateTimeBounds findTableDateTimeBounds(Connection connection, String tableName) throws SQLException {
        /*
         * Intent: Determine stored timestamp coverage for a contract table.
         * Precondition: Connection must be open and tableName must refer to an existing contract table.
         * Returns: First and last stored trade timestamps, or empty bounds for no rows.
         * Postcondition: Database state is unchanged.
         */
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT MIN(" + quoteIdentifier("tradeDateTime") + "), " +
                             "MAX(" + quoteIdentifier("tradeDateTime") + ") FROM " + quoteIdentifier(tableName)
             )) {
            resultSet.next();
            Timestamp first = resultSet.getTimestamp(1);
            Timestamp last = resultSet.getTimestamp(2);
            return new TradeDateTimeBounds(
                    first == null ? null : first.toInstant(),
                    last == null ? null : last.toInstant()
            );
        }
    }

    private boolean isContractTableName(String tableName) {
        /*
         * Intent: Identify normalized futures contract tables owned by FORGE.
         * Precondition: tableName may be null or arbitrary database metadata.
         * Returns: True for expected contract symbols such as ESU25 or CLF26.
         * Postcondition: Input value is unchanged.
         */
        return tableName != null && tableName.toUpperCase().matches("[A-Z]{1,3}[FGHJKMNQUVXZ][0-9]{1,2}");
    }

    private List<String> listForgeOwnedTables(Connection connection) throws SQLException {
        /*
         * Intent: Discover tables that belong to FORGE without touching unrelated tables in the same schema.
         * Precondition: Connection must target the configured primary database.
         * Returns: Table names for contract tables and forge_* metadata/cache tables.
         * Postcondition: Database state is unchanged.
         */
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT table_name FROM information_schema.tables " +
                        "WHERE table_schema = current_schema() AND table_type = 'BASE TABLE' " +
                        "ORDER BY table_name"
        );
             ResultSet resultSet = statement.executeQuery()) {
            List<String> tableNames = new ArrayList<>();
            while (resultSet.next()) {
                String tableName = resultSet.getString(1);
                if (isForgeOwnedTable(tableName)) {
                    tableNames.add(tableName);
                }
            }
            return tableNames;
        }
    }

    private boolean isForgeOwnedTable(String tableName) {
        /*
         * Intent: Distinguish tables FORGE may manage from unrelated schema tables.
         * Precondition: tableName may be null or any table in the current schema.
         * Returns: True for forge_* tables and normalized contract tables.
         * Postcondition: Input value is unchanged.
         */
        return tableName != null && (tableName.startsWith("forge_") || isContractTableName(tableName));
    }

    private void createTemporaryImportTable(Connection connection, String temporaryTableName) throws SQLException {
        /*
         * Intent: Create a transaction-scoped staging table for PostgreSQL COPY import batches.
         * Precondition: Connection must be open inside an import transaction.
         * Returns: Nothing.
         * Postcondition: Temporary table exists until the transaction commits or rolls back.
         */
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "CREATE TEMP TABLE " + quoteIdentifier(temporaryTableName) + " (" +
                            quoteIdentifier("tradeDateTime") + " TIMESTAMPTZ NOT NULL, " +
                            quoteIdentifier("priceTicks") + " BIGINT NOT NULL, " +
                            quoteIdentifier("bidPriceTicks") + " BIGINT, " +
                            quoteIdentifier("askPriceTicks") + " BIGINT, " +
                            "quantity BIGINT NOT NULL, " +
                            "side INT, " +
                            quoteIdentifier("numTrades") + " BIGINT NOT NULL, " +
                            quoteIdentifier("sourceFileName") + " TEXT NOT NULL, " +
                            quoteIdentifier("scidRecordIndex") + " BIGINT NOT NULL" +
                            ") ON COMMIT DROP"
            );
        }
    }

    private int insertFromTemporaryImportTable(
            Connection connection,
            String tableName,
            String temporaryTableName
    ) throws SQLException {
        /*
         * Intent: Move staged import rows into the contract table while skipping duplicates.
         * Precondition: Temporary table must contain rows shaped like the target contract table.
         * Returns: Number of rows inserted into the target table.
         * Postcondition: Existing duplicate rows remain unchanged.
         */
        try (Statement statement = connection.createStatement()) {
            return statement.executeUpdate(
                    "INSERT INTO " + quoteIdentifier(tableName) + " (" +
                            quoteIdentifier("tradeDateTime") + ", " +
                            quoteIdentifier("priceTicks") + ", " +
                            quoteIdentifier("bidPriceTicks") + ", " +
                            quoteIdentifier("askPriceTicks") + ", " +
                            "quantity, " +
                            "side, " +
                            quoteIdentifier("numTrades") + ", " +
                            quoteIdentifier("sourceFileName") + ", " +
                            quoteIdentifier("scidRecordIndex") +
                            ") SELECT " +
                            quoteIdentifier("tradeDateTime") + ", " +
                            quoteIdentifier("priceTicks") + ", " +
                            quoteIdentifier("bidPriceTicks") + ", " +
                            quoteIdentifier("askPriceTicks") + ", " +
                            "quantity, " +
                            "side, " +
                            quoteIdentifier("numTrades") + ", " +
                            quoteIdentifier("sourceFileName") + ", " +
                            quoteIdentifier("scidRecordIndex") +
                            " FROM " + quoteIdentifier(temporaryTableName) +
                            " ON CONFLICT DO NOTHING"
            );
        }
    }

    private void updateImportCheckpoint(
            Connection connection,
            String tableName,
            String sourceFileName,
            long nextRecordIndex,
            int importedRows,
            List<TradeRow> importedTrades
    ) throws SQLException {
        /*
         * Intent: Advance checkpoint row counts and timestamp coverage after a committed batch.
         * Precondition: Connection must be open and checkpoint row must exist for tableName.
         * Returns: Nothing.
         * Postcondition: Resume index, inserted row count, coverage bounds, and updatedAt are current.
         */
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
        /*
         * Intent: Calculate timestamp coverage for one imported batch.
         * Precondition: trades may be null or empty after rollover filtering.
         * Returns: First/last trade timestamps for the batch, or empty bounds.
         * Postcondition: Trade list contents are unchanged.
         */
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
        /*
         * Intent: Bind nullable Instant values to PostgreSQL timestamp parameters.
         * Precondition: statement must be open and index must refer to a timestamp placeholder.
         * Returns: Nothing.
         * Postcondition: Parameter is set to timestamp or SQL NULL.
         */
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
        /*
         * Intent: Check whether the configured FORGE database already exists.
         * Precondition: Connection must target a maintenance database with pg_database access.
         * Returns: True when a database with the requested name exists.
         * Postcondition: PostgreSQL catalog state is unchanged.
         */
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
        /*
         * Intent: Safely quote validated PostgreSQL identifiers used in dynamic SQL.
         * Precondition: identifier must be a simple table/column/index name.
         * Returns: Double-quoted identifier for SQL statements.
         * Postcondition: Invalid identifiers fail before SQL execution.
         */
        if (identifier == null || !identifier.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Invalid PostgreSQL identifier: " + identifier);
        }
        return "\"" + identifier + "\"";
    }

    private String toCopyText(String sourceFileName, List<TradeRow> trades) {
        /*
         * Intent: Serialize one trade batch into PostgreSQL COPY text format.
         * Precondition: sourceFileName and trades must describe one import batch.
         * Returns: Tab-delimited COPY payload with escaped text and nullable fields.
         * Postcondition: Trade rows are unchanged.
         */
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
        /*
         * Intent: Append a nullable long value in PostgreSQL COPY text format.
         * Precondition: builder must be non-null.
         * Returns: Nothing.
         * Postcondition: Builder contains either the number or COPY null marker.
         */
        if (value == null) {
            builder.append("\\N");
            return;
        }
        builder.append(value);
    }

    private void appendNullableInteger(StringBuilder builder, Integer value) {
        /*
         * Intent: Append a nullable integer value in PostgreSQL COPY text format.
         * Precondition: builder must be non-null.
         * Returns: Nothing.
         * Postcondition: Builder contains either the number or COPY null marker.
         */
        if (value == null) {
            builder.append("\\N");
            return;
        }
        builder.append(value);
    }

    private void appendCopyText(StringBuilder builder, String value) {
        /*
         * Intent: Append escaped text in PostgreSQL COPY text format.
         * Precondition: builder must be non-null and value may be null.
         * Returns: Nothing.
         * Postcondition: Builder contains escaped text or COPY null marker.
         */
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
