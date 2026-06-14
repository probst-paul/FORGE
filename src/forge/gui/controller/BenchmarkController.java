package forge.gui.controller;

import forge.benchmark.BenchmarkRunRequest;
import forge.benchmark.BenchmarkRunResult;
import forge.benchmark.FacadeForgeBenchmark;
import forge.gui.viewmodel.BenchmarkViewModel;
import forge.gui.viewmodel.GuiWorkflowTask;
import javafx.concurrent.Task;

import java.time.Duration;

public class BenchmarkController {
    private static final int BENCHMARK_STAGES = 4;
    private static final long STAGE_PROGRESS_UNITS = 1_000L;

    private final FacadeForgeBenchmark forgeBenchmark;
    private final BenchmarkViewModel viewModel;

    public BenchmarkController() {
        this(FacadeForgeBenchmark.getTheInstance(), new BenchmarkViewModel());
    }

    public BenchmarkController(FacadeForgeBenchmark forgeBenchmark, BenchmarkViewModel viewModel) {
        if (forgeBenchmark == null) {
            throw new IllegalArgumentException("forgeBenchmark is required");
        }
        if (viewModel == null) {
            throw new IllegalArgumentException("viewModel is required");
        }
        this.forgeBenchmark = forgeBenchmark;
        this.viewModel = viewModel;
    }

    public BenchmarkViewModel getViewModel() {
        return viewModel;
    }

    public Task<BenchmarkRunResult> runBenchmarkTask(
            String scidFilePath,
            boolean overwriteOverlappingData,
            boolean rebuildDerivedData
    ) {
        /*
         * Intent: Create a JavaFX background task for running the full benchmark workflow.
         * Precondition: scidFilePath must point to a .scid file; benchmark flags must reflect GUI choices.
         * Returns: Task that yields the completed benchmark result.
         * Postcondition: Benchmark progress and final summary are exposed through the view model.
         */
        viewModel.setScidFilePath(scidFilePath);
        viewModel.setOverwriteOverlappingData(overwriteOverlappingData);
        viewModel.setRebuildDerivedData(rebuildDerivedData);
        return GuiControllerTasks.create(
                viewModel,
                "Running benchmark...",
                "Could not run benchmark.",
                task -> {
                    publishBenchmarkProgress(task, 1, 0, 1, "Benchmark: starting import...");
                    return forgeBenchmark.forgeBenchmarkAccess().runBenchmark(new BenchmarkRunRequest(
                            scidFilePath,
                            overwriteOverlappingData,
                            rebuildDerivedData,
                            progress -> publishBenchmarkProgress(
                                    task,
                                    1,
                                    progress.getProcessedRecords(),
                                    progress.getTotalRecords(),
                                    "Benchmark: importing " + progress.getContractSymbol() + "..."
                            ),
                            progress -> publishBenchmarkProgress(
                                    task,
                                    2,
                                    progress.getProcessedTicks(),
                                    progress.getTotalTicks(),
                                    "Benchmark: building derived data..."
                            ),
                            progress -> publishBenchmarkProgress(
                                    task,
                                    3,
                                    progress.getProcessedTicks(),
                                    progress.getTotalTicks(),
                                    "Benchmark: running event statistics..."
                            ),
                            progress -> publishBenchmarkProgress(
                                    task,
                                    4,
                                    progress.getProcessedTicks(),
                                    progress.getTotalTicks(),
                                    "Benchmark: running backtest..."
                            )
                    ));
                },
                this::applyBenchmarkResult
        );
    }

    private void publishBenchmarkProgress(
            GuiWorkflowTask<?> task,
            int stageNumber,
            long processed,
            long total,
            String statusMessage
    ) {
        /*
         * Intent: Map benchmark sub-step progress into one aggregate JavaFX task progress value.
         * Precondition: stageNumber must identify one benchmark stage and counts must describe that stage.
         * Returns: Nothing.
         * Postcondition: Footer and dialog progress show benchmark stage number plus overall completion percent.
         */
        int normalizedStage = Math.min(BENCHMARK_STAGES, Math.max(1, stageNumber));
        long stageUnits = stageUnits(processed, total);
        long completedUnits = ((long) normalizedStage - 1L) * STAGE_PROGRESS_UNITS + stageUnits;
        task.publishFooterTitle("Task " + normalizedStage + "/" + BENCHMARK_STAGES);
        task.publishProgress(completedUnits, BENCHMARK_STAGES * STAGE_PROGRESS_UNITS);
        task.publishStatusMessage(statusMessage);
    }

    private long stageUnits(long processed, long total) {
        /*
         * Intent: Convert a benchmark stage's native progress counts into fixed-width progress units.
         * Precondition: Counts may be zero when a workflow stage has no records to process.
         * Returns: Clamped stage progress from 0 through STAGE_PROGRESS_UNITS.
         * Postcondition: Input counts are unchanged.
         */
        if (total <= 0) {
            return STAGE_PROGRESS_UNITS;
        }
        double ratio = (double) Math.max(0L, Math.min(processed, total)) / (double) total;
        return Math.round(ratio * STAGE_PROGRESS_UNITS);
    }

    private void applyBenchmarkResult(BenchmarkRunResult result) {
        /*
         * Intent: Copy completed benchmark results into GUI state.
         * Precondition: result must describe a completed benchmark run.
         * Returns: Nothing.
         * Postcondition: Benchmark dialog displays row counts and elapsed timing for each workflow stage.
         */
        viewModel.markSucceeded(
                "Benchmark complete.",
                benchmarkSummary(result)
        );
    }

    private String benchmarkSummary(BenchmarkRunResult result) {
        /*
         * Intent: Format benchmark counts and timings for the GUI result area.
         * Precondition: result must be non-null and contain all benchmark stage outputs.
         * Returns: Multi-line benchmark summary text.
         * Postcondition: Benchmark result objects are unchanged.
         */
        return "Contract: " + result.getImportResult().getContractSymbol()
                + System.lineSeparator()
                + "Rows imported: " + result.getImportResult().getImportedRows()
                + System.lineSeparator()
                + "Ticks read for derived data: " + result.getDatabaseBuildResult().getTicksRead()
                + System.lineSeparator()
                + "Backtest ticks processed: " + result.getBacktestResult().getTicksProcessed()
                + System.lineSeparator()
                + System.lineSeparator()
                + "Import time: " + formatDuration(result.getImportResult().getElapsedTime())
                + System.lineSeparator()
                + "Derived data time: " + formatDuration(result.getDatabaseBuildResult().getElapsedTime())
                + System.lineSeparator()
                + "Event statistics time: " + formatDuration(result.getEventStatisticsElapsedTime())
                + System.lineSeparator()
                + "Backtest time: " + formatDuration(result.getBacktestElapsedTime())
                + System.lineSeparator()
                + "Total benchmark time: " + formatDuration(result.getElapsedTime());
    }

    private String formatDuration(Duration duration) {
        /*
         * Intent: Render elapsed time consistently in benchmark output.
         * Precondition: duration may be null only when an upstream result omitted timing.
         * Returns: Seconds formatted to three decimal places.
         * Postcondition: Duration value is unchanged.
         */
        if (duration == null) {
            return "0.000s";
        }
        return String.format("%.3fs", duration.toNanos() / 1_000_000_000.0);
    }
}
