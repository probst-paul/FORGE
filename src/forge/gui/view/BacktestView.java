package forge.gui.view;

import forge.event.MarketEvent;
import forge.config.BacktestRequest;
import forge.config.RiskSettings;
import forge.config.TargetSettings;
import forge.data.catalog.InstrumentDataCatalog.AvailableContractData;
import forge.data.market.ContractTradeWindow;
import forge.gui.controller.BacktestController;
import forge.gui.viewmodel.BacktestViewModel;
import forge.model.FuturesInstrumentSpec;
import forge.model.StaticFuturesInstrumentSpecProvider;
import forge.reporting.BacktestResult;
import forge.reporting.ContractBacktestResult;
import forge.reporting.InstrumentBacktestResult;
import forge.reporting.PerformanceMetrics;
import forge.strategy.StrategyConfigurationProfile;
import forge.strategy.TradingStrategy;
import forge.trade.TradeResult;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class BacktestView {
    private final BacktestController controller;
    private final StaticFuturesInstrumentSpecProvider instrumentSpecProvider = new StaticFuturesInstrumentSpecProvider();

    public BacktestView(BacktestController controller) {
        if (controller == null) {
            throw new IllegalArgumentException("controller is required");
        }
        this.controller = controller;
    }

    public Parent createView() {
        /*
         * Intent: Build the backtest screen and wire strategy, condition, target, and contract selections.
         * Precondition: Controller must be initialized and imported contract windows may be available.
         * Returns: JavaFX parent node for the backtest workflow.
         * Postcondition: View controls are bound to the backtest view model.
         */
        BacktestViewModel viewModel = controller.getViewModel();

        Label heading = new Label("Backtest");
        heading.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Label strategyLabel = new Label("Strategy");
        ComboBox<StrategySelection> strategyComboBox = new ComboBox<>();
        strategyComboBox.setMaxWidth(Double.MAX_VALUE);

        Label strategyDescription = new Label();
        strategyDescription.setWrapText(true);

        Label contractLabel = new Label("Contract windows");
        VBox contractList = new VBox(6);
        contractList.setPadding(new Insets(8));
        contractList.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d7dde3;");

        ScrollPane contractScrollPane = new ScrollPane(contractList);
        contractScrollPane.setFitToWidth(true);
        contractScrollPane.setPrefViewportHeight(190);
        contractScrollPane.setMinHeight(140);

        List<ContractSelection> contractSelections = new ArrayList<>();
        Button refreshButton = new Button("Refresh Contracts");
        refreshButton.disableProperty().bind(viewModel.runningProperty());
        refreshButton.setOnAction(event -> loadAvailableContracts(contractList, contractSelections));

        CheckBox perTradeRiskEnabledCheckBox = new CheckBox("Enable per-trade risk");
        perTradeRiskEnabledCheckBox.setSelected(true);
        TextField riskPerTradeField = new TextField("400");
        riskPerTradeField.disableProperty().bind(perTradeRiskEnabledCheckBox.selectedProperty().not());
        CheckBox dailyRiskEnabledCheckBox = new CheckBox("Enable daily risk");
        dailyRiskEnabledCheckBox.setSelected(true);
        TextField maxDailyLossField = new TextField("400");
        maxDailyLossField.disableProperty().bind(dailyRiskEnabledCheckBox.selectedProperty().not());

        ComboBox<ConditionSelection> conditionComboBox = new ComboBox<>();
        conditionComboBox.setMaxWidth(Double.MAX_VALUE);
        Label conditionMessage = new Label();
        conditionMessage.setWrapText(true);

        ComboBox<String> targetModeComboBox = new ComboBox<>();
        targetModeComboBox.setMaxWidth(Double.MAX_VALUE);
        Label targetMessage = new Label();
        targetMessage.setWrapText(true);

        TextField rewardRiskRatioField = new TextField();
        TextField profitTargetTicksField = new TextField();

        strategyComboBox.setOnAction(event -> applyStrategySelection(
                strategyComboBox.getValue(),
                strategyDescription,
                conditionComboBox,
                conditionMessage,
                targetModeComboBox,
                targetMessage,
                rewardRiskRatioField,
                profitTargetTicksField
        ));
        targetModeComboBox.setOnAction(event -> applyTargetSelection(
                strategyComboBox.getValue(),
                targetModeComboBox.getValue(),
                rewardRiskRatioField,
                profitTargetTicksField
        ));

        Button runButton = new Button("Run Backtest");
        runButton.disableProperty().bind(viewModel.runningProperty());

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.progressProperty().bind(viewModel.progressProperty());
        progressBar.setMaxWidth(Double.MAX_VALUE);

        Label statusLabel = new Label();
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());
        VBox progressSection = createProgressSection(viewModel, progressBar, statusLabel);

        TabPane resultsTabs = createResultsTabs(viewModel);

        Label errorLabel = new Label();
        errorLabel.textProperty().bind(viewModel.errorMessageProperty());
        errorLabel.setStyle("-fx-text-fill: #b00020;");

        HBox actions = new HBox(8, refreshButton, runButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox setupContent = new VBox(10);
        setupContent.setPadding(new Insets(8));
        setupContent.getChildren().addAll(
                strategyLabel,
                strategyComboBox,
                strategyDescription,
                contractLabel,
                contractScrollPane,
                createRiskSettingsGrid(
                        perTradeRiskEnabledCheckBox,
                        riskPerTradeField,
                        dailyRiskEnabledCheckBox,
                        maxDailyLossField
                ),
                createSelectionGrid(
                        conditionComboBox,
                        conditionMessage,
                        targetModeComboBox,
                        targetMessage,
                        rewardRiskRatioField,
                        profitTargetTicksField
                ),
                actions
        );
        TitledPane setupPane = new TitledPane("Backtest Configuration", setupContent);
        setupPane.setCollapsible(true);
        setupPane.setExpanded(true);

        runButton.setOnAction(event -> runBacktest(
                contractSelections,
                strategyComboBox.getValue(),
                conditionComboBox.getValue(),
                targetModeComboBox.getValue(),
                perTradeRiskEnabledCheckBox,
                riskPerTradeField,
                dailyRiskEnabledCheckBox,
                maxDailyLossField,
                rewardRiskRatioField,
                profitTargetTicksField,
                resultsTabs,
                setupPane
        ));

        VBox root = new VBox(12);
        root.setPadding(new Insets(4));
        root.setAlignment(Pos.TOP_LEFT);
        root.getChildren().addAll(
                heading,
                setupPane,
                progressSection,
                resultsTabs,
                errorLabel
        );
        VBox.setVgrow(resultsTabs, Priority.ALWAYS);

        loadStrategies(strategyComboBox, strategyDescription, conditionComboBox, conditionMessage,
                targetModeComboBox, targetMessage, rewardRiskRatioField, profitTargetTicksField);
        loadAvailableContracts(contractList, contractSelections);
        return root;
    }

    private GridPane createRiskSettingsGrid(
            CheckBox perTradeRiskEnabledCheckBox,
            TextField riskPerTradeField,
            CheckBox dailyRiskEnabledCheckBox,
            TextField maxDailyLossField
    ) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.add(perTradeRiskEnabledCheckBox, 0, 0);
        grid.add(new Label("Risk per trade"), 1, 0);
        grid.add(riskPerTradeField, 2, 0);
        grid.add(dailyRiskEnabledCheckBox, 0, 1);
        grid.add(new Label("Max daily loss"), 1, 1);
        grid.add(maxDailyLossField, 2, 1);
        return grid;
    }

    private GridPane createSelectionGrid(
            ComboBox<ConditionSelection> conditionComboBox,
            Label conditionMessage,
            ComboBox<String> targetModeComboBox,
            Label targetMessage,
            TextField rewardRiskRatioField,
            TextField profitTargetTicksField
    ) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.add(new Label("Market event"), 0, 0);
        grid.add(conditionComboBox, 1, 0);
        grid.add(conditionMessage, 2, 0);
        grid.add(new Label("Target mode"), 0, 1);
        grid.add(targetModeComboBox, 1, 1);
        grid.add(targetMessage, 2, 1);
        grid.add(new Label("Reward/risk ratio"), 0, 2);
        grid.add(rewardRiskRatioField, 1, 2);
        grid.add(new Label("Profit target ticks"), 0, 3);
        grid.add(profitTargetTicksField, 1, 3);
        return grid;
    }

    private VBox createProgressSection(
            BacktestViewModel viewModel,
            ProgressBar progressBar,
            Label statusLabel
    ) {
        Label progressLabel = new Label("Backtest progress");
        progressLabel.setStyle("-fx-font-weight: bold;");

        Label progressDetails = new Label();
        progressDetails.setMinWidth(150);
        progressDetails.setPrefWidth(150);
        progressDetails.setMaxWidth(150);
        progressDetails.setAlignment(Pos.CENTER_RIGHT);
        progressDetails.textProperty().bind(Bindings.createStringBinding(
                () -> String.format(
                        "%.0f%%  %d/%d ticks",
                        viewModel.getProgress() * 100.0,
                        viewModel.getProcessedUnits(),
                        viewModel.getTotalUnits()
                ),
                viewModel.progressProperty(),
                viewModel.processedUnitsProperty(),
                viewModel.totalUnitsProperty()
        ));

        progressBar.setMinWidth(260);
        HBox.setHgrow(progressBar, Priority.ALWAYS);

        HBox progressRow = new HBox(10, progressBar, progressDetails);
        progressRow.setAlignment(Pos.CENTER_LEFT);

        VBox section = new VBox(6, progressLabel, progressRow, statusLabel);
        section.setPadding(new Insets(8));
        section.setStyle(
                "-fx-background-color: #ffffff;"
                        + "-fx-border-color: #d7dde3;"
                        + "-fx-border-radius: 6;"
                        + "-fx-background-radius: 6;"
        );
        return section;
    }

    private void loadStrategies(
            ComboBox<StrategySelection> strategyComboBox,
            Label strategyDescription,
            ComboBox<ConditionSelection> conditionComboBox,
            Label conditionMessage,
            ComboBox<String> targetModeComboBox,
            Label targetMessage,
            TextField rewardRiskRatioField,
            TextField profitTargetTicksField
    ) {
        /*
         * Intent: Populate strategy choices and apply defaults for the first strategy.
         * Precondition: Selection controls must belong to the active backtest view.
         * Returns: Nothing.
         * Postcondition: Strategy-dependent condition and target controls are initialized.
         */
        strategyComboBox.getItems().clear();
        try {
            for (Class<? extends TradingStrategy> strategyClass : controller.getAvailableStrategies()) {
                strategyComboBox.getItems().add(new StrategySelection(
                        strategyClass,
                        controller.getStrategyDisplayName(strategyClass),
                        controller.getStrategyDescription(strategyClass),
                        controller.getStrategyConfigurationProfile(strategyClass)
                ));
            }
            if (!strategyComboBox.getItems().isEmpty()) {
                strategyComboBox.getSelectionModel().selectFirst();
                applyStrategySelection(
                        strategyComboBox.getValue(),
                        strategyDescription,
                        conditionComboBox,
                        conditionMessage,
                        targetModeComboBox,
                        targetMessage,
                        rewardRiskRatioField,
                        profitTargetTicksField
                );
            }
        } catch (RuntimeException exception) {
            controller.getViewModel().markFailed("Could not load strategies.", exception);
        }
    }

    private void applyStrategySelection(
            StrategySelection selection,
            Label strategyDescription,
            ComboBox<ConditionSelection> conditionComboBox,
            Label conditionMessage,
            ComboBox<String> targetModeComboBox,
            Label targetMessage,
            TextField rewardRiskRatioField,
            TextField profitTargetTicksField
    ) {
        /*
         * Intent: Apply a selected strategy's configurable condition and target profile to the UI.
         * Precondition: selection may be null during ComboBox clearing.
         * Returns: Nothing.
         * Postcondition: Condition and target controls reflect the strategy profile.
         */
        conditionComboBox.getItems().clear();
        targetModeComboBox.getItems().clear();
        if (selection == null) {
            strategyDescription.setText("");
            return;
        }

        StrategyConfigurationProfile profile = selection.profile();
        strategyDescription.setText(selection.description());

        for (Class<? extends MarketEvent> eventClass : profile.getAllowedEvents()) {
            conditionComboBox.getItems().add(new ConditionSelection(
                    eventClass,
                    controller.getConditionDisplayName(eventClass)
            ));
        }
        selectEvent(conditionComboBox, profile.getDefaultEvent());
        conditionComboBox.setDisable(!profile.isEventSelectionAllowed());
        conditionMessage.setText(profile.isEventSelectionAllowed()
                ? "Choose the condition this strategy should trade."
                : "Using default condition: " + conditionComboBox.getValue());

        targetModeComboBox.getItems().addAll(profile.getAllowedTargets());
        targetModeComboBox.getSelectionModel().select(profile.getDefaultTarget());
        targetModeComboBox.setDisable(!profile.isTargetSelectionAllowed());
        targetMessage.setText(profile.isTargetSelectionAllowed()
                ? "Choose the target mode for this strategy."
                : "Using default target mode: " + profile.getDefaultTarget());
        applyTargetSelection(selection, profile.getDefaultTarget(), rewardRiskRatioField, profitTargetTicksField);
    }

    private void selectEvent(
            ComboBox<ConditionSelection> conditionComboBox,
            Class<? extends MarketEvent> defaultEvent
    ) {
        for (ConditionSelection selection : conditionComboBox.getItems()) {
            if (selection.eventClass().equals(defaultEvent)) {
                conditionComboBox.getSelectionModel().select(selection);
                return;
            }
        }
        if (!conditionComboBox.getItems().isEmpty()) {
            conditionComboBox.getSelectionModel().selectFirst();
        }
    }

    private void applyTargetSelection(
            StrategySelection strategySelection,
            String targetMode,
            TextField rewardRiskRatioField,
            TextField profitTargetTicksField
    ) {
        if (strategySelection == null || targetMode == null) {
            rewardRiskRatioField.clear();
            profitTargetTicksField.clear();
            return;
        }

        TargetSettings defaults = strategySelection.profile().getDefaultTargetSettings(targetMode);
        boolean fixedRiskReward = TargetSettings.FIXED_RISK_REWARD.equals(targetMode);
        rewardRiskRatioField.setDisable(!fixedRiskReward);
        profitTargetTicksField.setDisable(fixedRiskReward);
        rewardRiskRatioField.setText(defaults.getRewardRiskRatio() == null
                ? ""
                : Double.toString(defaults.getRewardRiskRatio()));
        profitTargetTicksField.setText(defaults.getProfitTargetTicks() == null
                ? ""
                : Integer.toString(defaults.getProfitTargetTicks()));
    }

    private void loadAvailableContracts(VBox contractList, List<ContractSelection> contractSelections) {
        /*
         * Intent: Refresh selectable contract windows for the backtest run.
         * Precondition: contractList and contractSelections must be the active UI state containers.
         * Returns: Nothing.
         * Postcondition: The checkbox list mirrors currently available imported contract windows.
         */
        contractList.getChildren().clear();
        contractSelections.clear();

        try {
            List<AvailableContractData> contracts = controller.getAvailableContracts();
            if (contracts.isEmpty()) {
                contractList.getChildren().add(new Label("No imported contract windows are available."));
                controller.getViewModel().setStatusMessage("No imported contract windows are available.");
                return;
            }

            for (AvailableContractData contract : contracts) {
                ContractTradeWindow window = new ContractTradeWindow(
                        contract.getContractSymbol(),
                        contract.getStartDate(),
                        contract.getEndDate()
                );
                CheckBox checkBox = new CheckBox(contract.toString());
                checkBox.setSelected(true);
                contractSelections.add(new ContractSelection(checkBox, window));
                contractList.getChildren().add(checkBox);
            }
            controller.getViewModel().setStatusMessage("Loaded " + contracts.size() + " contract window(s).");
        } catch (RuntimeException exception) {
            controller.getViewModel().markFailed("Could not load available contracts.", exception);
        }
    }

    private TabPane createResultsTabs(BacktestViewModel viewModel) {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getTabs().add(createSummaryTab(null));
        tabPane.getTabs().add(createTradesTab(null));
        tabPane.getTabs().add(createPlaceholderTab("Tables", "Tabular backtest results will be added here."));
        tabPane.getTabs().add(createPlaceholderTab("Charts", "Equity curves and performance charts will be added here."));
        tabPane.getTabs().add(createExportTab(viewModel));
        return tabPane;
    }

    private Tab createSummaryTab(BacktestResult result) {
        VBox content = new VBox(14);
        content.setPadding(new Insets(10));
        if (result == null) {
            content.getChildren().add(new Label("Run a backtest to populate summary cards."));
        } else {
            content.getChildren().add(createRunSummary(result));
            content.getChildren().add(createInstrumentCardsSection(result.getInstrumentResults()));
            List<ContractBacktestResult> contractResults = allContractResults(result);
            if (contractResults.size() > 1) {
                content.getChildren().add(createContractCardsSection("Contract Results", contractResults));
            } else if (!contractResults.isEmpty()) {
                content.getChildren().add(createContractCardsSection("Contract Result", contractResults));
            }
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        Tab tab = new Tab("Summary");
        tab.setContent(scrollPane);
        return tab;
    }

    private Tab createTradesTab(BacktestResult result) {
        /*
         * Intent: Create a tabular view of every simulated trade from a backtest result.
         * Precondition: result may be null before the first backtest run.
         * Returns: Tab containing either placeholder text or a populated trade table.
         * Postcondition: Backtest result data is read but not modified.
         */
        Tab tab = new Tab("Trades");
        if (result == null) {
            VBox placeholder = new VBox(8);
            placeholder.setPadding(new Insets(12));
            placeholder.getChildren().add(new Label("Run a backtest to populate simulated trades."));
            tab.setContent(placeholder);
            return tab;
        }

        ObservableList<TradeResult> trades = FXCollections.observableArrayList(allTrades(result));
        if (trades.isEmpty()) {
            VBox emptyState = new VBox(8);
            emptyState.setPadding(new Insets(12));
            emptyState.getChildren().add(new Label("No trades were simulated for this backtest."));
            tab.setContent(emptyState);
            return tab;
        }

        TableView<TradeResult> table = new TableView<>(trades);
        table.setPrefHeight(360);
        table.getColumns().add(textColumn("Instrument", trade -> trade.getInstrumentSymbol()));
        table.getColumns().add(textColumn("Contract", trade -> trade.getContractSymbol()));
        table.getColumns().add(textColumn("Side", trade -> trade.getSide().name()));
        table.getColumns().add(textColumn("Entry Time", trade -> trade.getEntryTime().toString()));
        table.getColumns().add(textColumn("Exit Time", trade -> trade.getExitTime().toString()));
        table.getColumns().add(textColumn("Max Pos", trade -> Integer.toString(trade.getQuantity())));
        table.getColumns().add(textColumn("Entry Price", trade -> formatPrice(trade, trade.getEntryPriceTicks())));
        table.getColumns().add(textColumn("Exit Price", trade -> formatPrice(trade, trade.getExitPriceTicks())));
        table.getColumns().add(textColumn("Gross Ticks", trade -> Long.toString(trade.getGrossTicks())));
        table.getColumns().add(textColumn("P/L", trade -> String.format("$%.2f", trade.getGrossDollars())));
        table.getColumns().add(textColumn("MFE", trade -> String.format("$%.2f", trade.getMaxFavorableExcursionDollars())));
        table.getColumns().add(textColumn("MAE", trade -> String.format("$%.2f", trade.getMaxAdverseExcursionDollars())));
        table.getColumns().add(textColumn("Exit Reason", TradeResult::getExitReason));

        VBox content = new VBox(8, table);
        content.setPadding(new Insets(10));
        tab.setContent(content);
        return tab;
    }

    private TableColumn<TradeResult, String> textColumn(
            String title,
            Function<TradeResult, String> valueProvider
    ) {
        /*
         * Intent: Create a string-backed table column for the Trades tab.
         * Precondition: title and valueProvider must describe one TradeResult field.
         * Returns: Configured TableColumn.
         * Postcondition: Column values are derived on demand from immutable trade results.
         */
        TableColumn<TradeResult, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(valueProvider.apply(cell.getValue())));
        return column;
    }

    private String formatPrice(TradeResult trade, long priceTicks) {
        /*
         * Intent: Display normalized tick prices as human-readable futures prices.
         * Precondition: trade must identify a supported instrument symbol and priceTicks must be positive.
         * Returns: Actual price derived from the instrument's stored-price conversion rules.
         * Postcondition: Trade result state remains unchanged.
         */
        FuturesInstrumentSpec spec = instrumentSpecProvider.getBySymbol(trade.getInstrumentSymbol());
        return String.format("%." + priceDecimalPlaces(spec) + "f", spec.displayPrice(priceTicks));
    }

    private int priceDecimalPlaces(FuturesInstrumentSpec spec) {
        /*
         * Intent: Match displayed precision to an instrument's tick size.
         * Precondition: spec must be a supported futures instrument.
         * Returns: Decimal places needed to display one tick, such as 0 for YM and 2 for ES.
         * Postcondition: Instrument specification state remains unchanged.
         */
        return Math.max(0, BigDecimal.valueOf(spec.getTickSize()).stripTrailingZeros().scale());
    }

    private VBox createRunSummary(BacktestResult result) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(12));
        card.setStyle(
                "-fx-background-color: #ffffff;"
                        + "-fx-border-color: #cfd6dd;"
                        + "-fx-border-radius: 6;"
                        + "-fx-background-radius: 6;"
        );
        Label title = new Label("Run Summary");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        card.getChildren().addAll(
                title,
                new Label("Strategy: " + result.getStrategyName()),
                new Label("Ticks processed: " + result.getTicksProcessed()),
                new Label("Order signals: " + result.getOrderSignalsGenerated())
        );
        return card;
    }

    private VBox createInstrumentCardsSection(List<InstrumentBacktestResult> results) {
        Label heading = new Label("Instrument Results");
        heading.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        FlowPane cards = new FlowPane();
        cards.setHgap(12);
        cards.setVgap(12);
        for (InstrumentBacktestResult result : results) {
            cards.getChildren().add(createMetricsCard(
                    result.getInstrumentSymbol(),
                    result.getTicksProcessed(),
                    result.getOrderSignalsGenerated(),
                    result.getPerformanceMetrics()
            ));
        }

        VBox section = new VBox(8, heading, cards);
        section.setAlignment(Pos.TOP_LEFT);
        return section;
    }

    private VBox createContractCardsSection(String title, List<ContractBacktestResult> results) {
        Label heading = new Label(title);
        heading.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        FlowPane cards = new FlowPane();
        cards.setHgap(12);
        cards.setVgap(12);
        for (ContractBacktestResult result : results) {
            cards.getChildren().add(createMetricsCard(
                    result.getContractSymbol(),
                    result.getTicksProcessed(),
                    result.getOrderSignalsGenerated(),
                    result.getPerformanceMetrics()
            ));
        }

        VBox section = new VBox(8, heading, cards);
        section.setAlignment(Pos.TOP_LEFT);
        return section;
    }

    private VBox createMetricsCard(
            String titleText,
            long ticksProcessed,
            long orderSignalsGenerated,
            PerformanceMetrics metrics
    ) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(12));
        card.setPrefWidth(260);
        card.setStyle(
                "-fx-background-color: #ffffff;"
                        + "-fx-border-color: #cfd6dd;"
                        + "-fx-border-radius: 6;"
                        + "-fx-background-radius: 6;"
        );

        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        card.getChildren().addAll(
                title,
                new Label("Ticks: " + ticksProcessed),
                new Label("Signals: " + orderSignalsGenerated),
                new Label("Trades: " + metrics.getTotalTrades()),
                new Label("Wins / Losses: " + metrics.getWinningTrades() + " / " + metrics.getLosingTrades()),
                new Label(String.format("Win Rate: %.2f%%", metrics.getWinRate())),
                new Label(String.format("Net P/L: $%.2f", metrics.getNetProfitLoss())),
                new Label(String.format("Gross Profit / Loss: $%.2f / $%.2f", metrics.getGrossProfit(), metrics.getGrossLoss())),
                new Label(String.format("Average Trade: $%.2f", metrics.getAverageTrade())),
                new Label(String.format("Profit Factor: %.2f", metrics.getProfitFactor())),
                new Label(String.format("Max Drawdown: $%.2f", metrics.getMaximumDrawdown())),
                new Label(String.format("Max Runup: $%.2f", metrics.getMaximumRunup())),
                new Label(String.format("MFE Avg / Max: $%.2f / $%.2f",
                        metrics.getAverageFavorableExcursion(),
                        metrics.getMaxFavorableExcursion())),
                new Label(String.format("MAE Avg / Max: $%.2f / $%.2f",
                        metrics.getAverageAdverseExcursion(),
                        metrics.getMaxAdverseExcursion()))
        );
        return card;
    }

    private Tab createPlaceholderTab(String title, String message) {
        VBox content = new VBox(8);
        content.setPadding(new Insets(12));
        content.getChildren().add(new Label(message));
        Tab tab = new Tab(title);
        tab.setContent(content);
        return tab;
    }

    private Tab createExportTab(BacktestViewModel viewModel) {
        TextArea exportText = new TextArea();
        exportText.setEditable(false);
        exportText.setWrapText(false);
        exportText.textProperty().bind(viewModel.resultSummaryProperty());
        Tab tab = new Tab("Export Text");
        tab.setContent(exportText);
        return tab;
    }

    private void renderReport(TabPane resultsTabs, BacktestResult result) {
        /*
         * Intent: Replace result tabs with views from the completed backtest.
         * Precondition: resultsTabs must contain summary at index 0 and trades at index 1.
         * Returns: Nothing.
         * Postcondition: Summary and trades tabs display the latest backtest result.
         */
        resultsTabs.getTabs().set(0, createSummaryTab(result));
        resultsTabs.getTabs().set(1, createTradesTab(result));
        resultsTabs.getSelectionModel().select(0);
    }

    private void runBacktest(
            List<ContractSelection> contractSelections,
            StrategySelection strategySelection,
            ConditionSelection conditionSelection,
            String targetMode,
            CheckBox perTradeRiskEnabledCheckBox,
            TextField riskPerTradeField,
            CheckBox dailyRiskEnabledCheckBox,
            TextField maxDailyLossField,
            TextField rewardRiskRatioField,
            TextField profitTargetTicksField,
            TabPane resultsTabs,
            TitledPane setupPane
    ) {
        /*
         * Intent: Validate backtest inputs, create the request, and start a background backtest task.
         * Precondition: User must select contracts, strategy, condition, target mode, and valid risk values.
         * Returns: Nothing.
         * Postcondition: A daemon backtest thread is started, or the view model reports validation/failure.
         */
        BacktestViewModel viewModel = controller.getViewModel();
        List<ContractTradeWindow> selectedWindows = selectedWindows(contractSelections);

        if (selectedWindows.isEmpty()) {
            viewModel.markFailed("Could not run backtest.", new RuntimeException("Select at least one contract window."));
            return;
        }
        if (strategySelection == null) {
            viewModel.markFailed("Could not run backtest.", new RuntimeException("Select a strategy."));
            return;
        }
        if (conditionSelection == null) {
            viewModel.markFailed("Could not run backtest.", new RuntimeException("Select a market event."));
            return;
        }
        if (targetMode == null || targetMode.isBlank()) {
            viewModel.markFailed("Could not run backtest.", new RuntimeException("Select a target mode."));
            return;
        }

        try {
            RiskSettings riskSettings = new RiskSettings(
                    perTradeRiskEnabledCheckBox.isSelected(),
                    perTradeRiskEnabledCheckBox.isSelected()
                            ? parseDouble(riskPerTradeField, "Risk per trade")
                            : 0.0,
                    dailyRiskEnabledCheckBox.isSelected(),
                    dailyRiskEnabledCheckBox.isSelected()
                            ? parseDouble(maxDailyLossField, "Max daily loss")
                            : 0.0
            );
            TargetSettings targetSettings = createTargetSettings(targetMode, rewardRiskRatioField, profitTargetTicksField);
            BacktestRequest request = controller.createBacktestRequest(
                    strategySelection.strategyClass(),
                    selectedWindows,
                    conditionSelection.eventClass(),
                    riskSettings,
                    targetSettings
            );

            Task<BacktestResult> task = controller.runBacktestTask(request);
            task.addEventHandler(
                    WorkerStateEvent.WORKER_STATE_SUCCEEDED,
                    event -> {
                        viewModel.setResultSummary(task.getValue().toString());
                        renderReport(resultsTabs, task.getValue());
                        setupPane.setExpanded(false);
                    }
            );
            Thread thread = new Thread(task, "forge-gui-backtest");
            thread.setDaemon(true);
            thread.start();
        } catch (RuntimeException exception) {
            viewModel.markFailed("Could not run backtest.", exception);
        }
    }

    private TargetSettings createTargetSettings(
            String targetMode,
            TextField rewardRiskRatioField,
            TextField profitTargetTicksField
    ) {
        /*
         * Intent: Convert the selected target mode and text fields into target settings.
         * Precondition: targetMode must be one of the supported target setting names.
         * Returns: TargetSettings for the backtest request.
         * Postcondition: Text-field values are parsed but not modified.
         */
        if (TargetSettings.FIXED_RISK_REWARD.equals(targetMode)) {
            return TargetSettings.fixedRiskReward(parseDouble(rewardRiskRatioField, "Reward/risk ratio"));
        }
        return TargetSettings.fixedTarget(parseInt(profitTargetTicksField, "Profit target ticks"));
    }

    private double parseDouble(TextField field, String name) {
        try {
            return Double.parseDouble(field.getText().trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be a number", exception);
        }
    }

    private int parseInt(TextField field, String name) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be a whole number", exception);
        }
    }

    private List<ContractTradeWindow> selectedWindows(List<ContractSelection> contractSelections) {
        List<ContractTradeWindow> selectedWindows = new ArrayList<>();
        for (ContractSelection selection : contractSelections) {
            if (selection.checkBox().isSelected()) {
                selectedWindows.add(selection.window());
            }
        }
        return selectedWindows;
    }

    private List<ContractBacktestResult> allContractResults(BacktestResult result) {
        List<ContractBacktestResult> contractResults = new ArrayList<>();
        for (InstrumentBacktestResult instrumentResult : result.getInstrumentResults()) {
            contractResults.addAll(instrumentResult.getContractResults());
        }
        return contractResults;
    }

    private List<TradeResult> allTrades(BacktestResult result) {
        /*
         * Intent: Flatten instrument/contract results into one trade list for the Trades tab.
         * Precondition: result must be a completed backtest result.
         * Returns: List of all simulated trades in report traversal order.
         * Postcondition: Reporting result objects are not modified.
         */
        List<TradeResult> trades = new ArrayList<>();
        for (ContractBacktestResult contractResult : allContractResults(result)) {
            trades.addAll(contractResult.getTrades());
        }
        return trades;
    }

    private record ContractSelection(CheckBox checkBox, ContractTradeWindow window) {
    }

    private record StrategySelection(
            Class<? extends TradingStrategy> strategyClass,
            String displayName,
            String description,
            StrategyConfigurationProfile profile
    ) {
        @Override
        public String toString() {
            return displayName;
        }
    }

    private record ConditionSelection(Class<? extends MarketEvent> eventClass, String displayName) {
        @Override
        public String toString() {
            return displayName;
        }
    }
}
