package forge.gui.view;

import forge.gui.FacadeForgeGui;
import forge.gui.controller.SettingsController;
import forge.gui.viewmodel.GuiWorkflowType;
import forge.gui.viewmodel.SettingsViewModel;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.Optional;

public class SettingsView {
    private final SettingsController controller;

    public SettingsView(SettingsController controller) {
        if (controller == null) {
            throw new IllegalArgumentException("controller is required");
        }
        this.controller = controller;
    }

    public Parent createView(Window owner) {
        /*
         * Intent: Build the GUI settings screen for administrative maintenance actions.
         * Precondition: owner may be null when no parent window is available.
         * Returns: JavaFX parent node for the settings workflow.
         * Postcondition: Destructive database actions require confirmation before task submission.
         */
        SettingsViewModel viewModel = controller.getViewModel();

        Label heading = new Label("Settings");
        heading.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Label databaseHeading = new Label("Database Maintenance");
        databaseHeading.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label prepareDescription = new Label(
                "Creates the configured PostgreSQL database if missing and ensures FORGE support tables are available."
        );
        prepareDescription.setWrapText(true);

        Button prepareButton = new Button("Repair/Create Database");
        prepareButton.disableProperty().bind(viewModel.runningProperty());

        Label warning = new Label(
                "Drops all FORGE-owned database tables in the configured PostgreSQL database. " +
                        "Imported trades, derived data, and import metadata will be removed."
        );
        warning.setWrapText(true);

        Button wipeButton = new Button("Drop Database Tables");
        wipeButton.setStyle("-fx-text-fill: #b00020;");
        wipeButton.disableProperty().bind(viewModel.runningProperty());

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.progressProperty().bind(viewModel.progressProperty());
        progressBar.setMaxWidth(Double.MAX_VALUE);

        Label statusLabel = new Label();
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());

        TextArea resultText = new TextArea();
        resultText.setEditable(false);
        resultText.setWrapText(true);
        resultText.setPrefRowCount(5);
        resultText.textProperty().bind(viewModel.resultSummaryProperty());

        Label errorLabel = new Label();
        errorLabel.textProperty().bind(viewModel.errorMessageProperty());
        errorLabel.setStyle("-fx-text-fill: #b00020;");

        prepareButton.setOnAction(event -> prepareDatabase());
        wipeButton.setOnAction(event -> wipeDatabase(owner));

        VBox root = new VBox(12);
        root.setPadding(new Insets(4));
        root.setAlignment(Pos.TOP_LEFT);
        root.getChildren().addAll(
                heading,
                databaseHeading,
                prepareDescription,
                prepareButton,
                warning,
                wipeButton,
                progressBar,
                statusLabel,
                resultText,
                errorLabel
        );
        return root;
    }

    private void prepareDatabase() {
        /*
         * Intent: Submit the non-destructive database repair/create workflow from Settings.
         * Precondition: Settings view has been constructed with a controller.
         * Returns: Nothing.
         * Postcondition: Database prepare task is queued on the shared GUI workflow runner.
         */
        try {
            Task<Void> task = controller.prepareDatabaseTask();
            FacadeForgeGui.getTheInstance()
                    .forgeGuiAccess()
                    .submitWorkflowTask(GuiWorkflowType.SETTINGS, task);
        } catch (RuntimeException exception) {
            controller.getViewModel().markFailed("Could not start database preparation.", exception);
        }
    }

    private void wipeDatabase(Window owner) {
        /*
         * Intent: Confirm and submit the destructive database wipe workflow.
         * Precondition: Settings view has been constructed with a controller.
         * Returns: Nothing.
         * Postcondition: Database wipe task is queued only when the user confirms the warning dialog.
         */
        if (!confirmDatabaseWipe(owner)) {
            controller.getViewModel().setStatusMessage("Database wipe canceled.");
            return;
        }

        try {
            Task<Integer> task = controller.wipeDatabaseTask();
            FacadeForgeGui.getTheInstance()
                    .forgeGuiAccess()
                    .submitWorkflowTask(GuiWorkflowType.SETTINGS, task);
        } catch (RuntimeException exception) {
            controller.getViewModel().markFailed("Could not start database wipe.", exception);
        }
    }

    private boolean confirmDatabaseWipe(Window owner) {
        /*
         * Intent: Ask for explicit confirmation before dropping database tables.
         * Precondition: owner may be null.
         * Returns: true only when the user confirms the destructive action.
         * Postcondition: No data is changed by the dialog itself.
         */
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setTitle("Confirm Database Wipe");
        alert.setHeaderText("Drop all FORGE database tables?");
        alert.setContentText(
                "This will permanently remove imported trades, derived data, and import metadata " +
                        "from the configured PostgreSQL database.\n\nAre you sure?"
        );
        Optional<ButtonType> selection = alert.showAndWait();
        return selection.isPresent() && selection.get() == ButtonType.OK;
    }
}
