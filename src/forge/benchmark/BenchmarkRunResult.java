package forge.benchmark;

import forge.data.build.DatabaseBuildResult;
import forge.data.importing.DataImportResult;
import forge.engine.EventStatisticsReport;
import forge.reporting.BacktestResult;

import java.time.Duration;

public class BenchmarkRunResult {
    private final DataImportResult importResult;
    private final DatabaseBuildResult databaseBuildResult;
    private final EventStatisticsReport eventStatisticsReport;
    private final BacktestResult backtestResult;
    private final Duration elapsedTime;

    public BenchmarkRunResult(
            DataImportResult importResult,
            DatabaseBuildResult databaseBuildResult,
            EventStatisticsReport eventStatisticsReport,
            BacktestResult backtestResult,
            Duration elapsedTime
    ) {
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
        if (elapsedTime == null || elapsedTime.isNegative()) {
            throw new IllegalArgumentException("elapsedTime is required");
        }
        this.importResult = importResult;
        this.databaseBuildResult = databaseBuildResult;
        this.eventStatisticsReport = eventStatisticsReport;
        this.backtestResult = backtestResult;
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

    public Duration getElapsedTime() {
        return elapsedTime;
    }
}
