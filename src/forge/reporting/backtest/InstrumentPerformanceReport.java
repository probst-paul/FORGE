package forge.reporting.backtest;

import forge.reporting.PerformanceMetrics;
import forge.util.ImmutableLists;

import java.util.List;
import java.util.Objects;

public class InstrumentPerformanceReport {
    private final String instrumentSymbol;
    private final long ticksProcessed;
    private final long orderSignalsGenerated;
    private final PerformanceMetrics performanceMetrics;
    private final List<ContractPerformanceReport> contractReports;

    public InstrumentPerformanceReport(
            String instrumentSymbol,
            long ticksProcessed,
            long orderSignalsGenerated,
            PerformanceMetrics performanceMetrics,
            List<ContractPerformanceReport> contractReports
    ) {
        /*
         * Intent: Represent display/export-ready results for one instrument.
         * Precondition: Instrument symbol, metrics, and contract reports must be present; counts must be non-negative.
         * Returns: Constructed InstrumentPerformanceReport.
         * Postcondition: Contract reports are defensively copied and report fields are immutable.
         */
        if (instrumentSymbol == null || instrumentSymbol.trim().isEmpty()) {
            throw new IllegalArgumentException("instrumentSymbol is required");
        }
        if (ticksProcessed < 0) {
            throw new IllegalArgumentException("ticksProcessed cannot be negative");
        }
        if (orderSignalsGenerated < 0) {
            throw new IllegalArgumentException("orderSignalsGenerated cannot be negative");
        }
        this.instrumentSymbol = instrumentSymbol.trim().toUpperCase();
        this.ticksProcessed = ticksProcessed;
        this.orderSignalsGenerated = orderSignalsGenerated;
        this.performanceMetrics = Objects.requireNonNull(performanceMetrics, "performanceMetrics is required");
        this.contractReports = ImmutableLists.copyOfRequired(contractReports, "contractReports");
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

    public PerformanceMetrics getPerformanceMetrics() {
        return performanceMetrics;
    }

    public List<ContractPerformanceReport> getContractReports() {
        return contractReports;
    }
}
