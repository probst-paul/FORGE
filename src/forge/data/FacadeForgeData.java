package forge.data;

import forge.app.ImportProgressListener;
import forge.data.build.DataBuildProgressListener;
import forge.data.build.DatabaseBuildPlan;
import forge.data.build.DatabaseBuildRequest;
import forge.data.build.DatabaseBuildResult;
import forge.data.build.DerivedDataBuildService;
import forge.data.build.DerivedDataBuildStore;
import forge.data.build.DerivedDataBuildTradeSource;
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
import forge.condition.MarketConditionOccurrence;
import forge.feature.SessionRangeFeature;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public class FacadeForgeData {
    private static final FacadeForgeData THE_INSTANCE = new FacadeForgeData();

    private InstrumentDataCatalog instrumentDataCatalog;
    private ScidDataImportService scidDataImportService;
    private TickDataProvider tickDataProvider;
    private DerivedDataBuildService derivedDataBuildService;
    private final ForgeDataAccess access = new ForgeDataAccess();

    public static FacadeForgeData getTheInstance() {
        /*
         * Intent: Provide the shared data facade used by application, engine, and CLI wiring.
         * Precondition: Static facade instance must have initialized successfully.
         * Returns: Singleton FacadeForgeData instance.
         * Postcondition: No new facade is created.
         */
        return THE_INSTANCE;
    }

    public FacadeForgeData() {
        /*
         * Intent: Create the data facade from environment-based PostgreSQL settings.
         * Precondition: PostgreSQL settings may be resolved from environment variables or defaults.
         * Returns: A constructed FacadeForgeData instance.
         * Postcondition: Catalog, importer, tick provider, and derived-data builder share the same repository settings.
         */
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
        /*
         * Intent: Create the data facade with explicit core data dependencies.
         * Precondition: Catalog, import service, and tick provider must be non-null.
         * Returns: A constructed FacadeForgeData instance.
         * Postcondition: Derived-data build service is wired to the supplied tick provider and import repository.
         */
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
        this.derivedDataBuildService = createDerivedDataBuildService(tickDataProvider, scidDataImportService.getTradeRepository());
    }

    public ForgeDataAccess forgeDataAccess() {
        /*
         * Intent: Expose the public access object for data package operations.
         * Precondition: Facade must be constructed.
         * Returns: Stable ForgeDataAccess instance.
         * Postcondition: Facade state is unchanged.
         */
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

        public DatabaseBuildPlan planDatabaseBuild(DatabaseBuildRequest request) {
            return derivedDataBuildService.planBuild(request);
        }

        public DatabaseBuildResult runDatabaseBuild(
                DatabaseBuildRequest request,
                DataBuildProgressListener progressListener
        ) {
            return derivedDataBuildService.runBuild(request, progressListener);
        }

        public boolean areSessionRangesBuilt(List<ContractTradeWindow> windows) {
            return scidDataImportService.getTradeRepository().areSessionRangesBuilt(windows);
        }

        public List<SessionRangeFeature> loadSessionRanges(List<ContractTradeWindow> windows) {
            return scidDataImportService.getTradeRepository().loadSessionRanges(windows);
        }

        public void saveSessionRanges(Collection<SessionRangeFeature> sessionRangeFeatures) {
            scidDataImportService.getTradeRepository().saveSessionRanges(sessionRangeFeatures);
        }

        public void markSessionRangesBuilt(List<ContractTradeWindow> windows) {
            scidDataImportService.getTradeRepository().markSessionRangesBuilt(windows);
        }

        public boolean areMarketConditionOccurrencesBuilt(List<ContractTradeWindow> windows, String eventName) {
            return scidDataImportService.getTradeRepository().areMarketConditionOccurrencesBuilt(windows, eventName);
        }

        public List<MarketConditionOccurrence> loadMarketConditionOccurrences(List<ContractTradeWindow> windows, String eventName) {
            return scidDataImportService.getTradeRepository().loadMarketConditionOccurrences(windows, eventName);
        }

        public void saveMarketConditionOccurrences(Collection<MarketConditionOccurrence> marketEvents) {
            scidDataImportService.getTradeRepository().saveMarketConditionOccurrences(marketEvents);
        }

        public void markMarketConditionOccurrencesBuilt(List<ContractTradeWindow> windows, String eventName) {
            scidDataImportService.getTradeRepository().markMarketConditionOccurrencesBuilt(windows, eventName);
        }

        public void configurePostgresDatabase(PostgresDatabaseSettings databaseSettings) {
            /*
             * Intent: Reconfigure all data services to use a new PostgreSQL database target.
             * Precondition: Database settings must be valid.
             * Returns: Nothing.
             * Postcondition: Catalog, importer, tick provider, and derived-data builder are rebuilt around the new settings.
             */
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
            derivedDataBuildService = createDerivedDataBuildService(tickDataProvider, tradeRepository);
        }
    }

    private DerivedDataBuildService createDerivedDataBuildService(
            TickDataProvider tickDataProvider,
            PostgresTradeRepository tradeRepository
    ) {
        /*
         * Intent: Adapt repository/tick-provider operations into the derived-data build service interfaces.
         * Precondition: Tick provider and repository must be configured for the same database.
         * Returns: DerivedDataBuildService wired for streaming ticks and persisting derived data.
         * Postcondition: No derived data is built until the returned service is invoked.
         */
        return new DerivedDataBuildService(
                new DerivedDataBuildTradeSource() {
                    @Override
                    public TradeBatchReader openTradeBatchReader(List<ContractTradeWindow> windows, int batchSize) {
                        return tickDataProvider.openReader(windows, batchSize);
                    }

                    @Override
                    public long countTradeTicks(List<ContractTradeWindow> windows) {
                        return tickDataProvider.countTicks(windows);
                    }
                },
                new DerivedDataBuildStore() {
                    @Override
                    public boolean areSessionRangesBuilt(List<ContractTradeWindow> windows) {
                        return tradeRepository.areSessionRangesBuilt(windows);
                    }

                    @Override
                    public List<SessionRangeFeature> loadSessionRanges(List<ContractTradeWindow> windows) {
                        return tradeRepository.loadSessionRanges(windows);
                    }

                    @Override
                    public void saveSessionRanges(Collection<SessionRangeFeature> sessionRangeFeatures) {
                        tradeRepository.saveSessionRanges(sessionRangeFeatures);
                    }

                    @Override
                    public void markSessionRangesBuilt(List<ContractTradeWindow> windows) {
                        tradeRepository.markSessionRangesBuilt(windows);
                    }

                    @Override
                    public void clearSessionRanges(List<ContractTradeWindow> windows) {
                        tradeRepository.clearSessionRanges(windows);
                    }

                    @Override
                    public boolean areMarketConditionOccurrencesBuilt(List<ContractTradeWindow> windows, String eventName) {
                        return tradeRepository.areMarketConditionOccurrencesBuilt(windows, eventName);
                    }

                    @Override
                    public List<MarketConditionOccurrence> loadMarketConditionOccurrences(List<ContractTradeWindow> windows, String eventName) {
                        return tradeRepository.loadMarketConditionOccurrences(windows, eventName);
                    }

                    @Override
                    public void saveMarketConditionOccurrences(Collection<MarketConditionOccurrence> marketEvents) {
                        tradeRepository.saveMarketConditionOccurrences(marketEvents);
                    }

                    @Override
                    public void markMarketConditionOccurrencesBuilt(List<ContractTradeWindow> windows, String eventName) {
                        tradeRepository.markMarketConditionOccurrencesBuilt(windows, eventName);
                    }

                    @Override
                    public void clearMarketConditionOccurrences(List<ContractTradeWindow> windows, String eventName) {
                        tradeRepository.clearMarketConditionOccurrences(windows, eventName);
                    }
                },
                new forge.feature.FeatureBuildService(),
                new forge.condition.ConditionBuildService()
        );
    }
}
