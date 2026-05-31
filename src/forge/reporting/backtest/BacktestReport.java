package forge.reporting.backtest;

import forge.util.ImmutableLists;

import java.util.ArrayList;
import java.util.List;

public class BacktestReport {
    private final String strategyName;
    private final List<String> contractSymbols;
    private final long ticksProcessed;
    private final long orderSignalsGenerated;
    private final List<InstrumentPerformanceReport> instrumentReports;
    private final List<ContractPerformanceReport> contractReports;
    private final List<TradePerformanceReport> trades;

    public BacktestReport(
            String strategyName,
            List<String> contractSymbols,
            long ticksProcessed,
            long orderSignalsGenerated,
            List<InstrumentPerformanceReport> instrumentReports
    ) {
        /*
         * Intent: Represent a complete backtest report in a display/export-friendly shape.
         * Precondition: Strategy name, contract symbols, and instrument reports must be present; counts must be non-negative.
         * Returns: Constructed BacktestReport.
         * Postcondition: Contract and trade row lists are flattened once for convenient consumers.
         */
        if (strategyName == null || strategyName.trim().isEmpty()) {
            throw new IllegalArgumentException("strategyName is required");
        }
        if (ticksProcessed < 0) {
            throw new IllegalArgumentException("ticksProcessed cannot be negative");
        }
        if (orderSignalsGenerated < 0) {
            throw new IllegalArgumentException("orderSignalsGenerated cannot be negative");
        }
        this.strategyName = strategyName.trim();
        this.contractSymbols = ImmutableLists.copyOfRequired(contractSymbols, "contractSymbols");
        this.ticksProcessed = ticksProcessed;
        this.orderSignalsGenerated = orderSignalsGenerated;
        this.instrumentReports = ImmutableLists.copyOfRequired(instrumentReports, "instrumentReports");

        List<ContractPerformanceReport> flattenedContracts = new ArrayList<>();
        List<TradePerformanceReport> flattenedTrades = new ArrayList<>();
        for (InstrumentPerformanceReport instrumentReport : this.instrumentReports) {
            flattenedContracts.addAll(instrumentReport.getContractReports());
        }
        for (ContractPerformanceReport contractReport : flattenedContracts) {
            flattenedTrades.addAll(contractReport.getTrades());
        }
        this.contractReports = ImmutableLists.copyOfRequired(flattenedContracts, "contractReports");
        this.trades = ImmutableLists.copyOfRequired(flattenedTrades, "trades");
    }

    public String getStrategyName() {
        return strategyName;
    }

    public List<String> getContractSymbols() {
        return contractSymbols;
    }

    public long getTicksProcessed() {
        return ticksProcessed;
    }

    public long getOrderSignalsGenerated() {
        return orderSignalsGenerated;
    }

    public List<InstrumentPerformanceReport> getInstrumentReports() {
        return instrumentReports;
    }

    public List<ContractPerformanceReport> getContractReports() {
        return contractReports;
    }

    public List<TradePerformanceReport> getTrades() {
        return trades;
    }
}
