package forge.reporting;

public class FacadeReporting {
    public BacktestResult createBacktestResult() {
        return new BacktestResult();
    }

    public PerformanceMetrics createPerformanceMetrics() {
        return new PerformanceMetrics();
    }

    public InstrumentPerformanceReport createInstrumentPerformanceReport() {
        return new InstrumentPerformanceReport();
    }

    public String summarize(BacktestResult result) {
        if (result == null) {
            throw new IllegalArgumentException("result is required");
        }
        return result.toString();
    }
}
