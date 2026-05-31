package forge.benchmark;

import forge.data.build.DatabaseBuildResult;
import forge.data.importing.DataImportResult;
import forge.reporting.eventstatistics.EventStatisticsReport;
import forge.engine.backtest.BacktestResult;

import java.time.Duration;

public class BenchmarkRunResult {
    private final DataImportResult importResult;
    private final DatabaseBuildResult databaseBuildResult;
    private final EventStatisticsReport eventStatisticsReport;
    private final BacktestResult backtestResult;
    private final Duration eventStatisticsElapsedTime;
    private final Duration backtestElapsedTime;
    private final Duration elapsedTime;

    public BenchmarkRunResult(
            DataImportResult importResult,
            DatabaseBuildResult databaseBuildResult,
            EventStatisticsReport eventStatisticsReport,
            BacktestResult backtestResult,
            Duration eventStatisticsElapsedTime,
            Duration backtestElapsedTime,
            Duration elapsedTime
    ) {
        /*
         * Intent: Store the outputs and timing measurements from a completed benchmark workflow.
         * Precondition: All result objects must exist and all durations must be non-null and nonnegative.
         * Returns: A constructed BenchmarkRunResult instance.
         * Postcondition: Benchmark outputs are available as immutable references for CLI/GUI reporting.
         */
        if (importResult == null) {
            throw new IllegalArgumentException("importResult is required");
        }
        if (databaseBuildResult == null) {
            throw new IllegalArgumentException("databaseBuildResult is required");
        }
        if (eventStatisticsReport == null) {
            throw new IllegalArgumentException("eventStatisticsReport is required");
        }
        if (backtestResult == null) {
            throw new IllegalArgumentException("backtestResult is required");
        }
        if (eventStatisticsElapsedTime == null || eventStatisticsElapsedTime.isNegative()) {
            throw new IllegalArgumentException("eventStatisticsElapsedTime is required");
        }
        if (backtestElapsedTime == null || backtestElapsedTime.isNegative()) {
            throw new IllegalArgumentException("backtestElapsedTime is required");
        }
        if (elapsedTime == null || elapsedTime.isNegative()) {
            throw new IllegalArgumentException("elapsedTime is required");
        }
        this.importResult = importResult;
        this.databaseBuildResult = databaseBuildResult;
        this.eventStatisticsReport = eventStatisticsReport;
        this.backtestResult = backtestResult;
        this.eventStatisticsElapsedTime = eventStatisticsElapsedTime;
        this.backtestElapsedTime = backtestElapsedTime;
        this.elapsedTime = elapsedTime;
    }

    public DataImportResult getImportResult() {
        return importResult;
    }

    public DatabaseBuildResult getDatabaseBuildResult() {
        return databaseBuildResult;
    }

    public EventStatisticsReport getEventStatisticsReport() {
        return eventStatisticsReport;
    }

    public BacktestResult getBacktestResult() {
        return backtestResult;
    }

    public Duration getEventStatisticsElapsedTime() {
        return eventStatisticsElapsedTime;
    }

    public Duration getBacktestElapsedTime() {
        return backtestElapsedTime;
    }

    public Duration getElapsedTime() {
        return elapsedTime;
    }
}
