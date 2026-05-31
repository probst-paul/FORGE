package forge.reporting.backtest;

import forge.reporting.PerformanceMetrics;
import forge.util.ImmutableLists;

import java.util.List;
import java.util.Objects;

public class ContractPerformanceReport {
    private final String contractSymbol;
    private final long ticksProcessed;
    private final long orderSignalsGenerated;
    private final PerformanceMetrics performanceMetrics;
    private final List<TradePerformanceReport> trades;

    public ContractPerformanceReport(
            String contractSymbol,
            long ticksProcessed,
            long orderSignalsGenerated,
            PerformanceMetrics performanceMetrics,
            List<TradePerformanceReport> trades
    ) {
        /*
         * Intent: Represent display/export-ready results for one futures contract.
         * Precondition: Contract symbol, metrics, and trade rows must be present; counts must be non-negative.
         * Returns: Constructed ContractPerformanceReport.
         * Postcondition: Trade rows are defensively copied and report fields are immutable.
         */
        if (contractSymbol == null || contractSymbol.trim().isEmpty()) {
            throw new IllegalArgumentException("contractSymbol is required");
        }
        if (ticksProcessed < 0) {
            throw new IllegalArgumentException("ticksProcessed cannot be negative");
        }
        if (orderSignalsGenerated < 0) {
            throw new IllegalArgumentException("orderSignalsGenerated cannot be negative");
        }
        this.contractSymbol = contractSymbol.trim().toUpperCase();
        this.ticksProcessed = ticksProcessed;
        this.orderSignalsGenerated = orderSignalsGenerated;
        this.performanceMetrics = Objects.requireNonNull(performanceMetrics, "performanceMetrics is required");
        this.trades = ImmutableLists.copyOfRequired(trades, "trades");
    }

    public String getContractSymbol() {
        return contractSymbol;
    }

    public long getTicksProcessed() {
        return ticksProcessed;
    }

    public long getOrderSignalsGenerated() {
        return orderSignalsGenerated;
    }

    public PerformanceMetrics getPerformanceMetrics() {
        return performanceMetrics;
    }

    public List<TradePerformanceReport> getTrades() {
        return trades;
    }
}
