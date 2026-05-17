package forge.config;

import forge.data.market.ContractTradeWindow;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class BacktestRequest {
    private final StrategyOptions strategyOptions;
    private final List<String> instruments;
    private final List<ContractTradeWindow> contractWindows;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final TradeTriggerOptions tradeTriggerOptions;
    private final RiskSettings riskSettings;
    private final TargetSettings targetSettings;
    private final OrderSettings orderSettings;

    public BacktestRequest(
            StrategyOptions strategyOptions,
            List<String> instruments,
            LocalDate startDate,
            LocalDate endDate,
            TradeTriggerOptions tradeTriggerOptions,
            RiskSettings riskSettings,
            TargetSettings targetSettings,
            OrderSettings orderSettings
    ) {
        this(
                strategyOptions,
                toContractWindows(instruments, startDate, endDate),
                tradeTriggerOptions,
                riskSettings,
                targetSettings,
                orderSettings
        );
    }

    public BacktestRequest(
            StrategyOptions strategyOptions,
            List<ContractTradeWindow> contractWindows,
            TradeTriggerOptions tradeTriggerOptions,
            RiskSettings riskSettings,
            TargetSettings targetSettings,
            OrderSettings orderSettings
    ) {
        this.strategyOptions = Objects.requireNonNull(strategyOptions, "strategyOptions is required");
        this.contractWindows = validateContractWindows(contractWindows);
        this.instruments = Collections.unmodifiableList(extractContractSymbols(this.contractWindows));
        this.startDate = findStartDate(this.contractWindows);
        this.endDate = findEndDate(this.contractWindows);
        this.tradeTriggerOptions = Objects.requireNonNull(tradeTriggerOptions, "tradeTriggerOptions is required");
        this.riskSettings = Objects.requireNonNull(riskSettings, "riskSettings is required");
        this.targetSettings = Objects.requireNonNull(targetSettings, "targetSettings is required");
        this.orderSettings = Objects.requireNonNull(orderSettings, "orderSettings is required");
    }

    public StrategyOptions getStrategyOptions() {
        return strategyOptions;
    }

    public List<String> getInstruments() {
        return instruments;
    }

    public List<ContractTradeWindow> getContractWindows() {
        return contractWindows;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public TradeTriggerOptions getTradeTriggerOptions() {
        return tradeTriggerOptions;
    }

    public RiskSettings getRiskSettings() {
        return riskSettings;
    }

    public TargetSettings getTargetSettings() {
        return targetSettings;
    }

    public OrderSettings getOrderSettings() {
        return orderSettings;
    }

    @Override
    public String toString() {
        return "BacktestRequest{" +
                "strategyOptions=" + strategyOptions +
                ", contractWindows=" + contractWindows +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", tradeTriggerOptions=" + tradeTriggerOptions +
                ", riskSettings=" + riskSettings +
                ", targetSettings=" + targetSettings +
                ", orderSettings=" + orderSettings +
                '}';
    }

    private static List<String> validateInstruments(List<String> instruments) {
        Objects.requireNonNull(instruments, "at least one instrument is required");
        if (instruments.isEmpty()) {
            throw new IllegalArgumentException("at least one instrument is required");
        }

        List<String> normalized = new ArrayList<>();
        for (String instrument : instruments) {
            if (instrument == null || instrument.trim().isEmpty()) {
                throw new IllegalArgumentException("instrument symbols cannot be blank");
            }
            normalized.add(instrument.trim().toUpperCase());
        }
        return Collections.unmodifiableList(normalized);
    }

    private static List<ContractTradeWindow> toContractWindows(
            List<String> instruments,
            LocalDate startDate,
            LocalDate endDate
    ) {
        List<String> normalizedInstruments = validateInstruments(instruments);
        Objects.requireNonNull(startDate, "startDate is required");
        Objects.requireNonNull(endDate, "endDate is required");
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate cannot be before startDate");
        }

        List<ContractTradeWindow> windows = new ArrayList<>();
        for (String instrument : normalizedInstruments) {
            windows.add(new ContractTradeWindow(instrument, startDate, endDate));
        }
        return windows;
    }

    private static List<ContractTradeWindow> validateContractWindows(List<ContractTradeWindow> contractWindows) {
        Objects.requireNonNull(contractWindows, "at least one contract window is required");
        if (contractWindows.isEmpty()) {
            throw new IllegalArgumentException("at least one contract window is required");
        }
        List<ContractTradeWindow> normalized = new ArrayList<>();
        for (ContractTradeWindow contractWindow : contractWindows) {
            if (contractWindow == null) {
                throw new IllegalArgumentException("contract windows cannot contain null values");
            }
            normalized.add(contractWindow);
        }
        return Collections.unmodifiableList(normalized);
    }

    private static List<String> extractContractSymbols(List<ContractTradeWindow> contractWindows) {
        List<String> contractSymbols = new ArrayList<>();
        for (ContractTradeWindow contractWindow : contractWindows) {
            contractSymbols.add(contractWindow.getContractSymbol());
        }
        return contractSymbols;
    }

    private static LocalDate findStartDate(List<ContractTradeWindow> contractWindows) {
        LocalDate startDate = null;
        for (ContractTradeWindow contractWindow : contractWindows) {
            if (startDate == null || contractWindow.getStartDate().isBefore(startDate)) {
                startDate = contractWindow.getStartDate();
            }
        }
        return startDate;
    }

    private static LocalDate findEndDate(List<ContractTradeWindow> contractWindows) {
        LocalDate endDate = null;
        for (ContractTradeWindow contractWindow : contractWindows) {
            if (endDate == null || contractWindow.getEndDate().isAfter(endDate)) {
                endDate = contractWindow.getEndDate();
            }
        }
        return endDate;
    }
}
