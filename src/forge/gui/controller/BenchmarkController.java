package forge.gui.controller;

import forge.app.BacktestProgressListener;
import forge.app.EventStatisticsProgressListener;
import forge.app.ImportProgressListener;
import forge.benchmark.BenchmarkRunRequest;
import forge.benchmark.BenchmarkRunResult;
import forge.benchmark.FacadeForgeBenchmark;
import forge.data.build.DataBuildProgressListener;
import forge.gui.viewmodel.BenchmarkPhaseProgress;
import forge.gui.viewmodel.BenchmarkViewModel;
import forge.gui.viewmodel.GuiProgressBindings;
import forge.gui.viewmodel.GuiWorkflowTask;
import javafx.application.Platform;
import javafx.concurrent.Task;

public class BenchmarkController {
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

    public BenchmarkRunResult runBenchmark(
            String scidFilePath,
            boolean rebuildExistingContract,
            boolean rebuildDerivedData
    ) {
        /*
         * Intent: Run the full benchmark workflow synchronously for tests or non-task callers.
         * Precondition: scidFilePath must identify a SCID file and rebuild flags must reflect user choices.
         * Returns: Completed benchmark result.
         * Postcondition: The view model is marked succeeded or failed and phase progress is finalized.
         */
        viewModel.setScidFilePath(scidFilePath);
        viewModel.setRebuildExistingContract(rebuildExistingContract);
        viewModel.setRebuildDerivedData(rebuildDerivedData);
        viewModel.resetPhaseProgress();
        viewModel.markStarted("Running benchmark workflow...");
        try {
            BenchmarkRunResult result = forgeBenchmark.forgeBenchmarkAccess().runBenchmark(new BenchmarkRunRequest(
                    scidFilePath,
                    rebuildExistingContract,
                    rebuildDerivedData,
                    importProgress(),
                    dataBuildProgress(),
                    eventStatisticsProgress(),
                    backtestProgress()
            ));
            applyBenchmarkResult(result);
            return result;
        } catch (RuntimeException exception) {
            viewModel.markFailed("Could not run benchmark workflow.", exception);
            throw exception;
        }
    }

    public Task<BenchmarkRunResult> runBenchmarkTask(
            String scidFilePath,
            boolean rebuildExistingContract,
            boolean rebuildDerivedData
    ) {
        /*
         * Intent: Create a JavaFX task for running the benchmark workflow off the UI thread.
         * Precondition: scidFilePath must identify a SCID file and rebuild flags must reflect user choices.
         * Returns: Task that yields the benchmark result.
         * Postcondition: Each benchmark phase updates its own progress model during the task.
         */
        viewModel.setScidFilePath(scidFilePath);
        viewModel.setRebuildExistingContract(rebuildExistingContract);
        viewModel.setRebuildDerivedData(rebuildDerivedData);
        viewModel.resetPhaseProgress();
        return GuiControllerTasks.create(
                viewModel,
                "Running benchmark workflow...",
                "Could not run benchmark workflow.",
                task -> forgeBenchmark.forgeBenchmarkAccess().runBenchmark(new BenchmarkRunRequest(
                        scidFilePath,
                        rebuildExistingContract,
                        rebuildDerivedData,
                        importProgress(task),
                        dataBuildProgress(task),
                        eventStatisticsProgress(task),
                        backtestProgress(task)
                )),
                this::applyBenchmarkResult
        );
    }

    private void applyBenchmarkResult(BenchmarkRunResult result) {
        /*
         * Intent: Finalize benchmark totals and phase progress after the benchmark completes.
         * Precondition: result must contain successful import, derived-data, statistics, and backtest results.
         * Returns: Nothing.
         * Postcondition: The view model exposes final benchmark counts, elapsed times, and summary text.
         */
        viewModel.setRowsImported(result.getImportResult().getImportedRows());
        viewModel.setDerivedDataTicksRead(result.getDatabaseBuildResult().getTicksRead());
        viewModel.setBacktestTicksProcessed(result.getBacktestResult().getTicksProcessed());
        long importTotalRecords = viewModel.getImportProgress().getTotalUnits();
        long completedImportRecords = importTotalRecords > 0
                ? importTotalRecords
                : result.getImportResult().getImportedRows();
        viewModel.getImportProgress().markComplete(
                completedImportRecords,
                completedImportRecords,
                "Import complete."
        );
        viewModel.getImportProgress().setElapsedTime(result.getImportResult().getElapsedTime());
        viewModel.getDerivedDataProgress().markComplete(
                result.getDatabaseBuildResult().getTicksRead(),
                result.getDatabaseBuildResult().getTicksRead(),
                "Derived data complete."
        );
        viewModel.getDerivedDataProgress().setElapsedTime(result.getDatabaseBuildResult().getElapsedTime());
        viewModel.getEventStatisticsProgress().markComplete(
                result.getEventStatisticsReport().getContractResults().size(),
                result.getEventStatisticsReport().getContractResults().size(),
                "Event statistics complete."
        );
        viewModel.getEventStatisticsProgress().setElapsedTime(result.getEventStatisticsElapsedTime());
        viewModel.getBacktestProgress().markComplete(
                result.getBacktestResult().getTicksProcessed(),
                result.getBacktestResult().getTicksProcessed(),
                "Backtest complete."
        );
        viewModel.getBacktestProgress().setElapsedTime(result.getBacktestElapsedTime());
        viewModel.markSucceeded("Benchmark complete.", viewModel.getResultSummary());
    }

    private ImportProgressListener importProgress() {
        /*
         * Intent: Adapt import progress for synchronous benchmark runs.
         * Precondition: The benchmark view model must be active.
         * Returns: Listener that updates shared and import-phase progress.
         * Postcondition: Benchmark import progress is visible in the GUI model.
         */
        return progress -> {
            GuiProgressBindings.importProgress(viewModel, "Benchmark: importing").onProgress(progress);
            viewModel.getImportProgress().updateProgress(
                    progress.getProcessedRecords(),
                    progress.getTotalRecords(),
                    "Importing " + progress.getContractSymbol() + "..."
            );
        };
    }

    private ImportProgressListener importProgress(GuiWorkflowTask<?> task) {
        /*
         * Intent: Adapt import progress for background benchmark tasks.
         * Precondition: task must be the active benchmark workflow task.
         * Returns: Listener that updates task progress and import-phase progress on the FX thread.
         * Postcondition: Benchmark import progress remains JavaFX-thread safe.
         */
        return progress -> {
            GuiProgressBindings.importProgress(task, "Benchmark: importing").onProgress(progress);
            updatePhaseOnFxThread(
                    viewModel.getImportProgress(),
                    progress.getProcessedRecords(),
                    progress.getTotalRecords(),
                    "Importing " + progress.getContractSymbol() + "..."
            );
        };
    }

    private DataBuildProgressListener dataBuildProgress() {
        return progress -> {
            GuiProgressBindings.dataBuildProgress(viewModel, "Benchmark: building derived data...").onProgress(progress);
            viewModel.getImportProgress().stopElapsedTimer();
            viewModel.getDerivedDataProgress().updateProgress(
                    progress.getProcessedTicks(),
                    progress.getTotalTicks(),
                    "Building derived data..."
            );
        };
    }

    private DataBuildProgressListener dataBuildProgress(GuiWorkflowTask<?> task) {
        return progress -> {
            GuiProgressBindings.dataBuildProgress(task, "Benchmark: building derived data...").onProgress(progress);
            runOnFxThread(() -> {
                viewModel.getImportProgress().stopElapsedTimer();
                viewModel.getDerivedDataProgress().updateProgress(
                        progress.getProcessedTicks(),
                        progress.getTotalTicks(),
                        "Building derived data..."
                );
            });
        };
    }

    private EventStatisticsProgressListener eventStatisticsProgress() {
        return progress -> {
            GuiProgressBindings.eventStatisticsProgress(viewModel, "Benchmark: running event statistics...").onProgress(progress);
            viewModel.getDerivedDataProgress().stopElapsedTimer();
            viewModel.getEventStatisticsProgress().updateProgress(
                    progress.getProcessedTicks(),
                    progress.getTotalTicks(),
                    "Running event statistics..."
            );
        };
    }

    private EventStatisticsProgressListener eventStatisticsProgress(GuiWorkflowTask<?> task) {
        return progress -> {
            GuiProgressBindings.eventStatisticsProgress(task, "Benchmark: running event statistics...").onProgress(progress);
            runOnFxThread(() -> {
                viewModel.getDerivedDataProgress().stopElapsedTimer();
                viewModel.getEventStatisticsProgress().updateProgress(
                        progress.getProcessedTicks(),
                        progress.getTotalTicks(),
                        "Running event statistics..."
                );
            });
        };
    }

    private BacktestProgressListener backtestProgress() {
        return progress -> {
            GuiProgressBindings.backtestProgress(viewModel, "Benchmark: running backtest...").onProgress(progress);
            viewModel.getEventStatisticsProgress().stopElapsedTimer();
            viewModel.getBacktestProgress().updateProgress(
                    progress.getProcessedTicks(),
                    progress.getTotalTicks(),
                    "Running backtest..."
            );
        };
    }

    private BacktestProgressListener backtestProgress(GuiWorkflowTask<?> task) {
        return progress -> {
            GuiProgressBindings.backtestProgress(task, "Benchmark: running backtest...").onProgress(progress);
            runOnFxThread(() -> {
                viewModel.getEventStatisticsProgress().stopElapsedTimer();
                viewModel.getBacktestProgress().updateProgress(
                        progress.getProcessedTicks(),
                        progress.getTotalTicks(),
                        "Running backtest..."
                );
            });
        };
    }

    private void updatePhaseOnFxThread(
            BenchmarkPhaseProgress phaseProgress,
            long processed,
            long total,
            String statusMessage
    ) {
        runOnFxThread(() -> phaseProgress.updateProgress(processed, total, statusMessage));
    }

    private void runOnFxThread(Runnable update) {
        /*
         * Intent: Apply benchmark phase updates on the JavaFX application thread.
         * Precondition: update must be non-null and quick to execute.
         * Returns: Nothing.
         * Postcondition: UI-bound benchmark properties are updated safely.
         */
        if (Platform.isFxApplicationThread()) {
            update.run();
        } else {
            Platform.runLater(update);
        }
    }
}
