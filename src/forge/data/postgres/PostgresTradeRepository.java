package forge.data.postgres;

import forge.data.catalog.ContractDataSummary;
import forge.data.importing.DataImportPlan;
import forge.data.importing.ImportCheckpoint;
import forge.data.importing.TradeRow;
import forge.data.market.ContractTradeWindow;
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
                            quoteIdentifier("createdAt") + " TIMESTAMPTZ NOT NULL, " +
                            "PRIMARY KEY (" + quoteIdentifier("contractSymbol") + ", " +
                            quoteIdentifier("sessionDate") + ", " +
                            quoteIdentifier("featureVersion") + ")" +
                            ")"
            );
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

    public ImportCheckpoint prepareImportCheckpoint(
            String tableName,
            String sourceFileName,
            long fileSizeBytes,
            long lastModifiedMillis,
            boolean rebuildExistingContract
    ) {
        /*
         * Intent: Prepare or resume import metadata according to the authoritative contract-table policy.
         * Precondition: Contract table/checkpoint table must exist and source metadata must describe selected file.
         * Returns: ImportCheckpoint indicating the next SCID record index to read.
         * Postcondition: Existing data may be truncated only when rebuildExistingContract is true.
         */
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
        return areDerivedRowsBuilt(BUILD_TYPE_SESSION_RANGE, SessionRangeFeature.FEATURE_NAME, windows);
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
                                quoteIdentifier("rthHighTicks") +
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
                                    resultSet.getLong(8)
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
                             quoteIdentifier("createdAt") +
                             ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" +
                             " ON CONFLICT (" + quoteIdentifier("contractSymbol") + ", " +
                             quoteIdentifier("sessionDate") + ", " +
                             quoteIdentifier("featureVersion") + ") DO UPDATE SET " +
                             quoteIdentifier("overnightLowTicks") + " = EXCLUDED." + quoteIdentifier("overnightLowTicks") + ", " +
                             quoteIdentifier("overnightHighTicks") + " = EXCLUDED." + quoteIdentifier("overnightHighTicks") + ", " +
                             quoteIdentifier("firstHourLowTicks") + " = EXCLUDED." + quoteIdentifier("firstHourLowTicks") + ", " +
                             quoteIdentifier("firstHourHighTicks") + " = EXCLUDED." + quoteIdentifier("firstHourHighTicks") + ", " +
                             quoteIdentifier("rthLowTicks") + " = EXCLUDED." + quoteIdentifier("rthLowTicks") + ", " +
                             quoteIdentifier("rthHighTicks") + " = EXCLUDED." + quoteIdentifier("rthHighTicks")
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
                statement.setTimestamp(10, Timestamp.from(Instant.now()));
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
        markDerivedRowsBuilt(BUILD_TYPE_SESSION_RANGE, SessionRangeFeature.FEATURE_NAME, windows);
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
                deleteDerivedBuild(connection, BUILD_TYPE_SESSION_RANGE, SessionRangeFeature.FEATURE_NAME, window);
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
                                    EventSide.valueOf(resultSet.getString(5)),
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

    private boolean areDerivedRowsBuilt(String buildType, String name, List<ContractTradeWindow> windows) {
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
