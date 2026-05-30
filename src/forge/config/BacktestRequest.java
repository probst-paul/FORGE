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
    private final MarketConditionOptions marketConditionOptions;
    private final RiskSettings riskSettings;
    private final TargetSettings targetSettings;
    private final OrderSettings orderSettings;

    public BacktestRequest(
            StrategyOptions strategyOptions,
            List<String> instruments,
            LocalDate startDate,
            LocalDate endDate,
            MarketConditionOptions marketConditionOptions,
            RiskSettings riskSettings,
            TargetSettings targetSettings,
            OrderSettings orderSettings
    ) {
        /*
         * Intent: Preserve the legacy request shape that uses symbols plus one shared date range.
         * Precondition: Strategy, instruments, dates, condition, risk, target, and order settings must be valid.
         * Returns: A constructed BacktestRequest instance.
         * Postcondition: Instrument/date inputs are converted into contract windows before normal validation.
         */
        this(
                strategyOptions,
                toContractWindows(instruments, startDate, endDate),
                marketConditionOptions,
                riskSettings,
                targetSettings,
                orderSettings
        );
    }

    public BacktestRequest(
            StrategyOptions strategyOptions,
            List<ContractTradeWindow> contractWindows,
            MarketConditionOptions marketConditionOptions,
            RiskSettings riskSettings,
            TargetSettings targetSettings,
            OrderSettings orderSettings
    ) {
        /*
         * Intent: Create the canonical backtest request using selected contract trade windows.
         * Precondition: All config objects must be non-null and at least one contract window is required.
         * Returns: A constructed BacktestRequest instance.
         * Postcondition: Contract windows and derived instrument symbols are stored as immutable lists.
         */
        this.strategyOptions = Objects.requireNonNull(strategyOptions, "strategyOptions is required");
        this.contractWindows = validateContractWindows(contractWindows);
        this.instruments = Collections.unmodifiableList(extractContractSymbols(this.contractWindows));
        this.startDate = findStartDate(this.contractWindows);
        this.endDate = findEndDate(this.contractWindows);
        this.marketConditionOptions = Objects.requireNonNull(marketConditionOptions, "marketConditionOptions is required");
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

    public MarketConditionOptions getMarketConditionOptions() {
        return marketConditionOptions;
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
        /*
         * Intent: Provide a readable diagnostic summary of the complete backtest request.
         * Precondition: Request must be constructed.
         * Returns: String representation of the request.
         * Postcondition: Request state is unchanged.
         */
        return "BacktestRequest{" +
                "strategyOptions=" + strategyOptions +
                ", contractWindows=" + contractWindows +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", marketConditionOptions=" + marketConditionOptions +
                ", riskSettings=" + riskSettings +
                ", targetSettings=" + targetSettings +
                ", orderSettings=" + orderSettings +
                '}';
    }

    private static List<String> validateInstruments(List<String> instruments) {
        /*
         * Intent: Validate and normalize legacy instrument/contract symbols.
         * Precondition: Instruments list must be non-null and contain at least one nonblank symbol.
         * Returns: Immutable uppercase symbol list.
         * Postcondition: Source list is not modified.
         */
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
        /*
         * Intent: Convert legacy symbol/date selections into contract trade windows.
         * Precondition: Symbols and dates must be valid and end date must not precede start date.
         * Returns: List of ContractTradeWindow objects.
         * Postcondition: Source inputs are unchanged.
         */
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
        /*
         * Intent: Validate canonical contract-window selections.
         * Precondition: Contract window list must be non-null, non-empty, and contain no null windows.
         * Returns: Immutable list of contract windows.
         * Postcondition: Source list is not modified.
         */
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
        /*
         * Intent: Derive the request's instrument/contract symbol list from selected windows.
         * Precondition: Contract windows must be validated and non-null.
         * Returns: Mutable symbol list for constructor normalization.
         * Postcondition: Contract windows are not modified.
         */
        List<String> contractSymbols = new ArrayList<>();
        for (ContractTradeWindow contractWindow : contractWindows) {
            contractSymbols.add(contractWindow.getContractSymbol());
        }
        return contractSymbols;
    }

    private static LocalDate findStartDate(List<ContractTradeWindow> contractWindows) {
        /*
         * Intent: Find the earliest selected contract-window start date.
         * Precondition: Contract windows must be validated and non-empty.
         * Returns: Earliest start date across selected windows.
         * Postcondition: Contract windows are not modified.
         */
        LocalDate startDate = null;
        for (ContractTradeWindow contractWindow : contractWindows) {
            if (startDate == null || contractWindow.getStartDate().isBefore(startDate)) {
                startDate = contractWindow.getStartDate();
            }
        }
        return startDate;
    }

    private static LocalDate findEndDate(List<ContractTradeWindow> contractWindows) {
        /*
         * Intent: Find the latest selected contract-window end date.
         * Precondition: Contract windows must be validated and non-empty.
         * Returns: Latest end date across selected windows.
         * Postcondition: Contract windows are not modified.
         */
        LocalDate endDate = null;
        for (ContractTradeWindow contractWindow : contractWindows) {
            if (endDate == null || contractWindow.getEndDate().isAfter(endDate)) {
                endDate = contractWindow.getEndDate();
            }
        }
        return endDate;
    }
}
