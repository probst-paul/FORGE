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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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
        addNavigationButton(navigation, navigationButtons, "Event Statistics", GuiWorkflowType.EVENT_STATISTICS,
                owner, viewModel, root, workflowContentCache);
        addNavigationButton(navigation, navigationButtons, "Backtest", GuiWorkflowType.BACKTEST,
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
        content.getChildren().add(switch (workflowType) {
            case IMPORT_DATA -> new ImportDataView(FacadeForgeGui.getTheInstance()
                    .forgeGuiAccess()
                    .createImportDataController()).createView(owner);
            case EVENT_STATISTICS -> new EventStatisticsView(FacadeForgeGui.getTheInstance()
                    .forgeGuiAccess()
                    .createEventStatisticsController()).createView(owner);
            case BACKTEST -> new BacktestView(FacadeForgeGui.getTheInstance()
                    .forgeGuiAccess()
                    .createBacktestController()).createView(owner);
        });
        return content;
    }

    private HBox createStatusBar(MainWindowViewModel viewModel) {
        Label status = new Label();
        status.textProperty().bind(viewModel.statusMessageProperty());
        status.setMinHeight(34);
        status.setMaxWidth(Double.MAX_VALUE);
        status.setPadding(new Insets(6, 12, 6, 12));
        HBox.setHgrow(status, Priority.ALWAYS);

        FacadeForgeGui.ForgeGuiAccess guiAccess = FacadeForgeGui.getTheInstance().forgeGuiAccess();
        Label taskLabel = new Label();
        taskLabel.textProperty().bind(guiAccess.taskQueueTextProperty());
        taskLabel.setStyle("-fx-text-fill: #263238;");

        Label taskPercentLabel = new Label();
        taskPercentLabel.textProperty().bind(guiAccess.taskPercentTextProperty());
        taskPercentLabel.setMinWidth(42);
        taskPercentLabel.setPrefWidth(42);
        taskPercentLabel.setAlignment(Pos.CENTER_RIGHT);
        taskPercentLabel.setStyle("-fx-text-fill: #263238;");

        HBox taskStatus = new HBox(16, taskLabel, taskPercentLabel);
        taskStatus.setAlignment(Pos.CENTER_RIGHT);
        taskStatus.setPadding(new Insets(0, 12, 0, 0));
        taskStatus.visibleProperty().bind(guiAccess.taskIndicatorVisibleProperty());
        taskStatus.managedProperty().bind(guiAccess.taskIndicatorVisibleProperty());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox statusBar = new HBox(status, spacer, taskStatus);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setMinHeight(34);
        statusBar.setStyle("-fx-background-color: #eef1f4; -fx-border-color: #d7dde3; -fx-border-width: 1 0 0 0;");
        return statusBar;
    }

}
