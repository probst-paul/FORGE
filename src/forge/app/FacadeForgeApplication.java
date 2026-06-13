package forge.app;

import forge.config.BacktestRequest;
import forge.data.FacadeForgeData;
import forge.data.importing.DataImportPlan;
import forge.data.importing.DataImportResult;
import forge.data.postgres.PostgresDatabaseSettings;
import forge.engine.FacadeForgeEngine;
import forge.engine.eventstatistics.EventStatisticsQueryRequest;
import forge.reporting.eventstatistics.EventStatisticsReport;
import forge.engine.backtest.BacktestResult;

import java.util.Objects;

public class FacadeForgeApplication {
    private static final FacadeForgeApplication THE_INSTANCE = new FacadeForgeApplication();

    private final FacadeForgeData forgeData;
    private final FacadeForgeEngine forgeEngine;
    private final ForgeApplicationAccess access = new ForgeApplicationAccess();

    public FacadeForgeApplication() {
        /*
         * Intent: Create the application facade using the default package facades.
         * Precondition: Default data and engine facades must be available.
         * Returns: A constructed FacadeForgeApplication instance.
         * Postcondition: Application facade is wired to data and engine package boundaries.
         */
        this(
                FacadeForgeData.getTheInstance(),
                FacadeForgeEngine.getTheInstance()
        );
    }

    public FacadeForgeApplication(FacadeForgeData forgeData) {
        /*
         * Intent: Create the application facade with a custom data facade and default engine facade.
         * Precondition: Data facade must exist and default engine facade must be available.
         * Returns: A constructed FacadeForgeApplication instance.
         * Postcondition: Application facade is wired for workflows that need data and engine access.
         */
        this(
                forgeData,
                FacadeForgeEngine.getTheInstance()
        );
    }

    public FacadeForgeApplication(FacadeForgeData forgeData, FacadeForgeEngine forgeEngine) {
        /*
         * Intent: Create the application facade with explicit package-facade dependencies.
         * Precondition: Data and engine facades must exist.
         * Returns: A constructed FacadeForgeApplication instance.
         * Postcondition: Application facade is ready to expose workflow-level access methods.
         */
        this.forgeData = Objects.requireNonNull(forgeData, "forgeData is required");
        this.forgeEngine = Objects.requireNonNull(forgeEngine, "forgeEngine is required");
    }

    public static FacadeForgeApplication getTheInstance() {
        return THE_INSTANCE;
    }

    public ForgeApplicationAccess forgeApplicationAccess() {
        return access;
    }

    public class ForgeApplicationAccess {
        /*
         * Intent: Run a backtest through the engine facade without exposing engine internals to callers.
         * Precondition: Backtest request must exist and be valid for the engine.
         * Returns: BacktestResult produced by the engine.
         * Postcondition: Application facade state is unchanged.
         */
        public BacktestResult runBacktest(BacktestRequest request) {
            Objects.requireNonNull(request, "request is required");
            return forgeEngine.forgeEngineAccess().run(request);
        }

        /*
         * Intent: Run a backtest through the engine facade while reporting progress.
         * Precondition: Backtest request must exist; progress listener may be handled by engine access according to its contract.
         * Returns: BacktestResult produced by the engine.
         * Postcondition: Application facade state is unchanged.
         */
        public BacktestResult runBacktest(BacktestRequest request, BacktestProgressListener progressListener) {
            Objects.requireNonNull(request, "request is required");
            return forgeEngine.forgeEngineAccess().run(request, progressListener);
        }

        /*
         * Intent: Translate an app-level statistics request into an engine query request.
         * Precondition: Event statistics request must exist and contain valid contract windows/event name.
         * Returns: EventStatisticsReport produced by the engine query runner.
         * Postcondition: Application facade state is unchanged.
         */
        public EventStatisticsReport runEventStatistics(EventStatisticsRequest request) {
            Objects.requireNonNull(request, "request is required");
            return forgeEngine.forgeEngineAccess().runEventStatistics(new EventStatisticsQueryRequest(
                    request.getContractWindows(),
                    request.getEventName(),
                    EventStatisticsQueryRequest.DEFAULT_BATCH_SIZE,
                    request.getProgressListener()
            ));
        }

        /*
         * Intent: Import SCID data through the data facade.
         * Precondition: Data import request must exist and contain a valid SCID file path.
         * Returns: DataImportResult describing the import outcome.
         * Postcondition: Data storage may be changed by the data package; application facade state is unchanged.
         */
        public DataImportResult importData(DataImportRequest request) {
            Objects.requireNonNull(request, "request is required");
            return forgeData.forgeDataAccess().importScidFile(
                    request.getScidFilePath(),
                    request.getImportMode(),
                    request.getProgressListener()
            );
        }

        /*
         * Intent: Inspect a SCID import before running it so callers can confirm rebuild behavior.
         * Precondition: Data import request must exist and contain a valid SCID file path.
         * Returns: DataImportPlan describing target contract/table state.
         * Postcondition: No import is performed and application facade state is unchanged.
         */
        public DataImportPlan planDataImport(DataImportRequest request) {
            Objects.requireNonNull(request, "request is required");
            return forgeData.forgeDataAccess().planScidImport(request.getScidFilePath());
        }

        /*
         * Intent: Configure PostgreSQL database access through the data facade.
         * Precondition: Database connection request must exist and contain validated connection settings.
         * Returns: The same request after configuration succeeds.
         * Postcondition: Data package database settings are updated.
         */
        public DatabaseConnectionRequest configureDatabase(DatabaseConnectionRequest request) {
            Objects.requireNonNull(request, "request is required");
            forgeData.forgeDataAccess().configurePostgresDatabase(new PostgresDatabaseSettings(
                    request.getHost(),
                    request.getPort(),
                    request.getDatabaseName(),
                    request.getMaintenanceDatabaseName(),
                    request.getUsername(),
                    request.getPassword()
            ));
            return request;
        }

        /*
         * Intent: Prepare the configured database for GUI workflows without requiring explicit user setup.
         * Precondition: PostgreSQL settings must be available from environment/default configuration.
         * Returns: Nothing.
         * Postcondition: Database and required FORGE support tables exist if PostgreSQL is reachable.
         */
        public void prepareDatabase() {
            forgeData.forgeDataAccess().prepareDatabase();
        }

        /*
         * Intent: Wipe all FORGE-owned data tables from the configured database.
         * Precondition: Caller must have completed destructive-action confirmation.
         * Returns: Number of tables dropped.
         * Postcondition: Imported contract, import metadata, and derived-data tables are removed.
         */
        public int wipeDatabase() {
            return forgeData.forgeDataAccess().wipeDatabase();
        }

    }
}
