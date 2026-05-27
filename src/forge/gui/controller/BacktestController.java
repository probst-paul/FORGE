package forge.gui.controller;

import forge.app.FacadeForgeApplication;
import forge.config.BacktestRequest;
import forge.gui.viewmodel.BacktestViewModel;
import forge.gui.viewmodel.GuiProgressBindings;
import forge.reporting.BacktestResult;
import javafx.concurrent.Task;

public class BacktestController {
    private final FacadeForgeApplication forgeApplication;
    private final BacktestViewModel viewModel;

    public BacktestController() {
        this(FacadeForgeApplication.getTheInstance(), new BacktestViewModel());
    }

    public BacktestController(FacadeForgeApplication forgeApplication, BacktestViewModel viewModel) {
        if (forgeApplication == null) {
            throw new IllegalArgumentException("forgeApplication is required");
        }
        if (viewModel == null) {
            throw new IllegalArgumentException("viewModel is required");
        }
        this.forgeApplication = forgeApplication;
        this.viewModel = viewModel;
    }

    public BacktestViewModel getViewModel() {
        return viewModel;
    }

    public BacktestResult runBacktest(BacktestRequest request) {
        if (request != null) {
            viewModel.setContractWindows(request.getContractWindows());
            viewModel.setStrategyName(request.getStrategyOptions().getStrategyName());
        }
        viewModel.markStarted("Running backtest...");
        try {
            BacktestResult result = forgeApplication.forgeApplicationAccess().runBacktest(
                    request,
                    GuiProgressBindings.backtestProgress(viewModel, "Running backtest...")
            );
            applyBacktestResult(result);
            return result;
        } catch (RuntimeException exception) {
            viewModel.markFailed("Could not run backtest.", exception);
            throw exception;
        }
    }

    public Task<BacktestResult> runBacktestTask(BacktestRequest request) {
        if (request != null) {
            viewModel.setContractWindows(request.getContractWindows());
            viewModel.setStrategyName(request.getStrategyOptions().getStrategyName());
        }
        return GuiControllerTasks.create(
                viewModel,
                "Running backtest...",
                "Could not run backtest.",
                task -> forgeApplication.forgeApplicationAccess().runBacktest(
                        request,
                        GuiProgressBindings.backtestProgress(task, "Running backtest...")
                ),
                this::applyBacktestResult
        );
    }

    private void applyBacktestResult(BacktestResult result) {
        viewModel.setStrategyName(result.getStrategyName());
        viewModel.setTicksProcessed(result.getTicksProcessed());
        viewModel.setOrderSignalsGenerated(result.getOrderSignalsGenerated());
        viewModel.markSucceeded(
                "Backtest complete.",
                "Processed " + result.getTicksProcessed() + " ticks and generated "
                        + result.getOrderSignalsGenerated() + " order signal(s)."
        );
    }
}
