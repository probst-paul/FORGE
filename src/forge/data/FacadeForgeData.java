package forge.data;

import forge.app.ImportProgressListener;
import forge.data.catalog.InstrumentDataCatalog;
import forge.data.catalog.InstrumentDataCatalog.AvailableContractData;
import forge.data.catalog.InstrumentDataCatalog.AvailableDateRange;
import forge.data.catalog.InstrumentDataCatalog.AvailableInstrumentData;
import forge.data.contract.ContractNameResolver;
import forge.data.importing.DataImportPlan;
import forge.data.importing.DataImportResult;
import forge.data.importing.ScidDataImportService;
import forge.data.market.ContractTradeWindow;
import forge.data.market.TradeBatchReader;
import forge.data.market.TickDataProvider;
import forge.data.postgres.PostgresDatabaseSettings;
import forge.data.postgres.PostgresTickDataProvider;
import forge.data.postgres.PostgresTradeRepository;

import java.time.LocalDate;
import java.util.List;

public class FacadeForgeData {
    private static final FacadeForgeData THE_INSTANCE = new FacadeForgeData();

    private InstrumentDataCatalog instrumentDataCatalog;
    private ScidDataImportService scidDataImportService;
    private TickDataProvider tickDataProvider;
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
                new ScidDataImportService(new ContractNameResolver(), tradeRepository),
                new PostgresTickDataProvider(PostgresDatabaseSettings.fromEnvironment())
        );
    }

    public FacadeForgeData(InstrumentDataCatalog instrumentDataCatalog) {
        this(
                instrumentDataCatalog,
                new ScidDataImportService(
                        new ContractNameResolver(),
                        new PostgresTradeRepository(PostgresDatabaseSettings.fromEnvironment())
                ),
                new PostgresTickDataProvider(PostgresDatabaseSettings.fromEnvironment())
        );
    }

    public FacadeForgeData(InstrumentDataCatalog instrumentDataCatalog, ScidDataImportService scidDataImportService) {
        this(instrumentDataCatalog, scidDataImportService, new PostgresTickDataProvider(PostgresDatabaseSettings.fromEnvironment()));
    }

    public FacadeForgeData(
            InstrumentDataCatalog instrumentDataCatalog,
            ScidDataImportService scidDataImportService,
            TickDataProvider tickDataProvider
    ) {
        if (instrumentDataCatalog == null) {
            throw new IllegalArgumentException("instrumentDataCatalog is required");
        }
        if (scidDataImportService == null) {
            throw new IllegalArgumentException("scidDataImportService is required");
        }
        if (tickDataProvider == null) {
            throw new IllegalArgumentException("tickDataProvider is required");
        }
        this.instrumentDataCatalog = instrumentDataCatalog;
        this.scidDataImportService = scidDataImportService;
        this.tickDataProvider = tickDataProvider;
    }

    public ForgeDataAccess forgeDataAccess() {
        return access;
    }

    public class ForgeDataAccess {
        public List<AvailableInstrumentData> getAvailableInstruments() {
            return instrumentDataCatalog.getAvailableInstruments();
        }

        public List<AvailableContractData> getAvailableContracts() {
            return instrumentDataCatalog.getAvailableContracts();
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

        public TradeBatchReader openTradeBatchReader(List<ContractTradeWindow> windows, int batchSize) {
            return tickDataProvider.openReader(windows, batchSize);
        }

        public long countTradeTicks(List<ContractTradeWindow> windows) {
            return tickDataProvider.countTicks(windows);
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
            tickDataProvider = new PostgresTickDataProvider(databaseSettings);
        }
    }
}
