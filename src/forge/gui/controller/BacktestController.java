package forge.gui.controller;

import forge.app.FacadeForgeApplication;
import forge.config.BacktestRequest;
import forge.config.FacadeForgeConfig;
import forge.config.MarketEventOptions;
import forge.config.RiskSettings;
import forge.config.StrategyOptions;
import forge.event.FacadeForgeEvent;
import forge.event.MarketEvent;
import forge.data.FacadeForgeData;
import forge.data.catalog.InstrumentDataCatalog.AvailableContractData;
import forge.data.market.ContractTradeWindow;
import forge.gui.viewmodel.BacktestViewModel;
import forge.gui.viewmodel.GuiProgressBindings;
import forge.reporting.BacktestResult;
import forge.strategy.FacadeForgeStrategy;
import forge.strategy.StrategyConfigurationProfile;
import forge.strategy.TradingStrategy;
import javafx.concurrent.Task;

import java.util.List;

public class BacktestController {
    private final FacadeForgeApplication forgeApplication;
    private final FacadeForgeData forgeData;
    private final FacadeForgeStrategy forgeStrategy;
    private final FacadeForgeEvent forgeEvent;
    private final FacadeForgeConfig forgeConfig;
    private final BacktestViewModel viewModel;

    public BacktestController() {
        this(
                FacadeForgeApplication.getTheInstance(),
                FacadeForgeData.getTheInstance(),
                FacadeForgeStrategy.getTheInstance(),
                FacadeForgeEvent.getTheInstance(),
                FacadeForgeConfig.getTheInstance(),
                new BacktestViewModel()
        );
    }

    public BacktestController(FacadeForgeApplication forgeApplication, BacktestViewModel viewModel) {
        this(
                forgeApplication,
                FacadeForgeData.getTheInstance(),
                FacadeForgeStrategy.getTheInstance(),
                FacadeForgeEvent.getTheInstance(),
                FacadeForgeConfig.getTheInstance(),
                viewModel
        );
    }

    public BacktestController(
            FacadeForgeApplication forgeApplication,
            FacadeForgeData forgeData,
            FacadeForgeStrategy forgeStrategy,
            FacadeForgeEvent forgeEvent,
            FacadeForgeConfig forgeConfig,
            BacktestViewModel viewModel
    ) {
        if (forgeApplication == null) {
            throw new IllegalArgumentException("forgeApplication is required");
        }
        if (forgeData == null) {
            throw new IllegalArgumentException("forgeData is required");
        }
        if (forgeStrategy == null) {
            throw new IllegalArgumentException("forgeStrategy is required");
        }
        if (forgeEvent == null) {
            throw new IllegalArgumentException("forgeEvent is required");
        }
        if (forgeConfig == null) {
            throw new IllegalArgumentException("forgeConfig is required");
        }
        if (viewModel == null) {
            throw new IllegalArgumentException("viewModel is required");
        }
        this.forgeApplication = forgeApplication;
        this.forgeData = forgeData;
        this.forgeStrategy = forgeStrategy;
        this.forgeEvent = forgeEvent;
        this.forgeConfig = forgeConfig;
        this.viewModel = viewModel;
    }

    public BacktestViewModel getViewModel() {
        return viewModel;
    }

    public List<AvailableContractData> getAvailableContracts() {
        return forgeData.forgeDataAccess().getAvailableContracts();
    }

    public List<Class<? extends TradingStrategy>> getAvailableStrategies() {
        return forgeStrategy.forgeStrategyAccess().findAvailableStrategies();
    }

    public String getStrategyDisplayName(Class<? extends TradingStrategy> strategyClass) {
        return forgeStrategy.forgeStrategyAccess().getDisplayName(strategyClass);
    }

    public String getStrategyDescription(Class<? extends TradingStrategy> strategyClass) {
        return forgeStrategy.forgeStrategyAccess().getDescription(strategyClass);
    }

    public StrategyConfigurationProfile getStrategyConfigurationProfile(Class<? extends TradingStrategy> strategyClass) {
        return forgeStrategy.forgeStrategyAccess().getConfigurationProfile(strategyClass);
    }

    public String getConditionDisplayName(Class<? extends MarketEvent> eventClass) {
        return forgeEvent.forgeEventAccess().getDisplayName(eventClass);
    }

    public BacktestRequest createBacktestRequest(
            Class<? extends TradingStrategy> strategyClass,
            List<ContractTradeWindow> contractWindows,
            Class<? extends MarketEvent> eventClass,
            RiskSettings riskSettings
    ) {
        /*
         * Intent: Translate GUI selections into a complete backtest request.
         * Precondition: Strategy, contract windows, event, and risk settings must be selected.
         * Returns: BacktestRequest ready for the application facade.
         * Postcondition: Strategy and event class selections are converted into config options.
         */
        StrategyOptions strategyOptions = forgeStrategy.forgeStrategyAccess().createStrategyOptions(strategyClass);
        MarketEventOptions eventOptions = forgeEvent.forgeEventAccess().createEventOptions(eventClass);
        return forgeConfig.forgeConfigAccess().createBacktestRequest(
                strategyOptions,
                contractWindows,
                eventOptions,
                riskSettings,
                forgeConfig.forgeConfigAccess().defaultOrderSettings()
        );
    }

    public BacktestResult runBacktest(BacktestRequest request) {
        /*
         * Intent: Run a backtest synchronously for tests or non-task GUI callers.
         * Precondition: request must be a complete backtest configuration.
         * Returns: Completed backtest result.
         * Postcondition: The view model is marked succeeded or failed with run totals.
         */
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
        /*
         * Intent: Create a JavaFX task for running a backtest off the UI thread.
         * Precondition: request must be a complete backtest configuration.
         * Returns: Task that yields the backtest result.
         * Postcondition: Task progress callbacks update the view model through JavaFX bindings.
         */
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
        /*
         * Intent: Copy completed backtest totals into GUI state.
         * Precondition: result must be a successful backtest result.
         * Returns: Nothing.
         * Postcondition: The view model exposes strategy name, tick count, order signals, and success text.
         */
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
