package forge.gui.view;

import forge.data.catalog.InstrumentDataCatalog.AvailableContractData;
import forge.data.market.ContractTradeWindow;
import forge.reporting.eventstatistics.EventStatisticsReport;
import forge.engine.eventstatistics.EventStatisticsResult;
import forge.gui.controller.EventStatisticsController;
import forge.gui.viewmodel.EventStatisticsViewModel;
import forge.study.MarketStudy;
import javafx.beans.binding.Bindings;
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
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class EventStatisticsView {
    private final EventStatisticsController controller;

    public EventStatisticsView(EventStatisticsController controller) {
        if (controller == null) {
            throw new IllegalArgumentException("controller is required");
        }
        this.controller = controller;
    }

    public Parent createView() {
        /*
         * Intent: Build the event-statistics screen and wire study/contract selections.
         * Precondition: Controller must be initialized and derived event data may be available.
         * Returns: JavaFX parent node for the statistics workflow.
         * Postcondition: View controls are bound to the statistics view model.
         */
        EventStatisticsViewModel viewModel = controller.getViewModel();

        Label heading = new Label("Event Statistics");
        heading.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Label studyLabel = new Label("Study");
        ComboBox<StudySelection> studyComboBox = new ComboBox<>();
        studyComboBox.setMaxWidth(Double.MAX_VALUE);

        Label studyDescription = new Label();
        studyDescription.setWrapText(true);
        studyComboBox.setOnAction(event -> {
            StudySelection selection = studyComboBox.getValue();
            studyDescription.setText(selection == null ? "" : selection.study().getDescription());
        });

        Label contractLabel = new Label("Contract windows");
        VBox contractList = new VBox(6);
        contractList.setPadding(new Insets(8));
        contractList.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d7dde3;");

        ScrollPane contractScrollPane = new ScrollPane(contractList);
        contractScrollPane.setFitToWidth(true);
        contractScrollPane.setPrefViewportHeight(220);
        contractScrollPane.setMinHeight(140);

        List<ContractSelection> contractSelections = new ArrayList<>();
        Button refreshButton = new Button("Refresh Contracts");
        refreshButton.disableProperty().bind(viewModel.runningProperty());
        refreshButton.setOnAction(event -> loadAvailableContracts(contractList, contractSelections));

        Button runButton = new Button("Run Statistics");
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
                studyLabel,
                studyComboBox,
                studyDescription,
                contractLabel,
                contractScrollPane,
                actions
        );
        TitledPane setupPane = new TitledPane("Event Statistics Configuration", setupContent);
        setupPane.setCollapsible(true);
        setupPane.setExpanded(true);

        runButton.setOnAction(event -> runStatistics(
                contractSelections,
                studyComboBox.getValue(),
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

        loadStudies(studyComboBox, studyDescription);
        loadAvailableContracts(contractList, contractSelections);
        return root;
    }

    private VBox createProgressSection(
            EventStatisticsViewModel viewModel,
            ProgressBar progressBar,
            Label statusLabel
    ) {
        Label progressLabel = new Label("Event statistics progress");
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

    private void loadStudies(ComboBox<StudySelection> studyComboBox, Label studyDescription) {
        /*
         * Intent: Populate the study selector from supported study definitions.
         * Precondition: studyComboBox and studyDescription must belong to the active view.
         * Returns: Nothing.
         * Postcondition: The first available study is selected and described when available.
         */
        studyComboBox.getItems().clear();
        try {
            for (String studyName : controller.getSupportedStudyNames()) {
                studyComboBox.getItems().add(new StudySelection(controller.getStudy(studyName)));
            }
            if (!studyComboBox.getItems().isEmpty()) {
                studyComboBox.getSelectionModel().selectFirst();
                studyDescription.setText(studyComboBox.getValue().study().getDescription());
            }
        } catch (RuntimeException exception) {
            controller.getViewModel().markFailed("Could not load studies.", exception);
        }
    }

    private void loadAvailableContracts(VBox contractList, List<ContractSelection> contractSelections) {
        /*
         * Intent: Refresh selectable contract windows for the statistics run.
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

    private TabPane createResultsTabs(EventStatisticsViewModel viewModel) {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getTabs().add(createSummaryTab(null));
        tabPane.getTabs().add(createPlaceholderTab("Tables", "Tabular event statistics will be added here."));
        tabPane.getTabs().add(createPlaceholderTab("Charts", "Charts for event rates and directional splits will be added here."));
        tabPane.getTabs().add(createExportTab(viewModel));
        return tabPane;
    }

    private Tab createSummaryTab(EventStatisticsReport report) {
        VBox content = new VBox(14);
        content.setPadding(new Insets(10));
        if (report == null) {
            content.getChildren().add(new Label("Run event statistics to populate summary cards."));
        } else {
            content.getChildren().add(createCardsSection("Instrument Results", report.getInstrumentResults()));
            if (report.getContractResults().size() > 1) {
                content.getChildren().add(createCardsSection("Contract Results", report.getContractResults()));
            } else if (!report.getContractResults().isEmpty()) {
                content.getChildren().add(createCardsSection("Contract Result", report.getContractResults()));
            }
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        Tab tab = new Tab("Summary");
        tab.setContent(scrollPane);
        return tab;
    }

    private VBox createCardsSection(String title, List<EventStatisticsResult> results) {
        Label heading = new Label(title);
        heading.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        FlowPane cards = new FlowPane();
        cards.setHgap(12);
        cards.setVgap(12);
        for (EventStatisticsResult result : results) {
            cards.getChildren().add(createResultCard(result));
        }

        VBox section = new VBox(8, heading, cards);
        section.setAlignment(Pos.TOP_LEFT);
        return section;
    }

    private VBox createResultCard(EventStatisticsResult result) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(12));
        card.setPrefWidth(240);
        card.setStyle(
                "-fx-background-color: #ffffff;"
                        + "-fx-border-color: #cfd6dd;"
                        + "-fx-border-radius: 6;"
                        + "-fx-background-radius: 6;"
        );

        Label title = new Label(result.getScopeName());
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        card.getChildren().addAll(
                title,
                new Label("Sessions: " + result.getSessionsAnalyzed()),
                new Label("Total Events: " + result.getTotalEventCount()),
                new Label("Long / Short: " + result.getLongEventCount() + " / " + result.getShortEventCount()),
                new Label("No Event: " + result.getNoEventCount()),
                new Label(String.format("Event Rate: %.2f%%", result.getEventRate() * 100.0))
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

    private Tab createExportTab(EventStatisticsViewModel viewModel) {
        TextArea exportText = new TextArea();
        exportText.setEditable(false);
        exportText.setWrapText(false);
        exportText.textProperty().bind(viewModel.resultSummaryProperty());
        Tab tab = new Tab("Export Text");
        tab.setContent(exportText);
        return tab;
    }

    private void renderReport(TabPane resultsTabs, EventStatisticsReport report) {
        /*
         * Intent: Replace the summary tab with cards from the completed report.
         * Precondition: resultsTabs must contain the summary tab at index 0.
         * Returns: Nothing.
         * Postcondition: The summary tab displays the latest statistics report.
         */
        resultsTabs.getTabs().set(0, createSummaryTab(report));
        resultsTabs.getSelectionModel().select(0);
    }

    private void runStatistics(
            List<ContractSelection> contractSelections,
            StudySelection studySelection,
            TabPane resultsTabs,
            TitledPane setupPane
    ) {
        /*
         * Intent: Validate statistics selections and start a background statistics task.
         * Precondition: User should have selected at least one contract window and one study.
         * Returns: Nothing.
         * Postcondition: A daemon statistics thread is started, or the view model reports validation/failure.
         */
        EventStatisticsViewModel viewModel = controller.getViewModel();
        List<ContractTradeWindow> selectedWindows = selectedWindows(contractSelections);

        if (selectedWindows.isEmpty()) {
            viewModel.markFailed("Could not run event statistics.", new RuntimeException("Select at least one contract window."));
            return;
        }
        if (studySelection == null) {
            viewModel.markFailed("Could not run event statistics.", new RuntimeException("Select a study."));
            return;
        }

        try {
            Task<EventStatisticsReport> task = controller.runEventStatisticsTask(
                    selectedWindows,
                    studySelection.study().getName()
            );
            task.addEventHandler(
                    WorkerStateEvent.WORKER_STATE_SUCCEEDED,
                    event -> {
                        viewModel.setResultSummary(formatReport(task.getValue()));
                        renderReport(resultsTabs, task.getValue());
                        setupPane.setExpanded(false);
                    }
            );
            Thread thread = new Thread(task, "forge-gui-event-statistics");
            thread.setDaemon(true);
            thread.start();
        } catch (RuntimeException exception) {
            viewModel.markFailed("Could not run event statistics.", exception);
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

    private String formatReport(EventStatisticsReport report) {
        /*
         * Intent: Create a text export for the latest event statistics report.
         * Precondition: report must be complete.
         * Returns: Multi-line text summary for instrument and contract results.
         * Postcondition: Report data is not modified.
         */
        StringBuilder builder = new StringBuilder();
        builder.append("Event: ").append(report.getEventName()).append(System.lineSeparator()).append(System.lineSeparator());
        appendSection(builder, "Instrument Results", report.getInstrumentResults());
        builder.append(System.lineSeparator());
        appendSection(builder, "Contract Results", report.getContractResults());
        return builder.toString();
    }

    private void appendSection(StringBuilder builder, String title, List<EventStatisticsResult> results) {
        builder.append(title).append(System.lineSeparator());
        builder.append("-------------------------").append(System.lineSeparator());
        if (results.isEmpty()) {
            builder.append("No results.").append(System.lineSeparator());
            return;
        }
        for (EventStatisticsResult result : results) {
            builder.append(result.getScopeName())
                    .append(": sessions=")
                    .append(result.getSessionsAnalyzed())
                    .append(", total=")
                    .append(result.getTotalEventCount())
                    .append(", long=")
                    .append(result.getLongEventCount())
                    .append(", short=")
                    .append(result.getShortEventCount())
                    .append(", no event=")
                    .append(result.getNoEventCount())
                    .append(", rate=")
                    .append(String.format("%.2f%%", result.getEventRate() * 100.0))
                    .append(System.lineSeparator());
        }
    }

    private record ContractSelection(CheckBox checkBox, ContractTradeWindow window) {
    }

    private record StudySelection(MarketStudy study) {
        @Override
        public String toString() {
            return study.getDisplayName();
        }
    }
}
