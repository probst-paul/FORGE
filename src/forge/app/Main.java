package forge.app;

import forge.config.BacktestRequest;
import forge.config.OrderSettings;
import forge.config.RiskSettings;
import forge.config.StrategyOptions;
import forge.config.TargetSettings;
import forge.config.TradeTriggerOptions;
import forge.data.InstrumentDataCatalog;
import forge.data.InstrumentDataCatalog.AvailableDateRange;
import forge.data.InstrumentDataCatalog.AvailableInstrumentData;
import forge.execution.OrderType;
import forge.strategy.StrategyCatalog;
import forge.strategy.TradingStrategy;
import forge.target.TargetModel;
import forge.target.TargetModelCatalog;
import forge.trigger.TradeTrigger;
import forge.trigger.TriggerCatalog;

import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Main {
    private static final String TITLE_SEPARATOR = "========================================================";
    private static final String SECTION_SEPARATOR = "-------------------------";
    private static final OrderSettings DEFAULT_ORDER_SETTINGS = new OrderSettings(OrderType.MARKET, 1, 0, 0);

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        InstrumentDataCatalog instrumentDataCatalog = new InstrumentDataCatalog();
        StrategyCatalog strategyCatalog = new StrategyCatalog();
        TriggerCatalog triggerCatalog = new TriggerCatalog();
        TargetModelCatalog targetModelCatalog = new TargetModelCatalog();

        printTitle();
        BacktestRequest request = readBacktestRequest(
                scanner,
                instrumentDataCatalog,
                strategyCatalog,
                triggerCatalog,
                targetModelCatalog
        );
        System.out.println();
        System.out.println("Backtest request accepted:");
        System.out.println(request);
    }

    private static BacktestRequest readBacktestRequest(
            Scanner scanner,
            InstrumentDataCatalog instrumentDataCatalog,
            StrategyCatalog strategyCatalog,
            TriggerCatalog triggerCatalog,
            TargetModelCatalog targetModelCatalog
    ) {
        printSection("Select Instrument(s)");
        List<String> instruments = selectInstruments(scanner, instrumentDataCatalog);

        printSection("Select Date Range");
        LocalDate[] dateRange = selectDateRange(scanner, instrumentDataCatalog, instruments);

        printSection("Select Trading Strategy");
        Class<? extends TradingStrategy> selectedStrategy = selectStrategy(scanner, strategyCatalog);

        printSection("Risk Settings");
        double riskPerTrade = readDouble(scanner, "Risk per trade");
        double maxDailyLoss = readDouble(scanner, "Max daily loss");

        printSection("Select Trade Trigger");
        Class<? extends TradeTrigger> selectedTrigger = selectTrigger(scanner, triggerCatalog);

        printSection("Select Target Model");
        Class<? extends TargetModel> selectedTargetModel = selectTargetModel(scanner, targetModelCatalog);

        printSection("Target Model Options");
        TargetSettings targetSettings = readTargetModelSpecificOptions(
                scanner,
                targetModelCatalog.getDisplayName(selectedTargetModel)
        );

        return new BacktestRequest(
                new StrategyOptions(strategyCatalog.getDisplayName(selectedStrategy)),
                instruments,
                dateRange[0],
                dateRange[1],
                new TradeTriggerOptions(triggerCatalog.getDisplayName(selectedTrigger)),
                new RiskSettings(riskPerTrade, maxDailyLoss),
                targetSettings,
                DEFAULT_ORDER_SETTINGS
        );
    }

    private static List<String> selectInstruments(Scanner scanner, InstrumentDataCatalog instrumentDataCatalog) {
        List<AvailableInstrumentData> instruments = instrumentDataCatalog.getAvailableInstruments();
        if (instruments.isEmpty()) {
            throw new IllegalStateException("No instrument data is available");
        }

        System.out.println("Available instruments:");
        for (int i = 0; i < instruments.size(); i++) {
            System.out.println((i + 1) + ". " + instruments.get(i).getSymbol());
        }

        String rawSelections = prompt(scanner, "Select instruments (comma separated numbers)");
        List<String> selectedSymbols = java.util.Arrays.stream(rawSelections.split(","))
                .map(String::trim)
                .filter(selection -> !selection.isEmpty())
                .map(selection -> {
                    int selectedIndex = Integer.parseInt(selection) - 1;
                    if (selectedIndex < 0 || selectedIndex >= instruments.size()) {
                        throw new IllegalArgumentException("Selected instrument is not available");
                    }
                    return instruments.get(selectedIndex).getSymbol();
                })
                .collect(Collectors.toList());
        if (selectedSymbols.isEmpty()) {
            throw new IllegalArgumentException("at least one instrument must be selected");
        }
        return selectedSymbols;
    }

    private static LocalDate[] selectDateRange(
            Scanner scanner,
            InstrumentDataCatalog instrumentDataCatalog,
            List<String> instruments
    ) {
        AvailableDateRange availableDateRange = instrumentDataCatalog.getSharedDateRange(instruments);
        System.out.println("Available date range for selected instruments: " + availableDateRange);

        LocalDate startDate = readDateOrDefault(
                scanner,
                "Start date (YYYY-MM-DD, blank for earliest available)",
                availableDateRange.getStartDate()
        );
        LocalDate endDate = readDateOrDefault(
                scanner,
                "End date (YYYY-MM-DD, blank for latest available)",
                availableDateRange.getEndDate()
        );
        instrumentDataCatalog.validateDateRange(instruments, startDate, endDate);
        return new LocalDate[]{startDate, endDate};
    }

    private static Class<? extends TradingStrategy> selectStrategy(Scanner scanner, StrategyCatalog strategyCatalog) {
        List<Class<? extends TradingStrategy>> strategies = strategyCatalog.findAvailableStrategies();
        if (strategies.isEmpty()) {
            throw new IllegalStateException("No trading strategies are available");
        }

        System.out.println("Available strategies:");
        for (int i = 0; i < strategies.size(); i++) {
            System.out.println((i + 1) + ". " + strategyCatalog.getDisplayName(strategies.get(i)));
        }

        int selectedIndex = readInt(scanner, "Select strategy") - 1;
        if (selectedIndex < 0 || selectedIndex >= strategies.size()) {
            throw new IllegalArgumentException("Selected strategy is not available");
        }
        return strategies.get(selectedIndex);
    }

    private static Class<? extends TradeTrigger> selectTrigger(Scanner scanner, TriggerCatalog triggerCatalog) {
        List<Class<? extends TradeTrigger>> triggers = triggerCatalog.findAvailableTriggers();
        if (triggers.isEmpty()) {
            throw new IllegalStateException("No trade triggers are available");
        }

        System.out.println("Available trade triggers:");
        for (int i = 0; i < triggers.size(); i++) {
            System.out.println((i + 1) + ". " + triggerCatalog.getDisplayName(triggers.get(i)));
        }

        int selectedIndex = readInt(scanner, "Select trade trigger") - 1;
        if (selectedIndex < 0 || selectedIndex >= triggers.size()) {
            throw new IllegalArgumentException("Selected trade trigger is not available");
        }
        return triggers.get(selectedIndex);
    }

    private static Class<? extends TargetModel> selectTargetModel(
            Scanner scanner,
            TargetModelCatalog targetModelCatalog
    ) {
        List<Class<? extends TargetModel>> targetModels = targetModelCatalog.findAvailableTargetModels();
        if (targetModels.isEmpty()) {
            throw new IllegalStateException("No target models are available");
        }

        System.out.println("Available target models:");
        for (int i = 0; i < targetModels.size(); i++) {
            System.out.println((i + 1) + ". " + targetModelCatalog.getDisplayName(targetModels.get(i)));
        }

        int selectedIndex = readInt(scanner, "Select target model") - 1;
        if (selectedIndex < 0 || selectedIndex >= targetModels.size()) {
            throw new IllegalArgumentException("Selected target model is not available");
        }
        return targetModels.get(selectedIndex);
    }

    private static TargetSettings readTargetModelSpecificOptions(Scanner scanner, String targetModel) {
        if ("Fixed Risk/Reward".equals(targetModel)) {
            double rewardRiskRatio = readDouble(scanner, "Reward/risk ratio");
            return TargetSettings.fixedRiskReward(targetModel, rewardRiskRatio);
        }

        int profitTargetTicks = readInt(scanner, "Profit target ticks");
        return TargetSettings.fixedTarget(targetModel, profitTargetTicks);
    }

    private static void printTitle() {
        System.out.println(TITLE_SEPARATOR);
        System.out.println();
        System.out.println("FORGE - Backtest Setup");
        System.out.println();
        System.out.println(TITLE_SEPARATOR);
    }

    private static void printSection(String title) {
        System.out.println(SECTION_SEPARATOR);
        System.out.println(title);
        System.out.println(SECTION_SEPARATOR);
    }

    private static String prompt(Scanner scanner, String label) {
        System.out.print(label + ": ");
        return scanner.nextLine();
    }

    private static int readInt(Scanner scanner, String label) {
        return Integer.parseInt(prompt(scanner, label));
    }

    private static double readDouble(Scanner scanner, String label) {
        return Double.parseDouble(prompt(scanner, label));
    }

    private static LocalDate readDateOrDefault(Scanner scanner, String label, LocalDate defaultDate) {
        String value = prompt(scanner, label);
        if (value.trim().isEmpty()) {
            return defaultDate;
        }
        return LocalDate.parse(value);
    }
}
