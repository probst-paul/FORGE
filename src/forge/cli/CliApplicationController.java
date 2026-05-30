package forge.cli;

import forge.app.ConsoleUserInput;
import forge.app.ConsoleUserOutput;
import forge.app.BacktestProgress;
import forge.app.DatabaseConnectionRequest;
import forge.app.FacadeForgeApplication;
import forge.app.ImportProgress;
import forge.app.UserInput;
import forge.app.UserOutput;
import forge.app.UserQuitException;
import forge.benchmark.BenchmarkRunRequest;
import forge.benchmark.BenchmarkRunResult;
import forge.benchmark.FacadeForgeBenchmark;
import forge.config.BacktestRequest;
import forge.config.FacadeForgeConfig;
import forge.config.RiskSettings;
import forge.config.StrategyOptions;
import forge.config.TargetSettings;
import forge.config.MarketConditionOptions;
import forge.app.DataImportRequest;
import forge.data.FacadeForgeData;
import forge.data.build.DataBuildProgress;
import forge.data.build.DatabaseBuildPlan;
import forge.data.build.DatabaseBuildRequest;
import forge.data.build.DatabaseBuildResult;
import forge.data.build.DerivedDataBuildOption;
import forge.data.importing.DataImportPlan;
import forge.data.importing.DataImportResult;
import forge.data.postgres.PostgresDatabaseSettings;
import forge.app.EventStatisticsRequest;
import forge.app.EventStatisticsProgress;
import forge.engine.EventStatisticsReport;
import forge.engine.EventStatisticsResult;
import forge.engine.FacadeForgeEngine;
import forge.reporting.BacktestResult;
import forge.strategy.FacadeForgeStrategy;
import forge.strategy.StrategyConfigurationProfile;
import forge.strategy.TradingStrategy;
import forge.condition.FacadeForgeCondition;
import forge.condition.MarketCondition;

import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Scanner;
import java.util.Set;

public class CliApplicationController {
    private static final String TITLE_SEPARATOR = "========================================================";
    private static final String SECTION_SEPARATOR = "-------------------------";

    private final FacadeForgeApplication forgeApplication;
    private final FacadeForgeConfig forgeConfig;
    private final InstrumentSelectionService instrumentSelectionService;
    private final StrategySelectionService strategySelectionService;
    private final RiskSettingsSelectionService riskSettingsSelectionService;
    private final ConditionSelectionService conditionSelectionService;
    private final TargetSettingsSelectionService targetSettingsSelectionService;

    public CliApplicationController() {
        /*
         * Intent: Create the CLI controller with default package facades and selection services.
         * Precondition: Default facade singletons must be available.
         * Returns: A constructed CliApplicationController instance.
         * Postcondition: Controller is wired for normal command-line execution.
         */
        this(
                FacadeForgeApplication.getTheInstance(),
                FacadeForgeConfig.getTheInstance(),
                new InstrumentSelectionService(FacadeForgeData.getTheInstance()),
                new StrategySelectionService(FacadeForgeStrategy.getTheInstance()),
                new RiskSettingsSelectionService(),
                new ConditionSelectionService(FacadeForgeCondition.getTheInstance()),
                new TargetSettingsSelectionService()
        );
    }

    public CliApplicationController(
            FacadeForgeApplication forgeApplication,
            FacadeForgeConfig forgeConfig,
            InstrumentSelectionService instrumentSelectionService,
            StrategySelectionService strategySelectionService,
            RiskSettingsSelectionService riskSettingsSelectionService,
            ConditionSelectionService conditionSelectionService,
            TargetSettingsSelectionService targetSettingsSelectionService
    ) {
        /*
         * Intent: Create the CLI controller with explicit dependencies for tests or alternate wiring.
         * Precondition: Dependencies should be non-null and satisfy their package contracts.
         * Returns: A constructed CliApplicationController instance.
         * Postcondition: Controller delegates workflow work to the supplied dependencies.
         */
        this.forgeApplication = forgeApplication;
        this.forgeConfig = forgeConfig;
        this.instrumentSelectionService = instrumentSelectionService;
        this.strategySelectionService = strategySelectionService;
        this.riskSettingsSelectionService = riskSettingsSelectionService;
        this.conditionSelectionService = conditionSelectionService;
        this.targetSettingsSelectionService = targetSettingsSelectionService;
    }

    /*
     * Intent: Run the CLI using System.in and console output.
     * Precondition: Standard input/output must be available.
     * Returns: Nothing.
     * Postcondition: CLI workflow runs until user exit.
     */
    public void run() {
        run(new ConsoleUserInput(new Scanner(System.in)), new ConsoleUserOutput());
    }

    /*
     * Intent: Run the top-level CLI menu loop using injected input/output adapters.
     * Precondition: Input and output adapters must exist and be usable.
     * Returns: Nothing.
     * Postcondition: Menu repeats after each action until the user enters the quit command.
     */
    public void run(UserInput input, UserOutput output) {
        try {
            printTitle(output);

            boolean running = true;
            while (running) {
                running = selectAction(input, output);
            }
        } catch (UserQuitException exception) {
            output.printBlankLine();
            output.printLine("Exiting FORGE.");
        }
    }

    /*
     * Intent: Display the main menu, dispatch the selected action, and indicate whether the CLI should continue.
     * Precondition: Input/output adapters must be active.
     * Returns: True when the menu should be shown again.
     * Postcondition: Selected action is attempted and failures are handled without terminating the menu loop.
     */
    private boolean selectAction(UserInput input, UserOutput output) {
        printSection(output, "Select Action");
        output.printLine("1. Run Backtest");
        output.printLine("2. Run Event Statistics");
        output.printLine("3. Import Data");
        output.printLine("4. Build/Refresh Derived Data");
        output.printLine("5. Configure Database");
        output.printLine("6. Run Benchmark Workflow");

        while (true) {
            int selectedAction = input.readInt("Select action (or enter 'quit' to exit program)");
            if (selectedAction == 1) {
                runCliAction("run backtest setup", output, () -> runBacktestSetup(input, output));
                output.printBlankLine();
                return true;
            }
            if (selectedAction == 2) {
                runCliAction("run event statistics", output, () -> runEventStatistics(input, output));
                output.printBlankLine();
                return true;
            }
            if (selectedAction == 3) {
                runCliAction("import data", output, () -> runDataImport(input, output));
                output.printBlankLine();
                return true;
            }
            if (selectedAction == 4) {
                runCliAction("build derived data", output, () -> runDerivedDataBuild(input, output));
                output.printBlankLine();
                return true;
            }
            if (selectedAction == 5) {
                runCliAction("configure database", output, () -> configureDatabase(input, output));
                output.printBlankLine();
                return true;
            }
            if (selectedAction == 6) {
                runCliAction("run benchmark workflow", output, () -> runBenchmarkWorkflow(input, output));
                output.printBlankLine();
                return true;
            }

            output.printLine("Please select 1, 2, 3, 4, 5, or 6, or enter 'quit' to exit program.");
        }
    }

    /*
     * Intent: Run one menu action with consistent CLI exception handling.
     * Precondition: Action name should describe the operation and action must not be null.
     * Returns: Nothing.
     * Postcondition: User quit is propagated; expected failures are reported and control returns to the menu.
     */
    private void runCliAction(String actionName, UserOutput output, Runnable action) {
        try {
            action.run();
        } catch (UserQuitException exception) {
            throw exception;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            output.printBlankLine();
            output.finishStatusLine();
            output.printLine("Could not " + actionName + ": " + exception.getMessage());
            output.printLine("Returning to Select Action.");
        } catch (RuntimeException exception) {
            output.printBlankLine();
            output.finishStatusLine();
            output.printLine("Unexpected error while trying to " + actionName + ": " + exception.getMessage());
            output.printLine("Returning to Select Action.");
        }
    }

    /*
     * Intent: Configure and run a backtest from CLI selections.
     * Precondition: Imported contract windows, strategies, and required config options must be available.
     * Returns: Nothing.
     * Postcondition: Backtest result is printed and the user is prompted before returning to the main menu.
     */
    private void runBacktestSetup(UserInput input, UserOutput output) {
        BacktestRequest request = configureBacktest(input, output);
        StatusTimer timer = StatusTimer.start();
        boolean[] backtestProgressFinished = {false};
        BacktestResult result = forgeApplication.forgeApplicationAccess().runBacktest(
                request,
                progress -> printBacktestProgress(output, progress, timer, backtestProgressFinished)
        );

        output.printBlankLine();
        output.printLine("Backtest complete:");
        output.printLine(result.toString());
        printBacktestEmptyResultGuidance(output, result);
        output.printBlankLine();
        input.readString("Press Enter or type anything to return to Select Action");
    }

    /*
     * Intent: Gather all CLI inputs needed to build a BacktestRequest.
     * Precondition: Available contracts, strategy metadata, condition metadata, and target defaults must exist.
     * Returns: Fully assembled BacktestRequest.
     * Postcondition: No backtest has run yet; selections are converted into config objects.
     */
    private BacktestRequest configureBacktest(UserInput input, UserOutput output) {
        printSection(output, "Select Instrument(s)");
        SelectedBacktestContracts selectedContracts = instrumentSelectionService.selectContracts(input, output);

        printSection(output, "Select Trading Strategy");
        Class<? extends TradingStrategy> selectedStrategy = strategySelectionService.selectStrategy(input, output);
        StrategyConfigurationProfile strategyProfile = strategySelectionService.getConfigurationProfile(selectedStrategy);

        printSection(output, "Risk Settings");
        RiskSettings riskSettings = riskSettingsSelectionService.readRiskSettings(input, output);

        printSection(output, strategyProfile.isConditionSelectionAllowed() ? "Select Market Condition" : "Market Condition");
        Class<? extends MarketCondition> selectedCondition = conditionSelectionService.selectCondition(input, output, strategyProfile);
        MarketConditionOptions conditionOptions = strategyProfile.isConditionSelectionAllowed()
                ? conditionSelectionService.readConditionOptions(input, output, selectedCondition)
                : conditionSelectionService.createDefaultConditionOptions(selectedCondition);

        printSection(output, strategyProfile.isTargetSelectionAllowed() ? "Select Target Mode" : "Target Mode");
        String selectedTargetMode = targetSettingsSelectionService.selectTargetMode(input, output, strategyProfile);

        printSection(output, "Target Options");
        TargetSettings targetSettings = targetSettingsSelectionService.readTargetSettings(
                input,
                output,
                selectedTargetMode,
                strategyProfile
        );

        return forgeConfig.forgeConfigAccess().createBacktestRequest(
                new StrategyOptions(strategySelectionService.getDisplayName(selectedStrategy)),
                selectedContracts.getContractWindows(),
                conditionOptions,
                riskSettings,
                targetSettings,
                forgeConfig.forgeConfigAccess().defaultOrderSettings()
        );
    }

    /*
     * Intent: Run event-statistics reporting from CLI-selected contract windows and statistic name.
     * Precondition: Imported contract windows and supported event-statistic queries must be available.
     * Returns: Nothing.
     * Postcondition: Event-statistics report is printed and the user is prompted before returning to the main menu.
     */
    private void runEventStatistics(UserInput input, UserOutput output) {
        printSection(output, "Select Instrument(s)");
        SelectedBacktestContracts selectedContracts = instrumentSelectionService.selectContracts(input, output);

        printSection(output, "Select Event Statistic");
        String eventName = selectEventStatistic(input, output);
        boolean[] eventStatisticsProgressFinished = {false};
        StatusTimer timer = StatusTimer.start();

        EventStatisticsReport report = forgeApplication.forgeApplicationAccess().runEventStatistics(
                new EventStatisticsRequest(
                        selectedContracts.getContractWindows(),
                        eventName,
                        progress -> printEventStatisticsProgress(output, progress, timer, eventStatisticsProgressFinished)
                )
        );

        output.printBlankLine();
        output.printLine("Event statistics complete:");
        output.printLine("Event: " + report.getEventName());
        printEventStatisticsResults(output, "Instrument Summary", report.getInstrumentResults());
        printEventStatisticsResults(output, "Contract Summary", report.getContractResults());
        printEventStatisticsEmptyResultGuidance(output, report);
        output.printBlankLine();
        input.readString("Press Enter or type anything to return to Select Action");
    }

    /*
     * Intent: Let the user choose one supported event-statistics query.
     * Precondition: Engine facade must expose at least one supported query event name.
     * Returns: Selected event statistic name.
     * Postcondition: Input is consumed until a valid selection is made or the user quits.
     */
    private String selectEventStatistic(UserInput input, UserOutput output) {
        java.util.List<String> eventNames = FacadeForgeEngine.getTheInstance()
                .forgeEngineAccess()
                .getSupportedQueryEventNames();
        if (eventNames.isEmpty()) {
            throw new IllegalStateException("No event statistics are available");
        }

        output.printLine("Available event statistics:");
        for (int i = 0; i < eventNames.size(); i++) {
            String eventName = eventNames.get(i);
            output.printLine((i + 1) + ". " + FacadeForgeEngine.getTheInstance()
                    .forgeEngineAccess()
                    .getEventStatisticDisplayName(eventName));
            output.printLine("   " + FacadeForgeEngine.getTheInstance()
                    .forgeEngineAccess()
                    .getEventStatisticDescription(eventName));
        }

        while (true) {
            int selectedIndex = input.readInt("Select event statistic") - 1;
            if (selectedIndex >= 0 && selectedIndex < eventNames.size()) {
                return eventNames.get(selectedIndex);
            }
            output.printLine("Selected event statistic is not available. Please select an available statistic, or enter 'quit' to exit program.");
        }
    }

    /*
     * Intent: Print helpful guidance when a backtest technically completes but produces little or no usable output.
     * Precondition: BacktestResult must exist.
     * Returns: Nothing.
     * Postcondition: Output may include diagnostic guidance; result object is unchanged.
     */
    private void printBacktestEmptyResultGuidance(UserOutput output, BacktestResult result) {
        if (result.getTicksProcessed() == 0) {
            output.printBlankLine();
            output.printLine("No strategy-usable ticks were available for the selected contract windows.");
            output.printLine("Check that the selected contracts have imported front-month data and strategy-usable trades.");
            return;
        }
        if (result.getOrderSignalsGenerated() == 0) {
            output.printBlankLine();
            output.printLine("No order signals were generated.");
            output.printLine("The selected strategy may have filtered out the available ticks by session/TPO period, or its required setup did not occur.");
            return;
        }
        if (totalTrades(result) == 0) {
            output.printBlankLine();
            output.printLine("Order signals were generated, but no completed trades were recorded.");
            output.printLine("The current MVP engine only records trades when a strategy decision includes a trade plan and the lifecycle can open and close the position.");
        }
    }

    /*
     * Intent: Count completed trades across all instrument-level backtest summaries.
     * Precondition: BacktestResult must exist and expose instrument results.
     * Returns: Total completed trade count.
     * Postcondition: Backtest result is unchanged.
     */
    private long totalTrades(BacktestResult result) {
        long trades = 0;
        for (forge.reporting.InstrumentBacktestResult instrumentResult : result.getInstrumentResults()) {
            trades += instrumentResult.getPerformanceMetrics().getTotalTrades();
        }
        return trades;
    }

    /*
     * Intent: Print guidance when event statistics have no complete session results.
     * Precondition: EventStatisticsReport must exist.
     * Returns: Nothing.
     * Postcondition: Output may include diagnostic guidance; report object is unchanged.
     */
    private void printEventStatisticsEmptyResultGuidance(UserOutput output, EventStatisticsReport report) {
        if (report.getInstrumentResults().isEmpty() && report.getContractResults().isEmpty()) {
            output.printBlankLine();
            output.printLine("No complete sessions were available for this statistic.");
            output.printLine("Import enough data to cover the required overnight, first-hour, and RTH windows for the selected contracts.");
        }
    }

    /*
     * Intent: Print event-statistics rows for either instrument or contract scope.
     * Precondition: Results list must exist; title should describe the scope.
     * Returns: Nothing.
     * Postcondition: Results are displayed and source result objects are unchanged.
     */
    private void printEventStatisticsResults(
            UserOutput output,
            String title,
            java.util.List<EventStatisticsResult> results
    ) {
        output.printBlankLine();
        output.printLine(title);
        output.printLine(SECTION_SEPARATOR);
        if (results.isEmpty()) {
            output.printLine("No complete sessions were available.");
            return;
        }
        for (EventStatisticsResult result : results) {
            output.printLine(result.getScopeName());
            output.printLine("Sessions Analyzed: " + result.getSessionsAnalyzed());
            output.printLine("Long Breaches: " + result.getLongEventCount());
            output.printLine("Short Breaches: " + result.getShortEventCount());
            output.printLine("No Breach: " + result.getNoEventCount());
            output.printLine(String.format("Breach Rate: %.2f%%", result.getEventRate() * 100.0));
            output.printBlankLine();
        }
    }

    /*
     * Intent: Run the SCID import workflow from CLI input.
     * Precondition: User must provide a valid .scid path and confirm rebuild when existing contract data is found.
     * Returns: Nothing.
     * Postcondition: Raw trade data may be imported/rebuilt and import summary is printed.
     */
    private void runDataImport(UserInput input, UserOutput output) {
        printSection(output, "Import Data");
        String scidFilePath;
        DataImportPlan plan;
        while (true) {
            try {
                scidFilePath = input.readString("SCID data file path (leave blank to return to Select Action)");
                if (scidFilePath.trim().isEmpty()) {
                    output.printLine("Import canceled. Returning to Select Action.");
                    return;
                }
                DataImportRequest planRequest = new DataImportRequest(scidFilePath);
                plan = forgeApplication.forgeApplicationAccess().planDataImport(planRequest);
                break;
            } catch (IllegalArgumentException exception) {
                output.printLine(exception.getMessage() + ". Please enter a valid SCID file path, or enter 'quit' to exit program.");
            }
        }
        boolean rebuildExistingContract = false;

        if (plan.hasExistingContractTable()) {
            output.printBlankLine();
            output.printLine("Existing data found for " + plan.getContractSymbol() + ":");
            output.printLine("Rows: " + plan.getExistingRows());
            if (plan.getCurrentSourceFileName() != null) {
                output.printLine("Current source: " + plan.getCurrentSourceFileName());
            }
            if (!confirmWipeAndRebuild(input, output, plan)) {
                output.printLine(importCanceledReason(scidFilePath, plan));
                return;
            }
            rebuildExistingContract = true;
        }

        StatusTimer timer = StatusTimer.start();
        boolean[] importProgressFinished = {false};
        DataImportResult result = forgeApplication.forgeApplicationAccess().importData(
                new DataImportRequest(
                        scidFilePath,
                        rebuildExistingContract,
                        progress -> printImportProgress(output, progress, timer, importProgressFinished)
                )
        );

        output.printBlankLine();
        output.printLine("Data storage prepared:");
        output.printLine("Database: " + result.getDatabaseName());
        output.printLine("Table: " + result.getTableName());
        output.printLine("Contract: " + result.getContractSymbol());
        output.printLine("Rows imported: " + result.getImportedRows());
        output.printLine("Import time: " + formatDuration(result.getElapsedTime()));
        output.printLine("Null-side rows imported: " + result.getNullSideRowsImported());
        output.printLine("Rows skipped outside front-month window: " + result.getSkippedOutsideFrontMonthRows());
        if (result.getNullSideRowsImported() > 0) {
            output.printLine("Null-side rows are stored but should be excluded from strategy calculations.");
        }
    }

    /*
     * Intent: Build or refresh selected derived-data tables from existing imported contract data.
     * Precondition: At least one valid contract window must be available and the user must select derived-data options.
     * Returns: Nothing.
     * Postcondition: Derived data may be built/rebuilt and build summary is printed.
     */
    private void runDerivedDataBuild(UserInput input, UserOutput output) {
        printSection(output, "Build/Refresh Derived Data");
        SelectedBacktestContracts selectedContracts = instrumentSelectionService.selectContracts(input, output);

        printSection(output, "Select Derived Data");
        Set<DerivedDataBuildOption> selectedOptions = selectDerivedDataBuildOptions(input, output);
        boolean rebuildExisting = confirmRebuildDerivedData(input, output);

        DatabaseBuildRequest request = new DatabaseBuildRequest(
                selectedContracts.getContractWindows(),
                selectedOptions,
                rebuildExisting
        );
        DatabaseBuildPlan plan = FacadeForgeData.getTheInstance().forgeDataAccess().planDatabaseBuild(request);
        printDatabaseBuildPlan(output, plan);

        if (!plan.hasWorkToRun()) {
            output.printBlankLine();
            output.printLine("No derived data rebuild is needed for the selected contracts.");
            output.printLine("Choose rebuild existing data if you want to force a refresh.");
            return;
        }

        boolean[] dataBuildProgressFinished = {false};
        StatusTimer timer = StatusTimer.start();
        DatabaseBuildResult result = FacadeForgeData.getTheInstance().forgeDataAccess().runDatabaseBuild(
                request,
                progress -> printDataBuildProgress(output, progress, timer, dataBuildProgressFinished)
        );

        output.printBlankLine();
        output.printLine("Derived data build complete:");
        output.printLine("Ticks read: " + result.getTicksRead());
        output.printLine("Session ranges built: " + result.getSessionRangesBuilt());
        output.printLine("Market events built: " + result.getMarketConditionOccurrencesBuilt());
        output.printLine("Build time: " + formatDuration(result.getElapsedTime()));
    }

    /*
     * Intent: Run the benchmark workflow from a SCID file path and print timing summaries.
     * Precondition: User must provide a valid .scid path and confirm rebuild when existing contract data is found.
     * Returns: Nothing.
     * Postcondition: Import, derived-data build, event statistics, and backtest may run; benchmark summary is printed.
     */
    private void runBenchmarkWorkflow(UserInput input, UserOutput output) {
        printSection(output, "Benchmark Workflow");
        String scidFilePath;
        while (true) {
            scidFilePath = input.readString("SCID data file path (leave blank to return to Select Action)");
            if (scidFilePath.trim().isEmpty()) {
                output.printLine("Benchmark canceled. Returning to Select Action.");
                return;
            }
            try {
                new DataImportRequest(scidFilePath);
                break;
            } catch (IllegalArgumentException exception) {
                output.printLine(exception.getMessage() + ". Please enter a valid SCID file path, or enter 'quit' to exit program.");
            }
        }

        DataImportPlan plan = forgeApplication.forgeApplicationAccess().planDataImport(new DataImportRequest(scidFilePath));
        boolean rebuildExistingContract = false;
        if (plan.hasExistingContractTable()) {
            output.printBlankLine();
            output.printLine("Benchmark import will rebuild existing data for " + plan.getContractSymbol() + ".");
            output.printLine("Rows: " + plan.getExistingRows());
            if (!confirmWipeAndRebuild(input, output, plan)) {
                output.printLine("Benchmark canceled. Existing " + plan.getContractSymbol() + " data was kept.");
                return;
            }
            rebuildExistingContract = true;
        }

        output.printBlankLine();
        output.printLine("Benchmark started:");

        boolean[] importFinished = {false};
        boolean[] dataBuildFinished = {false};
        boolean[] eventStatisticsFinished = {false};
        boolean[] backtestFinished = {false};
        StatusTimer[] importTimer = new StatusTimer[1];
        StatusTimer[] dataBuildTimer = new StatusTimer[1];
        StatusTimer[] eventStatisticsTimer = new StatusTimer[1];
        StatusTimer[] backtestTimer = new StatusTimer[1];

        BenchmarkRunResult result = FacadeForgeBenchmark.getTheInstance().forgeBenchmarkAccess().runBenchmark(
                new BenchmarkRunRequest(
                        scidFilePath,
                        rebuildExistingContract,
                        true,
                        progress -> printImportProgress(output, progress, timerFor(importTimer), importFinished),
                        progress -> printDataBuildProgress(output, progress, timerFor(dataBuildTimer), dataBuildFinished),
                        progress -> printEventStatisticsProgress(output, progress, timerFor(eventStatisticsTimer), eventStatisticsFinished),
                        progress -> printBacktestProgress(output, progress, timerFor(backtestTimer), backtestFinished)
                )
        );

        output.printBlankLine();
        output.printLine("Benchmark complete:");
        output.printLine("Contract: " + result.getImportResult().getContractSymbol());
        output.printLine("Rows imported: " + result.getImportResult().getImportedRows());
        output.printLine("Ticks read for derived data: " + result.getDatabaseBuildResult().getTicksRead());
        output.printLine("Backtest ticks processed: " + result.getBacktestResult().getTicksProcessed());
        output.printLine("Total benchmark time: " + formatDuration(result.getElapsedTime()));
        output.printBlankLine();
        input.readString("Press Enter or type anything to return to Select Action");
    }

    /*
     * Intent: Let the user choose which derived-data families to build.
     * Precondition: User must enter one of the displayed menu options.
     * Returns: Set of selected DerivedDataBuildOption values.
     * Postcondition: Invalid selections are rejected and reprompted.
     */
    private Set<DerivedDataBuildOption> selectDerivedDataBuildOptions(UserInput input, UserOutput output) {
        output.printLine("Available derived data:");
        output.printLine("1. Session ranges");
        output.printLine("2. First-hour breach events");
        output.printLine("3. All available derived data");

        while (true) {
            int selectedIndex = input.readInt("Select derived data option");
            if (selectedIndex == 1) {
                return EnumSet.of(DerivedDataBuildOption.SESSION_RANGES);
            }
            if (selectedIndex == 2) {
                return EnumSet.of(DerivedDataBuildOption.FIRST_HOUR_BREACH_EVENTS);
            }
            if (selectedIndex == 3) {
                return EnumSet.allOf(DerivedDataBuildOption.class);
            }
            output.printLine("Selected derived data option is not available. Please select 1, 2, or 3, or enter 'quit' to exit program.");
        }
    }

    /*
     * Intent: Ask whether existing derived data should be rebuilt.
     * Precondition: User must answer y or n, or quit.
     * Returns: True when existing derived data should be rebuilt.
     * Postcondition: Invalid answers are rejected and reprompted.
     */
    private boolean confirmRebuildDerivedData(UserInput input, UserOutput output) {
        while (true) {
            String confirmation = input.readString("Rebuild existing derived data if present? (y/n)");
            String normalizedConfirmation = confirmation.trim().toLowerCase();
            if ("y".equals(normalizedConfirmation)) {
                return true;
            }
            if ("n".equals(normalizedConfirmation)) {
                return false;
            }
            output.printLine("Please type y to rebuild existing derived data, n to keep existing derived data, or enter 'quit' to exit program.");
        }
    }

    /*
     * Intent: Print the planned derived-data build work before execution.
     * Precondition: DatabaseBuildPlan must exist.
     * Returns: Nothing.
     * Postcondition: Plan is displayed and unchanged.
     */
    private void printDatabaseBuildPlan(UserOutput output, DatabaseBuildPlan plan) {
        output.printBlankLine();
        output.printLine("Derived data build plan:");
        output.printLine("Contracts: " + plan.getContractWindows().size());
        output.printLine("Ticks available: " + plan.getTotalTicks());
        output.printLine("Rebuild existing: " + (plan.isRebuildExisting() ? "yes" : "no"));
        output.printLine("Session ranges: " + describeBuildPlanItem(
                plan.isSessionRangesAlreadyBuilt(),
                plan.willBuildSessionRanges()
        ));
        output.printLine("First-hour breach events: " + describeBuildPlanItem(
                plan.isFirstHourBreachConditionsAlreadyBuilt(),
                plan.willBuildFirstHourBreachConditions()
        ));
    }

    /*
     * Intent: Convert build-plan flags into compact user-facing text.
     * Precondition: Flags must represent whether an item exists and whether it will be built.
     * Returns: One of build, rebuild, already built, or not selected.
     * Postcondition: No state is changed.
     */
    private String describeBuildPlanItem(boolean alreadyBuilt, boolean willBuild) {
        if (willBuild) {
            return alreadyBuilt ? "rebuild" : "build";
        }
        return alreadyBuilt ? "already built" : "not selected";
    }

    /*
     * Intent: Confirm destructive replacement of existing imported contract data.
     * Precondition: DataImportPlan must describe the existing target contract.
     * Returns: True when the user confirms wipe/rebuild.
     * Postcondition: Invalid answers are rejected and reprompted; no data is changed by this method.
     */
    private boolean confirmWipeAndRebuild(UserInput input, UserOutput output, DataImportPlan plan) {
        while (true) {
            String confirmation = input.readString("Wipe and rebuild " + plan.getContractSymbol() + " from this file? (y/n)");
            String normalizedConfirmation = confirmation.trim().toLowerCase();
            if ("y".equals(normalizedConfirmation)) {
                return true;
            }
            if ("n".equals(normalizedConfirmation)) {
                return false;
            }
            output.printLine("Please type y to wipe/rebuild, n to keep the existing data, or enter 'quit' to exit program.");
        }
    }

    /*
     * Intent: Explain why a user-canceled import did not proceed.
     * Precondition: SCID path and import plan must describe the attempted import.
     * Returns: User-facing cancellation reason.
     * Postcondition: No state is changed.
     */
    private String importCanceledReason(String scidFilePath, DataImportPlan plan) {
        String requestedSourceFileName = Path.of(scidFilePath.trim().replace('\\', '/')).getFileName().toString();
        if (requestedSourceFileName.equalsIgnoreCase(plan.getCurrentSourceFileName())) {
            return "Import canceled: " + plan.getContractSymbol() +
                    " already contains data from this SCID file. Answer y to wipe and rebuild it.";
        }
        return "Import canceled: existing " + plan.getContractSymbol() +
                " data was kept. Answer y to wipe it and import the selected SCID file.";
    }

    /*
     * Intent: Format elapsed time compactly for CLI summaries and status bars.
     * Precondition: Duration should be non-null and nonnegative.
     * Returns: Human-readable duration string with millisecond precision.
     * Postcondition: Duration object is unchanged.
     */
    private String formatDuration(Duration duration) {
        long totalMillis = duration.toMillis();
        long hours = totalMillis / 3_600_000;
        long minutes = (totalMillis % 3_600_000) / 60_000;
        long seconds = (totalMillis % 60_000) / 1_000;
        long millis = totalMillis % 1_000;
        if (hours > 0) {
            return String.format("%dh %02dm %02d.%03ds", hours, minutes, seconds, millis);
        }
        if (minutes > 0) {
            return String.format("%dm %02d.%03ds", minutes, seconds, millis);
        }
        return String.format("%d.%03ds", seconds, millis);
    }

    private void printImportProgress(
            UserOutput output,
            ImportProgress progress,
            StatusTimer timer,
            boolean[] finished
    ) {
        /*
         * Intent: Render import progress as a single updating CLI status line.
         * Precondition: Progress totals must be available and finished must be a one-element mutable flag.
         * Returns: Nothing.
         * Postcondition: Final progress line is closed exactly once when import reaches completion.
         */
        if (finished[0]) {
            return;
        }
        output.printStatusLine(renderImportProgress(progress, timer));
        if (progress.getProcessedRecords() == progress.getTotalRecords()) {
            output.finishStatusLine();
            finished[0] = true;
        }
    }

    private String renderImportProgress(ImportProgress progress, StatusTimer timer) {
        /*
         * Intent: Build the import progress text shown in the one-line status bar.
         * Precondition: Progress and timer must be non-null.
         * Returns: A formatted import progress string.
         * Postcondition: Progress and timer state are not modified.
         */
        return "Importing " + progress.getContractSymbol() +
                " [" + renderProgressBar(progress.getCompletionRatio()) + "] " +
                progress.getCompletionPercent() + "% " +
                progress.getProcessedRecords() + "/" + progress.getTotalRecords() +
                " elapsed " + formatDuration(timer.elapsed());
    }

    private void printBacktestProgress(
            UserOutput output,
            BacktestProgress progress,
            StatusTimer timer,
            boolean[] finished
    ) {
        /*
         * Intent: Render backtest progress as a single updating CLI status line.
         * Precondition: Progress totals must be available and finished must be a one-element mutable flag.
         * Returns: Nothing.
         * Postcondition: Final progress line is closed exactly once when the backtest reaches completion.
         */
        if (finished[0]) {
            return;
        }
        output.printStatusLine(renderBacktestProgress(progress, timer));
        if (progress.getProcessedTicks() == progress.getTotalTicks()) {
            output.finishStatusLine();
            finished[0] = true;
        }
    }

    private String renderBacktestProgress(BacktestProgress progress, StatusTimer timer) {
        /*
         * Intent: Build the backtest progress text shown in the one-line status bar.
         * Precondition: Progress and timer must be non-null.
         * Returns: A formatted backtest progress string.
         * Postcondition: Progress and timer state are not modified.
         */
        return "Running backtest [" + renderProgressBar(progress.getCompletionRatio()) + "] " +
                progress.getCompletionPercent() + "% " +
                progress.getProcessedTicks() + "/" + progress.getTotalTicks() +
                " elapsed " + formatDuration(timer.elapsed());
    }

    private void printEventStatisticsProgress(
            UserOutput output,
            EventStatisticsProgress progress,
            StatusTimer timer,
            boolean[] finished
    ) {
        /*
         * Intent: Render event-statistics progress as a single updating CLI status line.
         * Precondition: Progress totals must be available and finished must be a one-element mutable flag.
         * Returns: Nothing.
         * Postcondition: Final progress line is closed exactly once when statistics processing completes.
         */
        if (finished[0]) {
            return;
        }
        output.printStatusLine(renderEventStatisticsProgress(progress, timer));
        if (progress.getProcessedTicks() == progress.getTotalTicks()) {
            output.finishStatusLine();
            finished[0] = true;
        }
    }

    private String renderEventStatisticsProgress(EventStatisticsProgress progress, StatusTimer timer) {
        /*
         * Intent: Build the event-statistics progress text shown in the one-line status bar.
         * Precondition: Progress and timer must be non-null.
         * Returns: A formatted event-statistics progress string.
         * Postcondition: Progress and timer state are not modified.
         */
        return "Running event statistics [" + renderProgressBar(progress.getCompletionRatio()) + "] " +
                progress.getCompletionPercent() + "% " +
                progress.getProcessedTicks() + "/" + progress.getTotalTicks() +
                " elapsed " + formatDuration(timer.elapsed());
    }

    private void printDataBuildProgress(
            UserOutput output,
            DataBuildProgress progress,
            StatusTimer timer,
            boolean[] finished
    ) {
        /*
         * Intent: Render derived-data build progress as a single updating CLI status line.
         * Precondition: Progress totals must be available and finished must be a one-element mutable flag.
         * Returns: Nothing.
         * Postcondition: Final progress line is closed exactly once when derived-data build completes.
         */
        if (finished[0]) {
            return;
        }
        output.printStatusLine(renderDataBuildProgress(progress, timer));
        if (progress.getProcessedTicks() == progress.getTotalTicks()) {
            output.finishStatusLine();
            finished[0] = true;
        }
    }

    private String renderDataBuildProgress(DataBuildProgress progress, StatusTimer timer) {
        /*
         * Intent: Build the derived-data progress text shown in the one-line status bar.
         * Precondition: Progress and timer must be non-null.
         * Returns: A formatted derived-data progress string.
         * Postcondition: Progress and timer state are not modified.
         */
        return "Building derived data [" + renderProgressBar(progress.getCompletionRatio()) + "] " +
                progress.getCompletionPercent() + "% " +
                progress.getProcessedTicks() + "/" + progress.getTotalTicks() +
                " elapsed " + formatDuration(timer.elapsed());
    }

    private String renderProgressBar(double completionRatio) {
        /*
         * Intent: Convert a completion ratio into the fixed-width text progress bar used by CLI workflows.
         * Precondition: Completion ratio should be between 0.0 and 1.0.
         * Returns: A 24-character progress bar made of filled and empty segments.
         * Postcondition: No external state is changed.
         */
        int barWidth = 24;
        int filledWidth = (int) Math.round(completionRatio * barWidth);
        StringBuilder bar = new StringBuilder();
        for (int index = 0; index < barWidth; index++) {
            bar.append(index < filledWidth ? '#' : '-');
        }
        return bar.toString();
    }

    private StatusTimer timerFor(StatusTimer[] timer) {
        /*
         * Intent: Lazily create a timer for a benchmark sub-workflow when its first progress event arrives.
         * Precondition: Timer holder must be a one-element mutable array.
         * Returns: Existing timer or newly started timer.
         * Postcondition: Timer holder contains a started timer.
         */
        if (timer[0] == null) {
            timer[0] = StatusTimer.start();
        }
        return timer[0];
    }

    private void configureDatabase(UserInput input, UserOutput output) {
        /*
         * Intent: Read database connection settings from the CLI and apply them through the app facade.
         * Precondition: User must provide valid settings or accept defaults.
         * Returns: Nothing.
         * Postcondition: Application data services use the accepted database configuration.
         */
        printSection(output, "Configure Database");
        PostgresDatabaseSettings defaults = PostgresDatabaseSettings.fromEnvironment();
        DatabaseConnectionRequest request;
        while (true) {
            try {
                request = new DatabaseConnectionRequest(
                        input.readStringOrDefault("Host [" + defaults.getHost() + "]", defaults.getHost()),
                        input.readIntOrDefault("Port [" + defaults.getPort() + "]", defaults.getPort()),
                        input.readStringOrDefault("Database name [" + defaults.getDatabaseName() + "]", defaults.getDatabaseName()),
                        input.readStringOrDefault(
                                "Maintenance database [" + defaults.getMaintenanceDatabaseName() + "]",
                                defaults.getMaintenanceDatabaseName()
                        ),
                        input.readStringOrDefault("Username [" + defaults.getUsername() + "]", defaults.getUsername()),
                        input.readStringOrDefault("Password [leave blank to keep default]", defaults.getPassword())
                );
                break;
            } catch (IllegalArgumentException exception) {
                output.printLine(exception.getMessage() + ". Please re-enter database settings, or enter 'quit' to exit program.");
            }
        }

        DatabaseConnectionRequest acceptedRequest = forgeApplication.forgeApplicationAccess().configureDatabase(request);

        output.printBlankLine();
        output.printLine("Database configured:");
        output.printLine(acceptedRequest.getHost() + ":" + acceptedRequest.getPort() + "/" + acceptedRequest.getDatabaseName());
    }

    private void printTitle(UserOutput output) {
        /*
         * Intent: Print the FORGE application title banner.
         * Precondition: Output adapter must be usable.
         * Returns: Nothing.
         * Postcondition: Title banner is written to output.
         */
        output.printLine(TITLE_SEPARATOR);
        output.printBlankLine();
        output.printLine("FORGE");
        output.printBlankLine();
        output.printLine(TITLE_SEPARATOR);
    }

    private void printSection(UserOutput output, String title) {
        /*
         * Intent: Print a visually consistent CLI section header.
         * Precondition: Output adapter must be usable and title should describe the section.
         * Returns: Nothing.
         * Postcondition: Section header is written to output.
         */
        output.printLine(SECTION_SEPARATOR);
        output.printLine(title);
        output.printLine(SECTION_SEPARATOR);
    }

    private static class StatusTimer {
        private final long startedAtNanos;

        private StatusTimer(long startedAtNanos) {
            /*
             * Intent: Store a monotonic start time for elapsed-time measurement.
             * Precondition: Start time should come from System.nanoTime().
             * Returns: A constructed StatusTimer instance.
             * Postcondition: Timer has an immutable start point.
             */
            this.startedAtNanos = startedAtNanos;
        }

        private static StatusTimer start() {
            /*
             * Intent: Start a timer using the current monotonic clock reading.
             * Precondition: System.nanoTime() must be available.
             * Returns: New StatusTimer instance.
             * Postcondition: Timer can report elapsed duration from this point.
             */
            return new StatusTimer(System.nanoTime());
        }

        private Duration elapsed() {
            /*
             * Intent: Calculate elapsed time since the timer started.
             * Precondition: Timer must have been created with a valid nanoTime value.
             * Returns: Duration between start time and current monotonic clock reading.
             * Postcondition: Timer start point is unchanged.
             */
            return Duration.ofNanos(System.nanoTime() - startedAtNanos);
        }
    }
}
