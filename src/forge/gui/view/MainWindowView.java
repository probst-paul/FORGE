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

public class MainWindowView {
    public void render(MainWindowViewModel viewModel) {
        if (viewModel == null) {
            throw new IllegalArgumentException("viewModel is required");
        }
        System.out.println(viewModel.getStatusMessage());
    }

    public void render(Stage stage, MainWindowViewModel viewModel) {
        if (stage == null) {
            throw new IllegalArgumentException("stage is required");
        }
        if (viewModel == null) {
            throw new IllegalArgumentException("viewModel is required");
        }

        BorderPane root = new BorderPane();
        root.setLeft(createNavigation(stage, viewModel, root));
        root.setCenter(createWorkflowContent(stage, viewModel.getActiveWorkflow()));
        root.setBottom(createStatusBar(viewModel));

        Scene scene = new Scene(root, 1180, 760);
        stage.setTitle(viewModel.getWindowTitle());
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }

    private VBox createNavigation(Window owner, MainWindowViewModel viewModel, BorderPane root) {
        VBox navigation = new VBox(8);
        navigation.setPadding(new Insets(14));
        navigation.setPrefWidth(230);
        navigation.setStyle("-fx-background-color: #f4f6f8; -fx-border-color: #d7dde3; -fx-border-width: 0 1 0 0;");

        Label title = new Label("FORGE");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        navigation.getChildren().add(title);
        navigation.getChildren().add(createNavigationButton("Import Data", GuiWorkflowType.IMPORT_DATA, owner, viewModel, root));
        navigation.getChildren().add(createNavigationButton("Derived Data", GuiWorkflowType.DERIVED_DATA, owner, viewModel, root));
        navigation.getChildren().add(createNavigationButton("Event Statistics", GuiWorkflowType.EVENT_STATISTICS, owner, viewModel, root));
        navigation.getChildren().add(createNavigationButton("Backtest", GuiWorkflowType.BACKTEST, owner, viewModel, root));
        navigation.getChildren().add(createNavigationButton("Benchmark", GuiWorkflowType.BENCHMARK, owner, viewModel, root));
        navigation.getChildren().add(createNavigationButton("Database Config", GuiWorkflowType.DATABASE_CONFIG, owner, viewModel, root));
        VBox.setVgrow(navigation.getChildren().get(navigation.getChildren().size() - 1), Priority.NEVER);
        return navigation;
    }

    private Button createNavigationButton(
            String label,
            GuiWorkflowType workflowType,
            Window owner,
            MainWindowViewModel viewModel,
            BorderPane root
    ) {
        Button button = new Button(label);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setOnAction(event -> {
            viewModel.setActiveWorkflow(workflowType);
            root.setCenter(createWorkflowContent(owner, workflowType));
            viewModel.setStatusMessage(label + " selected.");
        });
        return button;
    }

    private StackPane createWorkflowContent(Window owner, GuiWorkflowType workflowType) {
        StackPane content = new StackPane();
        content.setPadding(new Insets(28));
        if (workflowType == GuiWorkflowType.IMPORT_DATA) {
            content.getChildren().add(new ImportDataView(FacadeForgeGui.getTheInstance()
                    .forgeGuiAccess()
                    .createImportDataController()).createView(owner));
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
