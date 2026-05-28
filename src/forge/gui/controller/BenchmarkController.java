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
        if (Platform.isFxApplicationThread()) {
            update.run();
        } else {
            Platform.runLater(update);
        }
    }
}
