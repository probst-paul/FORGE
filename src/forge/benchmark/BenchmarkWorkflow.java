package forge.benchmark;

import forge.app.DataImportRequest;
import forge.app.EventStatisticsRequest;
import forge.app.FacadeForgeApplication;
import forge.condition.FirstHourBreachCondition;
import forge.config.BacktestRequest;
import forge.config.FacadeForgeConfig;
import forge.config.MarketConditionOptions;
import forge.config.RiskSettings;
import forge.config.StrategyOptions;
import forge.config.TargetSettings;
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
    private static final String DEFAULT_CONDITION_NAME = "PriceCrossover";
    private static final double DEFAULT_RISK_PER_TRADE = 400.0;
    private static final double DEFAULT_MAX_DAILY_LOSS = 400.0;
    private static final int DEFAULT_TARGET_TICKS = 1;

    private final FacadeForgeApplication forgeApplication;
    private final FacadeForgeData forgeData;
    private final FacadeForgeConfig forgeConfig;

    public BenchmarkWorkflow() {
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

        EventStatisticsReport eventStatisticsReport = forgeApplication.forgeApplicationAccess().runEventStatistics(
                new EventStatisticsRequest(
                        contractWindows,
                        FirstHourBreachCondition.EVENT_NAME,
                        request.getEventStatisticsProgressListener()
                )
        );

        BacktestResult backtestResult = forgeApplication.forgeApplicationAccess().runBacktest(
                createDefaultBacktestRequest(contractWindows),
                request.getBacktestProgressListener()
        );

        return new BenchmarkRunResult(
                importResult,
                databaseBuildResult,
                eventStatisticsReport,
                backtestResult,
                Duration.between(startedAt, Instant.now())
        );
    }

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

    private BacktestRequest createDefaultBacktestRequest(List<ContractTradeWindow> contractWindows) {
        return forgeConfig.forgeConfigAccess().createBacktestRequest(
                new StrategyOptions(DEFAULT_STRATEGY_NAME),
                contractWindows,
                new MarketConditionOptions(DEFAULT_CONDITION_NAME),
                new RiskSettings(DEFAULT_RISK_PER_TRADE, DEFAULT_MAX_DAILY_LOSS),
                TargetSettings.fixedTarget(DEFAULT_TARGET_TICKS),
                forgeConfig.forgeConfigAccess().defaultOrderSettings()
        );
    }
}
