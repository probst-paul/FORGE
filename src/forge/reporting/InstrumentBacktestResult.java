package forge.reporting;

import forge.backtest.TradeResult;

import java.util.ArrayList;
import java.util.Collections;
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
        if (contractResults == null) {
            throw new IllegalArgumentException("contractResults is required");
        }
        this.instrumentSymbol = instrumentSymbol.trim().toUpperCase();
        this.contractResults = Collections.unmodifiableList(new ArrayList<>(contractResults));

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
