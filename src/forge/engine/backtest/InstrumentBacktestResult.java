package forge.engine.backtest;

import forge.reporting.PerformanceMetrics;
import forge.trade.TradeResult;
import forge.util.ImmutableLists;

import java.util.ArrayList;
import java.util.List;

public class InstrumentBacktestResult {
    private final String instrumentSymbol;
    private final long ticksProcessed;
    private final long orderSignalsGenerated;
    private final List<ContractBacktestResult> contractResults;
    private final PerformanceMetrics performanceMetrics;

    public InstrumentBacktestResult(String instrumentSymbol, List<ContractBacktestResult> contractResults) {
        if (instrumentSymbol == null || instrumentSymbol.trim().isEmpty()) {
            throw new IllegalArgumentException("instrumentSymbol is required");
        }
        /*
         * Intent: Aggregate contract-level backtest results into one instrument-level result.
         * Precondition: instrumentSymbol must be present and contractResults must be non-null.
         * Returns: Constructed InstrumentBacktestResult.
         * Postcondition: Tick counts, signal counts, trades, and performance metrics are aggregated once.
         */
        this.instrumentSymbol = instrumentSymbol.trim().toUpperCase();
        this.contractResults = ImmutableLists.copyOfRequired(contractResults, "contractResults");

        long ticks = 0;
        long signals = 0;
        List<TradeResult> trades = new ArrayList<>();
        for (ContractBacktestResult contractResult : contractResults) {
            ticks += contractResult.getTicksProcessed();
            signals += contractResult.getOrderSignalsGenerated();
            trades.addAll(contractResult.getTrades());
        }
        this.ticksProcessed = ticks;
        this.orderSignalsGenerated = signals;
        this.performanceMetrics = PerformanceMetrics.fromTrades(trades);
    }

    public String getInstrumentSymbol() {
        return instrumentSymbol;
    }

    public long getTicksProcessed() {
        return ticksProcessed;
    }

    public long getOrderSignalsGenerated() {
        return orderSignalsGenerated;
    }

    public List<ContractBacktestResult> getContractResults() {
        return contractResults;
    }

    public PerformanceMetrics getPerformanceMetrics() {
        return performanceMetrics;
    }
}
