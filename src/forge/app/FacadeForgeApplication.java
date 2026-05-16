package forge.app;

import forge.config.BacktestRequest;
import forge.data.DataImportPlan;
import forge.data.DataImportResult;
import forge.data.FacadeForgeData;
import forge.data.PostgresDatabaseSettings;

import java.util.Objects;

public class FacadeForgeApplication {
    private static final FacadeForgeApplication THE_INSTANCE = new FacadeForgeApplication();

    private final FacadeForgeData forgeData;
    private final ForgeApplicationAccess access = new ForgeApplicationAccess();

    public FacadeForgeApplication() {
        this(FacadeForgeData.getTheInstance());
    }

    public FacadeForgeApplication(FacadeForgeData forgeData) {
        this.forgeData = Objects.requireNonNull(forgeData, "forgeData is required");
    }

    public static FacadeForgeApplication getTheInstance() {
        return THE_INSTANCE;
    }

    public ForgeApplicationAccess forgeApplicationAccess() {
        return access;
    }

    public class ForgeApplicationAccess {
        public BacktestRequest runBacktest(BacktestRequest request) {
            return Objects.requireNonNull(request, "request is required");
        }

        public DataImportResult importData(DataImportRequest request) {
            Objects.requireNonNull(request, "request is required");
            return forgeData.forgeDataAccess().importScidFile(
                    request.getScidFilePath(),
                    request.shouldRebuildExistingContract()
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
