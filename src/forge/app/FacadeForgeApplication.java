package forge.app;

import forge.config.BacktestRequest;
import forge.data.FacadeForgeData;
import forge.data.importing.DataImportPlan;
import forge.data.importing.DataImportResult;
import forge.data.postgres.PostgresDatabaseSettings;
import forge.engine.FacadeForgeEngine;
import forge.reporting.BacktestResult;

import java.util.Objects;

public class FacadeForgeApplication {
    private static final FacadeForgeApplication THE_INSTANCE = new FacadeForgeApplication();

    private final FacadeForgeData forgeData;
    private final FacadeForgeEngine forgeEngine;
    private final ForgeApplicationAccess access = new ForgeApplicationAccess();

    public FacadeForgeApplication() {
        this(FacadeForgeData.getTheInstance(), FacadeForgeEngine.getTheInstance());
    }

    public FacadeForgeApplication(FacadeForgeData forgeData) {
        this(forgeData, FacadeForgeEngine.getTheInstance());
    }

    public FacadeForgeApplication(FacadeForgeData forgeData, FacadeForgeEngine forgeEngine) {
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
        public BacktestResult runBacktest(BacktestRequest request) {
            Objects.requireNonNull(request, "request is required");
            return forgeEngine.forgeEngineAccess().run(request);
        }

        public DataImportResult importData(DataImportRequest request) {
            Objects.requireNonNull(request, "request is required");
            return forgeData.forgeDataAccess().importScidFile(
                    request.getScidFilePath(),
                    request.shouldRebuildExistingContract(),
                    request.getProgressListener()
            );
        }

        public DataImportPlan planDataImport(DataImportRequest request) {
            Objects.requireNonNull(request, "request is required");
            return forgeData.forgeDataAccess().planScidImport(request.getScidFilePath());
        }

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
    }
}
