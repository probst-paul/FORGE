package forge.reporting;

public class FacadeForgeReporting {
    private static final FacadeForgeReporting THE_INSTANCE = new FacadeForgeReporting();

    public static FacadeForgeReporting getTheInstance() {
        return THE_INSTANCE;
    }

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
