package forge.cli;

import forge.app.ConsoleUserInput;
import forge.app.ConsoleUserOutput;
import forge.app.DatabaseConnectionRequest;
import forge.app.FacadeForgeApplication;
import forge.app.ImportProgress;
import forge.app.UserInput;
import forge.app.UserOutput;
import forge.app.UserQuitException;
import forge.config.BacktestRequest;
import forge.config.FacadeForgeConfig;
import forge.config.RiskSettings;
import forge.config.TargetSettings;
import forge.app.DataImportRequest;
import forge.data.FacadeForgeData;
import forge.data.importing.DataImportPlan;
import forge.data.importing.DataImportResult;
import forge.data.postgres.PostgresDatabaseSettings;
import forge.reporting.BacktestResult;
import forge.strategy.FacadeForgeStrategy;
import forge.strategy.TradingStrategy;
import forge.target.FacadeForgeTarget;
import forge.target.TargetModel;
import forge.trigger.FacadeForgeTrigger;
import forge.trigger.TradeTrigger;

import java.nio.file.Path;
import java.time.Duration;
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

    private boolean selectAction(UserInput input, UserOutput output) {
        printSection(output, "Select Action");
        output.printLine("1. Run Backtest");
        output.printLine("2. Import Data");
        output.printLine("3. Configure Database");

        while (true) {
            int selectedAction = input.readInt("Select action (or enter 'quit' to exit program)");
            if (selectedAction == 1) {
                runCliAction("run backtest setup", output, () -> runBacktestSetup(input, output));
                output.printBlankLine();
                return true;
            }
            if (selectedAction == 2) {
                runCliAction("import data", output, () -> runDataImport(input, output));
                output.printBlankLine();
                return true;
            }
            if (selectedAction == 3) {
                runCliAction("configure database", output, () -> configureDatabase(input, output));
                output.printBlankLine();
                return true;
            }

            output.printLine("Please select 1, 2, or 3, or enter 'quit' to exit program.");
        }
    }

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

    private void runBacktestSetup(UserInput input, UserOutput output) {
        BacktestRequest request = configureBacktest(input, output);
        BacktestResult result = forgeApplication.forgeApplicationAccess().runBacktest(request);

        output.printBlankLine();
        output.printLine("Backtest complete:");
        output.printLine(result.toString());
        output.printBlankLine();
        input.readString("Press Enter or type anything to return to Select Action");
    }

    private BacktestRequest configureBacktest(UserInput input, UserOutput output) {
        printSection(output, "Select Instrument(s)");
        SelectedBacktestContracts selectedContracts = instrumentSelectionService.selectContracts(input, output);

        printSection(output, "Select Trading Strategy");
        Class<? extends TradingStrategy> selectedStrategy = strategySelectionService.selectStrategy(input, output);

        printSection(output, "Risk Settings");
        RiskSettings riskSettings = riskSettingsSelectionService.readRiskSettings(input, output);

        printSection(output, "Select Trade Trigger");
        Class<? extends TradeTrigger> selectedTrigger = triggerSelectionService.selectTrigger(input, output);

        printSection(output, "Select Target Model");
        Class<? extends TargetModel> selectedTargetModel = targetModelSelectionService.selectTargetModel(input, output);

        printSection(output, "Target Model Options");
        TargetSettings targetSettings = targetModelSelectionService.readTargetModelSettings(input, output, selectedTargetModel);

        return forgeConfig.forgeConfigAccess().createBacktestRequest(
                strategySelectionService.getDisplayName(selectedStrategy),
                selectedContracts.getContractWindows(),
                triggerSelectionService.getDisplayName(selectedTrigger),
                riskSettings,
                targetSettings
        );
    }

    private void runDataImport(UserInput input, UserOutput output) {
        printSection(output, "Import Data");
        String scidFilePath;
        DataImportPlan plan;
        while (true) {
            try {
                scidFilePath = input.readString("SCID data file path");
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

        DataImportResult result = forgeApplication.forgeApplicationAccess().importData(
                new DataImportRequest(scidFilePath, rebuildExistingContract, progress -> printImportProgress(output, progress))
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

    private String importCanceledReason(String scidFilePath, DataImportPlan plan) {
        String requestedSourceFileName = Path.of(scidFilePath.trim().replace('\\', '/')).getFileName().toString();
        if (requestedSourceFileName.equalsIgnoreCase(plan.getCurrentSourceFileName())) {
            return "Import canceled: " + plan.getContractSymbol() +
                    " already contains data from this SCID file. Answer y to wipe and rebuild it.";
        }
        return "Import canceled: existing " + plan.getContractSymbol() +
                " data was kept. Answer y to wipe it and import the selected SCID file.";
    }

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

    private void printImportProgress(UserOutput output, ImportProgress progress) {
        output.printStatusLine(renderImportProgress(progress));
        if (progress.getProcessedRecords() == progress.getTotalRecords()) {
            output.finishStatusLine();
        }
    }

    private String renderImportProgress(ImportProgress progress) {
        int barWidth = 24;
        int filledWidth = (int) Math.round(progress.getCompletionRatio() * barWidth);
        StringBuilder bar = new StringBuilder();
        for (int index = 0; index < barWidth; index++) {
            bar.append(index < filledWidth ? '#' : '-');
        }
        return "Importing " + progress.getContractSymbol() +
                " [" + bar + "] " +
                progress.getCompletionPercent() + "% " +
                progress.getProcessedRecords() + "/" + progress.getTotalRecords();
    }

    private void configureDatabase(UserInput input, UserOutput output) {
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
