package forge.data;

import forge.app.ImportProgressListener;
import forge.data.catalog.InstrumentDataCatalog;
import forge.data.catalog.InstrumentDataCatalog.AvailableDateRange;
import forge.data.catalog.InstrumentDataCatalog.AvailableInstrumentData;
import forge.data.contract.ContractNameResolver;
import forge.data.importing.DataImportPlan;
import forge.data.importing.DataImportResult;
import forge.data.importing.ScidDataImportService;
import forge.data.postgres.PostgresDatabaseSettings;
import forge.data.postgres.PostgresTradeRepository;

import java.time.LocalDate;
import java.util.List;

public class FacadeForgeData {
    private static final FacadeForgeData THE_INSTANCE = new FacadeForgeData();

    private InstrumentDataCatalog instrumentDataCatalog;
    private ScidDataImportService scidDataImportService;
    private final ForgeDataAccess access = new ForgeDataAccess();

    public static FacadeForgeData getTheInstance() {
        return THE_INSTANCE;
    }

    public FacadeForgeData() {
        this(new PostgresTradeRepository(PostgresDatabaseSettings.fromEnvironment()));
    }

    private FacadeForgeData(PostgresTradeRepository tradeRepository) {
        this(
                new InstrumentDataCatalog(tradeRepository),
                new ScidDataImportService(new ContractNameResolver(), tradeRepository)
        );
    }

    public FacadeForgeData(InstrumentDataCatalog instrumentDataCatalog) {
        this(
                instrumentDataCatalog,
                new ScidDataImportService(
                        new ContractNameResolver(),
                        new PostgresTradeRepository(PostgresDatabaseSettings.fromEnvironment())
                )
        );
    }

    public FacadeForgeData(InstrumentDataCatalog instrumentDataCatalog, ScidDataImportService scidDataImportService) {
        if (instrumentDataCatalog == null) {
            throw new IllegalArgumentException("instrumentDataCatalog is required");
        }
        if (scidDataImportService == null) {
            throw new IllegalArgumentException("scidDataImportService is required");
        }
        this.instrumentDataCatalog = instrumentDataCatalog;
        this.scidDataImportService = scidDataImportService;
    }

    public ForgeDataAccess forgeDataAccess() {
        return access;
    }

    public class ForgeDataAccess {
        public List<AvailableInstrumentData> getAvailableInstruments() {
            return instrumentDataCatalog.getAvailableInstruments();
        }

        public AvailableDateRange getSharedDateRange(List<String> symbols) {
            return instrumentDataCatalog.getSharedDateRange(symbols);
        }

        public void validateDateRange(List<String> symbols, LocalDate startDate, LocalDate endDate) {
            instrumentDataCatalog.validateDateRange(symbols, startDate, endDate);
        }

        public DataImportPlan planScidImport(String scidFilePath) {
            return scidDataImportService.planImport(scidFilePath);
        }

        public DataImportResult importScidFile(
                String scidFilePath,
                boolean rebuildExistingContract,
                ImportProgressListener progressListener
        ) {
            return scidDataImportService.importScidFile(scidFilePath, rebuildExistingContract, progressListener);
        }

        public void configurePostgresDatabase(PostgresDatabaseSettings databaseSettings) {
            if (databaseSettings == null) {
                throw new IllegalArgumentException("databaseSettings is required");
            }
            PostgresTradeRepository tradeRepository = new PostgresTradeRepository(databaseSettings);
            instrumentDataCatalog = new InstrumentDataCatalog(tradeRepository);
            scidDataImportService = new ScidDataImportService(
                    new ContractNameResolver(),
                    tradeRepository
            );
        }
    }
}
