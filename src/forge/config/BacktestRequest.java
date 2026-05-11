package forge.config;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class BacktestRequest {
    private final StrategyOptions strategyOptions;
    private final List<String> instruments;
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
        this.strategyOptions = Objects.requireNonNull(strategyOptions, "strategyOptions is required");
        this.instruments = validateInstruments(instruments);
        this.startDate = Objects.requireNonNull(startDate, "startDate is required");
        this.endDate = Objects.requireNonNull(endDate, "endDate is required");
        this.tradeTriggerOptions = Objects.requireNonNull(tradeTriggerOptions, "tradeTriggerOptions is required");
        this.riskSettings = Objects.requireNonNull(riskSettings, "riskSettings is required");
        this.targetSettings = Objects.requireNonNull(targetSettings, "targetSettings is required");
        this.orderSettings = Objects.requireNonNull(orderSettings, "orderSettings is required");

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate cannot be before startDate");
        }
    }

    public StrategyOptions getStrategyOptions() {
        return strategyOptions;
    }

    public List<String> getInstruments() {
        return instruments;
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
                ", instruments=" + instruments +
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
}
