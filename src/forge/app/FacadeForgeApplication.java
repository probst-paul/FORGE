package forge.app;

import forge.config.BacktestRequest;
import forge.config.FacadeBacktestConfiguration;
import forge.config.RiskSettings;
import forge.config.TargetSettings;
import forge.data.InstrumentDataCatalog;
import forge.strategy.StrategyCatalog;
import forge.strategy.TradingStrategy;
import forge.target.TargetModel;
import forge.target.TargetModelCatalog;
import forge.trigger.TradeTrigger;
import forge.trigger.TriggerCatalog;

import java.time.LocalDate;
import java.util.List;

public class FacadeForgeApplication {
    private static final String TITLE_SEPARATOR = "========================================================";
    private static final String SECTION_SEPARATOR = "-------------------------";

    private final FacadeBacktestConfiguration backtestConfigurationFacade;
    private final InstrumentSelectionService instrumentSelectionService;
    private final StrategySelectionService strategySelectionService;
    private final RiskSettingsSelectionService riskSettingsSelectionService;
    private final TriggerSelectionService triggerSelectionService;
    private final TargetModelSelectionService targetModelSelectionService;

    public FacadeForgeApplication() {
        this(
                new InstrumentDataCatalog(),
                new StrategyCatalog(),
                new TriggerCatalog(),
                new TargetModelCatalog()
        );
    }

    public FacadeForgeApplication(
            InstrumentDataCatalog instrumentDataCatalog,
            StrategyCatalog strategyCatalog,
            TriggerCatalog triggerCatalog,
            TargetModelCatalog targetModelCatalog
    ) {
        this(
                new FacadeBacktestConfiguration(),
                new InstrumentSelectionService(instrumentDataCatalog),
                new StrategySelectionService(strategyCatalog),
                new RiskSettingsSelectionService(),
                new TriggerSelectionService(triggerCatalog),
                new TargetModelSelectionService(targetModelCatalog)
        );
    }

    public FacadeForgeApplication(
            FacadeBacktestConfiguration backtestConfigurationFacade,
            InstrumentSelectionService instrumentSelectionService,
            StrategySelectionService strategySelectionService,
            RiskSettingsSelectionService riskSettingsSelectionService,
            TriggerSelectionService triggerSelectionService,
            TargetModelSelectionService targetModelSelectionService
    ) {
        this.backtestConfigurationFacade = backtestConfigurationFacade;
        this.instrumentSelectionService = instrumentSelectionService;
        this.strategySelectionService = strategySelectionService;
        this.riskSettingsSelectionService = riskSettingsSelectionService;
        this.triggerSelectionService = triggerSelectionService;
        this.targetModelSelectionService = targetModelSelectionService;
    }

    public void runBacktestSetup(UserInput input, UserOutput output) {
        printTitle(output);
        BacktestRequest request = configureBacktest(input, output);

        output.printBlankLine();
        output.printLine("Backtest request accepted:");
        output.printLine(request.toString());
    }

    public BacktestRequest configureBacktest(UserInput input, UserOutput output) {
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

        return backtestConfigurationFacade.createBacktestRequest(
                strategySelectionService.getDisplayName(selectedStrategy),
                instruments,
                dateRange[0],
                dateRange[1],
                triggerSelectionService.getDisplayName(selectedTrigger),
                riskSettings,
                targetSettings
        );
    }

    private void printTitle(UserOutput output) {
        output.printLine(TITLE_SEPARATOR);
        output.printBlankLine();
        output.printLine("FORGE - Backtest Setup");
        output.printBlankLine();
        output.printLine(TITLE_SEPARATOR);
    }

    private void printSection(UserOutput output, String title) {
        output.printLine(SECTION_SEPARATOR);
        output.printLine(title);
        output.printLine(SECTION_SEPARATOR);
    }
}
