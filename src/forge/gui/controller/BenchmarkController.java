package forge.gui.controller;

import forge.benchmark.BenchmarkRunRequest;
import forge.benchmark.BenchmarkRunResult;
import forge.benchmark.FacadeForgeBenchmark;
import forge.gui.viewmodel.BenchmarkViewModel;
import forge.gui.viewmodel.GuiProgressBindings;
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
        viewModel.markStarted("Running benchmark workflow...");
        try {
            BenchmarkRunResult result = forgeBenchmark.forgeBenchmarkAccess().runBenchmark(new BenchmarkRunRequest(
                    scidFilePath,
                    rebuildExistingContract,
                    rebuildDerivedData,
                    GuiProgressBindings.importProgress(viewModel, "Benchmark: importing"),
                    GuiProgressBindings.dataBuildProgress(viewModel, "Benchmark: building derived data..."),
                    GuiProgressBindings.eventStatisticsProgress(viewModel, "Benchmark: running event statistics..."),
                    GuiProgressBindings.backtestProgress(viewModel, "Benchmark: running backtest...")
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
        return GuiControllerTasks.create(
                viewModel,
                "Running benchmark workflow...",
                "Could not run benchmark workflow.",
                task -> forgeBenchmark.forgeBenchmarkAccess().runBenchmark(new BenchmarkRunRequest(
                        scidFilePath,
                        rebuildExistingContract,
                        rebuildDerivedData,
                        GuiProgressBindings.importProgress(task, "Benchmark: importing"),
                        GuiProgressBindings.dataBuildProgress(task, "Benchmark: building derived data..."),
                        GuiProgressBindings.eventStatisticsProgress(task, "Benchmark: running event statistics..."),
                        GuiProgressBindings.backtestProgress(task, "Benchmark: running backtest...")
                )),
                this::applyBenchmarkResult
        );
    }

    private void applyBenchmarkResult(BenchmarkRunResult result) {
        viewModel.setRowsImported(result.getImportResult().getImportedRows());
        viewModel.setDerivedDataTicksRead(result.getDatabaseBuildResult().getTicksRead());
        viewModel.setBacktestTicksProcessed(result.getBacktestResult().getTicksProcessed());
        viewModel.markSucceeded(
                "Benchmark complete.",
                "Imported " + result.getImportResult().getImportedRows()
                        + " rows, read " + result.getDatabaseBuildResult().getTicksRead()
                        + " derived-data ticks, and processed "
                        + result.getBacktestResult().getTicksProcessed() + " backtest ticks."
        );
    }
}
