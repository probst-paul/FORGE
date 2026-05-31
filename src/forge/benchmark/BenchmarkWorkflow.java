package forge.benchmark;

import forge.app.DataImportRequest;
import forge.app.EventStatisticsRequest;
import forge.app.FacadeForgeApplication;
import forge.event.FirstHourBreachEvent;
import forge.config.BacktestRequest;
import forge.config.FacadeForgeConfig;
import forge.config.MarketEventOptions;
import forge.config.RiskSettings;
import forge.config.StrategyOptions;
import forge.data.FacadeForgeData;
import forge.data.build.DatabaseBuildRequest;
import forge.data.build.DatabaseBuildResult;
import forge.data.build.DerivedDataBuildOption;
import forge.data.catalog.InstrumentDataCatalog.AvailableContractData;
import forge.data.importing.DataImportResult;
import forge.data.market.ContractTradeWindow;
import forge.engine.EventStatisticsReport;
import forge.reporting.BacktestResult;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

public class BenchmarkWorkflow {
    private static final String DEFAULT_STRATEGY_NAME = "OpeningRangeContinuation";
    private static final String DEFAULT_EVENT_NAME = "PriceCrossover";
    private static final double DEFAULT_RISK_PER_TRADE = 400.0;
    private static final double DEFAULT_MAX_DAILY_LOSS = 400.0;

    private final FacadeForgeApplication forgeApplication;
    private final FacadeForgeData forgeData;
    private final FacadeForgeConfig forgeConfig;

    public BenchmarkWorkflow() {
        /*
         * Intent: Create the benchmark workflow using the default application, data, and config facades.
         * Precondition: Default package facade singletons must be available.
         * Returns: A constructed BenchmarkWorkflow instance.
         * Postcondition: Workflow is ready to run benchmark scenarios through existing package boundaries.
         */
        this(
                FacadeForgeApplication.getTheInstance(),
                FacadeForgeData.getTheInstance(),
                FacadeForgeConfig.getTheInstance()
        );
    }

    public BenchmarkWorkflow(
            FacadeForgeApplication forgeApplication,
            FacadeForgeData forgeData,
            FacadeForgeConfig forgeConfig
    ) {
        /*
         * Intent: Create the benchmark workflow with explicit dependencies for production use or tests.
         * Precondition: Application, data, and config facades must exist.
         * Returns: A constructed BenchmarkWorkflow instance.
         * Postcondition: Workflow delegates all core behavior through the supplied facades.
         */
        if (forgeApplication == null) {
            throw new IllegalArgumentException("forgeApplication is required");
        }
        if (forgeData == null) {
            throw new IllegalArgumentException("forgeData is required");
        }
        if (forgeConfig == null) {
            throw new IllegalArgumentException("forgeConfig is required");
        }
        this.forgeApplication = forgeApplication;
        this.forgeData = forgeData;
        this.forgeConfig = forgeConfig;
    }

    /*
     * Intent: Run the full benchmark sequence: import, derived-data build, event statistics, and backtest.
     * Precondition: Request must exist, reference a valid SCID file, and the imported contract must produce an available contract window.
     * Returns: BenchmarkRunResult containing each workflow result and elapsed timing.
     * Postcondition: Database may contain rebuilt raw/derived data; workflow object state is unchanged.
     */
    public BenchmarkRunResult run(BenchmarkRunRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        Instant startedAt = Instant.now();

        DataImportResult importResult = forgeApplication.forgeApplicationAccess().importData(new DataImportRequest(
                request.getScidFilePath(),
                request.shouldRebuildExistingContract(),
                request.getImportProgressListener()
        ));
        List<ContractTradeWindow> contractWindows = List.of(resolveImportedContractWindow(importResult.getContractSymbol()));

        DatabaseBuildResult databaseBuildResult = forgeData.forgeDataAccess().runDatabaseBuild(
                new DatabaseBuildRequest(
                        contractWindows,
                        EnumSet.allOf(DerivedDataBuildOption.class),
                        request.shouldRebuildDerivedData()
                ),
                request.getDataBuildProgressListener()
        );

        Instant eventStatisticsStartedAt = Instant.now();
        EventStatisticsReport eventStatisticsReport = forgeApplication.forgeApplicationAccess().runEventStatistics(
                new EventStatisticsRequest(
                        contractWindows,
                        FirstHourBreachEvent.EVENT_NAME,
                        request.getEventStatisticsProgressListener()
                )
        );
        Duration eventStatisticsElapsedTime = Duration.between(eventStatisticsStartedAt, Instant.now());

        Instant backtestStartedAt = Instant.now();
        BacktestResult backtestResult = forgeApplication.forgeApplicationAccess().runBacktest(
                createDefaultBacktestRequest(contractWindows),
                request.getBacktestProgressListener()
        );
        Duration backtestElapsedTime = Duration.between(backtestStartedAt, Instant.now());

        return new BenchmarkRunResult(
                importResult,
                databaseBuildResult,
                eventStatisticsReport,
                backtestResult,
                eventStatisticsElapsedTime,
                backtestElapsedTime,
                Duration.between(startedAt, Instant.now())
        );
    }

    /*
     * Intent: Find the rollover-valid contract window created or refreshed by the import step.
     * Precondition: Contract symbol must identify an imported contract visible through the data facade.
     * Returns: ContractTradeWindow for the imported contract.
     * Postcondition: Workflow state is unchanged; an exception is thrown if the imported contract is not benchmarkable.
     */
    private ContractTradeWindow resolveImportedContractWindow(String contractSymbol) {
        for (AvailableContractData contractData : forgeData.forgeDataAccess().getAvailableContracts()) {
            if (contractData.getContractSymbol().equalsIgnoreCase(contractSymbol)) {
                return new ContractTradeWindow(
                        contractData.getContractSymbol(),
                        contractData.getStartDate(),
                        contractData.getEndDate()
                );
            }
        }
        throw new IllegalStateException("Imported contract has no rollover-valid benchmark window: " + contractSymbol);
    }

    /*
     * Intent: Build the default backtest request used by the benchmark workflow.
     * Precondition: Contract windows must be valid for the config facade and engine.
     * Returns: BacktestRequest using the default benchmark strategy, event, risk, and order settings.
     * Postcondition: Workflow state is unchanged.
     */
    private BacktestRequest createDefaultBacktestRequest(List<ContractTradeWindow> contractWindows) {
        return forgeConfig.forgeConfigAccess().createBacktestRequest(
                new StrategyOptions(DEFAULT_STRATEGY_NAME),
                contractWindows,
                new MarketEventOptions(DEFAULT_EVENT_NAME),
                new RiskSettings(DEFAULT_RISK_PER_TRADE, DEFAULT_MAX_DAILY_LOSS),
                forgeConfig.forgeConfigAccess().defaultOrderSettings()
        );
    }
}
