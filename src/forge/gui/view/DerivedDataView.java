package forge.gui.view;

import forge.data.build.DatabaseBuildPlan;
import forge.data.build.DatabaseBuildResult;
import forge.data.build.DerivedDataBuildOption;
import forge.data.catalog.InstrumentDataCatalog.AvailableContractData;
import forge.data.market.ContractTradeWindow;
import forge.gui.controller.DerivedDataController;
import forge.gui.viewmodel.DerivedDataViewModel;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class DerivedDataView {
    private final DerivedDataController controller;

    public DerivedDataView(DerivedDataController controller) {
        if (controller == null) {
            throw new IllegalArgumentException("controller is required");
        }
        this.controller = controller;
    }

    public Parent createView() {
        DerivedDataViewModel viewModel = controller.getViewModel();

        Label heading = new Label("Derived Data");
        heading.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Label contractLabel = new Label("Available contract windows");
        VBox contractList = new VBox(6);
        contractList.setPadding(new Insets(8));
        contractList.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d7dde3;");

        ScrollPane contractScrollPane = new ScrollPane(contractList);
        contractScrollPane.setFitToWidth(true);
        contractScrollPane.setPrefViewportHeight(220);

        List<ContractSelection> contractSelections = new ArrayList<>();
        Button refreshButton = new Button("Refresh Contracts");
        refreshButton.disableProperty().bind(viewModel.runningProperty());
        refreshButton.setOnAction(event -> loadAvailableContracts(contractList, contractSelections));

        CheckBox sessionRangesCheckBox = new CheckBox("Session ranges");
        sessionRangesCheckBox.setSelected(viewModel.shouldBuildSessionRanges());
        CheckBox firstHourBreachCheckBox = new CheckBox("First-hour breach events");
        firstHourBreachCheckBox.setSelected(viewModel.shouldBuildFirstHourBreachEvents());
        CheckBox rebuildExistingCheckBox = new CheckBox("Rebuild existing derived data");
        rebuildExistingCheckBox.setSelected(viewModel.isRebuildExisting());

        Button buildButton = new Button("Build Derived Data");
        buildButton.disableProperty().bind(viewModel.runningProperty());
        buildButton.setOnAction(event -> runBuild(
                contractSelections,
                sessionRangesCheckBox.isSelected(),
                firstHourBreachCheckBox.isSelected(),
                rebuildExistingCheckBox.isSelected()
        ));

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.progressProperty().bind(viewModel.progressProperty());
        progressBar.setMaxWidth(Double.MAX_VALUE);

        Label statusLabel = new Label();
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());

        TextArea resultText = new TextArea();
        resultText.setEditable(false);
        resultText.setWrapText(true);
        resultText.setPrefRowCount(7);
        resultText.textProperty().bind(viewModel.resultSummaryProperty());

        Label errorLabel = new Label();
        errorLabel.textProperty().bind(viewModel.errorMessageProperty());
        errorLabel.setStyle("-fx-text-fill: #b00020;");

        HBox actions = new HBox(8, refreshButton, buildButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox options = new VBox(6, sessionRangesCheckBox, firstHourBreachCheckBox, rebuildExistingCheckBox);
        options.setPadding(new Insets(4, 0, 4, 0));

        VBox root = new VBox(12);
        root.setPadding(new Insets(4));
        root.setAlignment(Pos.TOP_LEFT);
        root.getChildren().addAll(
                heading,
                contractLabel,
                contractScrollPane,
                options,
                actions,
                progressBar,
                statusLabel,
                resultText,
                errorLabel
        );

        loadAvailableContracts(contractList, contractSelections);
        return root;
    }

    private void loadAvailableContracts(VBox contractList, List<ContractSelection> contractSelections) {
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
                ContractSelection selection = new ContractSelection(checkBox, window);
                contractSelections.add(selection);
                contractList.getChildren().add(checkBox);
            }
            controller.getViewModel().setStatusMessage("Loaded " + contracts.size() + " contract window(s).");
        } catch (RuntimeException exception) {
            controller.getViewModel().markFailed("Could not load available contracts.", exception);
        }
    }

    private void runBuild(
            List<ContractSelection> contractSelections,
            boolean buildSessionRanges,
            boolean buildFirstHourBreachEvents,
            boolean rebuildExisting
    ) {
        DerivedDataViewModel viewModel = controller.getViewModel();
        List<ContractTradeWindow> selectedWindows = selectedWindows(contractSelections);
        Set<DerivedDataBuildOption> selectedOptions = selectedOptions(buildSessionRanges, buildFirstHourBreachEvents);

        if (selectedWindows.isEmpty()) {
            viewModel.markFailed("Could not build derived data.", new RuntimeException("Select at least one contract window."));
            return;
        }
        if (selectedOptions.isEmpty()) {
            viewModel.markFailed("Could not build derived data.", new RuntimeException("Select at least one derived-data option."));
            return;
        }

        try {
            DatabaseBuildPlan plan = controller.planBuild(selectedWindows, selectedOptions, rebuildExisting);
            if (!plan.hasWorkToRun()) {
                viewModel.markSucceeded(
                        "Derived data is already current.",
                        "No derived-data rebuild was needed for the selected contract window(s)."
                );
                return;
            }

            Task<DatabaseBuildResult> task = controller.runBuildTask(selectedWindows, selectedOptions, rebuildExisting);
            Thread thread = new Thread(task, "forge-gui-derived-data-build");
            thread.setDaemon(true);
            thread.start();
        } catch (RuntimeException exception) {
            viewModel.markFailed("Could not build derived data.", exception);
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

    private Set<DerivedDataBuildOption> selectedOptions(
            boolean buildSessionRanges,
            boolean buildFirstHourBreachEvents
    ) {
        Set<DerivedDataBuildOption> selectedOptions = EnumSet.noneOf(DerivedDataBuildOption.class);
        if (buildSessionRanges) {
            selectedOptions.add(DerivedDataBuildOption.SESSION_RANGES);
        }
        if (buildFirstHourBreachEvents) {
            selectedOptions.add(DerivedDataBuildOption.FIRST_HOUR_BREACH_EVENTS);
        }
        return selectedOptions;
    }

    private record ContractSelection(CheckBox checkBox, ContractTradeWindow window) {
    }
}
