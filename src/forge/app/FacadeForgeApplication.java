package forge.app;

import forge.config.BacktestRequest;

import java.util.Objects;

public class FacadeForgeApplication {
    private static final FacadeForgeApplication THE_INSTANCE = new FacadeForgeApplication();

    private final ForgeApplicationAccess access = new ForgeApplicationAccess();

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

        public DataImportRequest importData(DataImportRequest request) {
            return Objects.requireNonNull(request, "request is required");
        }
    }
}
