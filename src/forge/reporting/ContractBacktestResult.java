package forge.reporting;

import forge.backtest.TradeResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ContractBacktestResult {
    private final String contractSymbol;
    private final long ticksProcessed;
    private final long orderSignalsGenerated;
    private final List<TradeResult> trades;
    private final PerformanceMetrics performanceMetrics;

    public ContractBacktestResult(
            String contractSymbol,
            long ticksProcessed,
            long orderSignalsGenerated,
            List<TradeResult> trades
    ) {
        if (contractSymbol == null || contractSymbol.trim().isEmpty()) {
            throw new IllegalArgumentException("contractSymbol is required");
        }
        if (ticksProcessed < 0) {
            throw new IllegalArgumentException("ticksProcessed cannot be negative");
        }
        if (orderSignalsGenerated < 0) {
            throw new IllegalArgumentException("orderSignalsGenerated cannot be negative");
        }
        if (trades == null) {
            throw new IllegalArgumentException("trades is required");
        }
        this.contractSymbol = contractSymbol.trim().toUpperCase();
        this.ticksProcessed = ticksProcessed;
        this.orderSignalsGenerated = orderSignalsGenerated;
        this.trades = Collections.unmodifiableList(new ArrayList<>(trades));
        this.performanceMetrics = PerformanceMetrics.fromTrades(this.trades);
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

    public List<TradeResult> getTrades() {
        return trades;
    }

    public PerformanceMetrics getPerformanceMetrics() {
        return performanceMetrics;
    }
}
