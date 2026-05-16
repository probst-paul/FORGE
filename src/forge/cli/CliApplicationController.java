package forge.cli;

import forge.app.ConsoleUserInput;
import forge.app.ConsoleUserOutput;
import forge.app.DatabaseConnectionRequest;
import forge.app.FacadeForgeApplication;
import forge.app.UserInput;
import forge.app.UserOutput;
import forge.config.BacktestRequest;
import forge.config.FacadeForgeConfig;
import forge.config.RiskSettings;
import forge.config.TargetSettings;
import forge.app.DataImportRequest;
import forge.data.DataImportPlan;
import forge.data.FacadeForgeData;
import forge.data.DataImportResult;
import forge.data.PostgresDatabaseSettings;
import forge.strategy.FacadeForgeStrategy;
import forge.strategy.TradingStrategy;
import forge.target.FacadeForgeTarget;
import forge.target.TargetModel;
import forge.trigger.FacadeForgeTrigger;
import forge.trigger.TradeTrigger;

import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

public class CliApplicationController {
    private static final String TITLE_SEPARATOR = "========================================================";
    private static final String SECTION_SEPARATOR = "-------------------------";

    private final FacadeForgeApplication forgeApplication;
    private final FacadeForgeConfig forgeConfig;
    private final InstrumentSelectionService instrumentSelectionService;
    private final StrategySelectionService strategySelectionService;
    private final RiskSettingsSelectionService riskSettingsSelectionService;
    private final TriggerSelectionService triggerSelectionService;
    private final TargetModelSelectionService targetModelSelectionService;

    public CliApplicationController() {
        this(
                FacadeForgeApplication.getTheInstance(),
                FacadeForgeConfig.getTheInstance(),
                new InstrumentSelectionService(FacadeForgeData.getTheInstance()),
                new StrategySelectionService(FacadeForgeStrategy.getTheInstance()),
                new RiskSettingsSelectionService(),
                new TriggerSelectionService(FacadeForgeTrigger.getTheInstance()),
                new TargetModelSelectionService(FacadeForgeTarget.getTheInstance())
        );
    }

    public CliApplicationController(
            FacadeForgeApplication forgeApplication,
            FacadeForgeConfig forgeConfig,
            InstrumentSelectionService instrumentSelectionService,
            StrategySelectionService strategySelectionService,
            RiskSettingsSelectionService riskSettingsSelectionService,
            TriggerSelectionService triggerSelectionService,
            TargetModelSelectionService targetModelSelectionService
    ) {
        this.forgeApplication = forgeApplication;
        this.forgeConfig = forgeConfig;
        this.instrumentSelectionService = instrumentSelectionService;
        this.strategySelectionService = strategySelectionService;
        this.riskSettingsSelectionService = riskSettingsSelectionService;
        this.triggerSelectionService = triggerSelectionService;
        this.targetModelSelectionService = targetModelSelectionService;
    }

    public void run() {
        run(new ConsoleUserInput(new Scanner(System.in)), new ConsoleUserOutput());
    }

    public void run(UserInput input, UserOutput output) {
        printTitle(output);

        boolean running = true;
        while (running) {
            running = selectAction(input, output);
        }
    }

    private boolean selectAction(UserInput input, UserOutput output) {
        printSection(output, "Select Action");
        output.printLine("1. Run Backtest");
        output.printLine("2. Import Data");
        output.printLine("3. Configure Database");
        output.printLine("4. Exit");

        int selectedAction = input.readInt("Select action");
        if (selectedAction == 1) {
            runBacktestSetup(input, output);
            return false;
        }
        if (selectedAction == 2) {
            runDataImport(input, output);
            output.printBlankLine();
            return true;
        }
        if (selectedAction == 3) {
            configureDatabase(input, output);
            output.printBlankLine();
            return true;
        }
        if (selectedAction == 4) {
            output.printBlankLine();
            output.printLine("Exiting FORGE.");
            return false;
        }

        throw new IllegalArgumentException("Selected action is not available");
    }

    private void runBacktestSetup(UserInput input, UserOutput output) {
        BacktestRequest request = configureBacktest(input, output);
        BacktestRequest acceptedRequest = forgeApplication.forgeApplicationAccess().runBacktest(request);

        output.printBlankLine();
        output.printLine("Backtest request accepted:");
        output.printLine(acceptedRequest.toString());
    }

    private BacktestRequest configureBacktest(UserInput input, UserOutput output) {
        printSection(output, "Select Instrument(s)");
        List<String> instruments = instrumentSelectionService.selectInstruments(input, output);

        printSection(output, "Select Date Range");
        LocalDate[] dateRange = instrumentSelectionService.selectDateRange(input, output, instruments);

        printSection(output, "Select Trading Strategy");
        Class<? extends TradingStrategy> selectedStrategy = strategySelectionService.selectStrategy(input, output);

        printSection(output, "Risk Settings");
        RiskSettings riskSettings = riskSettingsSelectionService.readRiskSettings(input);

        printSection(output, "Select Trade Trigger");
        Class<? extends TradeTrigger> selectedTrigger = triggerSelectionService.selectTrigger(input, output);

        printSection(output, "Select Target Model");
        Class<? extends TargetModel> selectedTargetModel = targetModelSelectionService.selectTargetModel(input, output);

        printSection(output, "Target Model Options");
        TargetSettings targetSettings = targetModelSelectionService.readTargetModelSettings(input, selectedTargetModel);

        return forgeConfig.forgeConfigAccess().createBacktestRequest(
                strategySelectionService.getDisplayName(selectedStrategy),
                instruments,
                dateRange[0],
                dateRange[1],
                triggerSelectionService.getDisplayName(selectedTrigger),
                riskSettings,
                targetSettings
        );
    }

    private void runDataImport(UserInput input, UserOutput output) {
        printSection(output, "Import Data");
        String scidFilePath = input.readString("SCID data file path");
        DataImportRequest planRequest = new DataImportRequest(scidFilePath);
        DataImportPlan plan = forgeApplication.forgeApplicationAccess().planDataImport(planRequest);
        boolean rebuildExistingContract = false;

        if (plan.hasExistingContractTable()) {
            output.printBlankLine();
            output.printLine("Existing data found for " + plan.getContractSymbol() + ":");
            output.printLine("Rows: " + plan.getExistingRows());
            if (plan.getCurrentSourceFileName() != null) {
                output.printLine("Current source: " + plan.getCurrentSourceFileName());
            }
            String confirmation = input.readString("Wipe and rebuild " + plan.getContractSymbol() + " from this file? (yes/no)");
            if (!"yes".equalsIgnoreCase(confirmation.trim())) {
                output.printLine("Import canceled.");
                return;
            }
            rebuildExistingContract = true;
        }

        DataImportResult result = forgeApplication.forgeApplicationAccess().importData(
                new DataImportRequest(scidFilePath, rebuildExistingContract)
        );

        output.printBlankLine();
        output.printLine("Data storage prepared:");
        output.printLine("Database: " + result.getDatabaseName());
        output.printLine("Table: " + result.getTableName());
        output.printLine("Contract: " + result.getContractSymbol());
        output.printLine("Rows imported: " + result.getImportedRows());
    }

    private void configureDatabase(UserInput input, UserOutput output) {
        printSection(output, "Configure Database");
        PostgresDatabaseSettings defaults = PostgresDatabaseSettings.fromEnvironment();
        DatabaseConnectionRequest request = new DatabaseConnectionRequest(
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

        DatabaseConnectionRequest acceptedRequest = forgeApplication.forgeApplicationAccess().configureDatabase(request);

        output.printBlankLine();
        output.printLine("Database configured:");
        output.printLine(acceptedRequest.getHost() + ":" + acceptedRequest.getPort() + "/" + acceptedRequest.getDatabaseName());
    }

    private void printTitle(UserOutput output) {
        output.printLine(TITLE_SEPARATOR);
        output.printBlankLine();
        output.printLine("FORGE");
        output.printBlankLine();
        output.printLine(TITLE_SEPARATOR);
    }

    private void printSection(UserOutput output, String title) {
        output.printLine(SECTION_SEPARATOR);
        output.printLine(title);
        output.printLine(SECTION_SEPARATOR);
    }
}
