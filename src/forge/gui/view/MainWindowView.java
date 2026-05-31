package forge.gui.view;

import forge.gui.FacadeForgeGui;
import forge.gui.viewmodel.GuiWorkflowType;
import forge.gui.viewmodel.MainWindowViewModel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class MainWindowView {
    private static final String NAVIGATION_BUTTON_BASE_STYLE = String.join(
            "",
            "-fx-padding: 8 10 8 10;",
            "-fx-background-radius: 4;",
            "-fx-border-radius: 4;"
    );
    private static final String NAVIGATION_BUTTON_STYLE = String.join(
            "",
            NAVIGATION_BUTTON_BASE_STYLE,
            "-fx-background-color: transparent;",
            "-fx-text-fill: #263238;",
            "-fx-border-color: transparent;"
    );
    private static final String ACTIVE_NAVIGATION_BUTTON_STYLE = String.join(
            "",
            NAVIGATION_BUTTON_BASE_STYLE,
            "-fx-background-color: #2563eb;",
            "-fx-text-fill: white;",
            "-fx-font-weight: bold;",
            "-fx-border-color: #1d4ed8;"
    );

    public void render(MainWindowViewModel viewModel) {
        if (viewModel == null) {
            throw new IllegalArgumentException("viewModel is required");
        }
        System.out.println(viewModel.getStatusMessage());
    }

    public void render(Stage stage, MainWindowViewModel viewModel) {
        /*
         * Intent: Construct and show the main JavaFX shell for all GUI workflows.
         * Precondition: stage and viewModel must be non-null.
         * Returns: Nothing.
         * Postcondition: Navigation, workflow content, and status bar are visible.
         */
        if (stage == null) {
            throw new IllegalArgumentException("stage is required");
        }
        if (viewModel == null) {
            throw new IllegalArgumentException("viewModel is required");
        }

        Map<GuiWorkflowType, StackPane> workflowContentCache = new EnumMap<>(GuiWorkflowType.class);

        BorderPane root = new BorderPane();
        root.setLeft(createNavigation(stage, viewModel, root, workflowContentCache));
        root.setCenter(getWorkflowContent(stage, viewModel.getActiveWorkflow(), workflowContentCache));
        root.setBottom(createStatusBar(viewModel));

        Scene scene = new Scene(root, 1180, 760);
        stage.setTitle(viewModel.getWindowTitle());
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }

    private VBox createNavigation(
            Window owner,
            MainWindowViewModel viewModel,
            BorderPane root,
            Map<GuiWorkflowType, StackPane> workflowContentCache
    ) {
        VBox navigation = new VBox(8);
        navigation.setPadding(new Insets(14));
        navigation.setPrefWidth(230);
        navigation.setStyle("-fx-background-color: #f4f6f8; -fx-border-color: #d7dde3; -fx-border-width: 0 1 0 0;");

        Label title = new Label("FORGE");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        List<Button> navigationButtons = new ArrayList<>();
        navigation.getChildren().add(title);
        addNavigationButton(navigation, navigationButtons, "Import Data", GuiWorkflowType.IMPORT_DATA,
                owner, viewModel, root, workflowContentCache);
        addNavigationButton(navigation, navigationButtons, "Derived Data", GuiWorkflowType.DERIVED_DATA,
                owner, viewModel, root, workflowContentCache);
        addNavigationButton(navigation, navigationButtons, "Event Statistics", GuiWorkflowType.EVENT_STATISTICS,
                owner, viewModel, root, workflowContentCache);
        addNavigationButton(navigation, navigationButtons, "Backtest", GuiWorkflowType.BACKTEST,
                owner, viewModel, root, workflowContentCache);
        addNavigationButton(navigation, navigationButtons, "Benchmark", GuiWorkflowType.BENCHMARK,
                owner, viewModel, root, workflowContentCache);
        addNavigationButton(navigation, navigationButtons, "Database Config", GuiWorkflowType.DATABASE_CONFIG,
                owner, viewModel, root, workflowContentCache);
        updateNavigationButtonStyles(navigationButtons, viewModel.getActiveWorkflow());
        VBox.setVgrow(navigation.getChildren().get(navigation.getChildren().size() - 1), Priority.NEVER);
        return navigation;
    }

    private void addNavigationButton(
            VBox navigation,
            List<Button> navigationButtons,
            String label,
            GuiWorkflowType workflowType,
            Window owner,
            MainWindowViewModel viewModel,
            BorderPane root,
            Map<GuiWorkflowType, StackPane> workflowContentCache
    ) {
        Button button = createNavigationButton(
                label,
                workflowType,
                owner,
                viewModel,
                root,
                navigationButtons,
                workflowContentCache
        );
        navigationButtons.add(button);
        navigation.getChildren().add(button);
    }

    private Button createNavigationButton(
            String label,
            GuiWorkflowType workflowType,
            Window owner,
            MainWindowViewModel viewModel,
            BorderPane root,
            List<Button> navigationButtons,
            Map<GuiWorkflowType, StackPane> workflowContentCache
    ) {
        /*
         * Intent: Create a navigation button that swaps the center workflow content.
         * Precondition: workflowType and root must belong to the active main window.
         * Returns: Configured JavaFX button.
         * Postcondition: Clicking the button updates active workflow and status text.
         */
        Button button = new Button(label);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setUserData(workflowType);
        button.setOnAction(event -> {
            viewModel.setActiveWorkflow(workflowType);
            root.setCenter(getWorkflowContent(owner, workflowType, workflowContentCache));
            viewModel.setStatusMessage(label + " selected.");
            updateNavigationButtonStyles(navigationButtons, workflowType);
        });
        return button;
    }

    private void updateNavigationButtonStyles(List<Button> navigationButtons, GuiWorkflowType activeWorkflowType) {
        for (Button button : navigationButtons) {
            button.setStyle(button.getUserData() == activeWorkflowType
                    ? ACTIVE_NAVIGATION_BUTTON_STYLE
                    : NAVIGATION_BUTTON_STYLE);
        }
    }

    private StackPane getWorkflowContent(
            Window owner,
            GuiWorkflowType workflowType,
            Map<GuiWorkflowType, StackPane> workflowContentCache
    ) {
        return workflowContentCache.computeIfAbsent(workflowType, type -> createWorkflowContent(owner, type));
    }

    private StackPane createWorkflowContent(Window owner, GuiWorkflowType workflowType) {
        /*
         * Intent: Build one workflow view through the GUI facade.
         * Precondition: workflowType must identify a supported GUI workflow.
         * Returns: StackPane containing the selected workflow screen.
         * Postcondition: Workflow-specific controller/view instances are created for cache reuse.
         */
        StackPane content = new StackPane();
        content.setPadding(new Insets(28));
        if (workflowType == GuiWorkflowType.IMPORT_DATA) {
            content.getChildren().add(new ImportDataView(FacadeForgeGui.getTheInstance()
                    .forgeGuiAccess()
                    .createImportDataController()).createView(owner));
        } else if (workflowType == GuiWorkflowType.DERIVED_DATA) {
            content.getChildren().add(new DerivedDataView(FacadeForgeGui.getTheInstance()
                    .forgeGuiAccess()
                    .createDerivedDataController()).createView());
        } else if (workflowType == GuiWorkflowType.EVENT_STATISTICS) {
            content.getChildren().add(new EventStatisticsView(FacadeForgeGui.getTheInstance()
                    .forgeGuiAccess()
                    .createEventStatisticsController()).createView());
        } else if (workflowType == GuiWorkflowType.BACKTEST) {
            content.getChildren().add(new BacktestView(FacadeForgeGui.getTheInstance()
                    .forgeGuiAccess()
                    .createBacktestController()).createView());
        } else if (workflowType == GuiWorkflowType.BENCHMARK) {
            content.getChildren().add(new BenchmarkView(FacadeForgeGui.getTheInstance()
                    .forgeGuiAccess()
                    .createBenchmarkController()).createView(owner));
        } else if (workflowType == GuiWorkflowType.DATABASE_CONFIG) {
            content.getChildren().add(new DatabaseConfigView(FacadeForgeGui.getTheInstance()
                    .forgeGuiAccess()
                    .createDatabaseConfigController()).createView());
        } else {
            content.getChildren().add(createPlaceholder(workflowType));
        }
        return content;
    }

    private VBox createPlaceholder(GuiWorkflowType workflowType) {
        VBox panel = new VBox(10);
        panel.setAlignment(Pos.TOP_LEFT);

        Label heading = new Label(toDisplayName(workflowType));
        heading.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Label body = new Label("JavaFX view placeholder. Controller and view model wiring is available for this workflow.");
        body.setWrapText(true);

        panel.getChildren().addAll(heading, body);
        return panel;
    }

    private Label createStatusBar(MainWindowViewModel viewModel) {
        Label status = new Label();
        status.textProperty().bind(viewModel.statusMessageProperty());
        status.setMinHeight(34);
        status.setMaxWidth(Double.MAX_VALUE);
        status.setPadding(new Insets(6, 12, 6, 12));
        status.setStyle("-fx-background-color: #eef1f4; -fx-border-color: #d7dde3; -fx-border-width: 1 0 0 0;");
        return status;
    }

    private String toDisplayName(GuiWorkflowType workflowType) {
        return switch (workflowType) {
            case IMPORT_DATA -> "Import Data";
            case DERIVED_DATA -> "Derived Data";
            case EVENT_STATISTICS -> "Event Statistics";
            case BACKTEST -> "Backtest";
            case BENCHMARK -> "Benchmark";
            case DATABASE_CONFIG -> "Database Config";
        };
    }
}
