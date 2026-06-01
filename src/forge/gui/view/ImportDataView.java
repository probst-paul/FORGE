package forge.gui.view;

import forge.data.build.DerivedDataBuildOption;
import forge.data.importing.DataImportPlan;
import forge.data.importing.DataImportResult;
import forge.gui.controller.ImportDataController;
import forge.gui.preset.GuiPreferencesStore;
import forge.gui.preset.GuiUserPreferences;
import forge.gui.viewmodel.ImportDataViewModel;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

public class ImportDataView {
    private final ImportDataController controller;
    private final GuiPreferencesStore preferencesStore;

    public ImportDataView(ImportDataController controller) {
        this(controller, new GuiPreferencesStore());
    }

    public ImportDataView(ImportDataController controller, GuiPreferencesStore preferencesStore) {
        if (controller == null) {
            throw new IllegalArgumentException("controller is required");
        }
        if (preferencesStore == null) {
            throw new IllegalArgumentException("preferencesStore is required");
        }
        this.controller = controller;
        this.preferencesStore = preferencesStore;
    }

    public Parent createView(Window owner) {
        /*
         * Intent: Build the import-data screen and wire file selection/import actions.
         * Precondition: owner may be null when no parent window is available.
         * Returns: JavaFX parent node for the import-data workflow.
         * Postcondition: View controls are bound to the import view model.
         */
        ImportDataViewModel viewModel = controller.getViewModel();
        GuiUserPreferences preferences = preferencesStore.load();

        Label heading = new Label("Import Data");
        heading.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        TextField filePathField = new TextField();
        filePathField.setPromptText("Select a Sierra Chart .scid file");
        filePathField.setEditable(false);
        if (!preferences.getImportScidFilePath().isEmpty()) {
            filePathField.setText(preferences.getImportScidFilePath());
            viewModel.setScidFilePath(preferences.getImportScidFilePath());
        }

        Button browseButton = new Button("Browse...");
        Button importButton = new Button("Import");
        importButton.disableProperty().bind(filePathField.textProperty().isEmpty().or(viewModel.runningProperty()));
        browseButton.disableProperty().bind(viewModel.runningProperty());

        CheckBox buildSessionRanges = new CheckBox("Build session ranges after import");
        CheckBox buildFirstHourBreachEvents = new CheckBox("Build first-hour breach events after import");
        buildSessionRanges.disableProperty().bind(viewModel.runningProperty());
        buildFirstHourBreachEvents.disableProperty().bind(viewModel.runningProperty());

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

        browseButton.setOnAction(event -> {
            File selectedFile = chooseScidFile(owner);
            if (selectedFile != null) {
                filePathField.setText(selectedFile.getAbsolutePath());
                viewModel.setScidFilePath(selectedFile.getAbsolutePath());
                viewModel.setStatusMessage("Selected " + selectedFile.getName() + ".");
                saveScidSelection(selectedFile, true);
            }
        });

        importButton.setOnAction(event -> importSelectedFile(
                owner,
                filePathField.getText(),
                selectedDerivedDataOptions(buildSessionRanges, buildFirstHourBreachEvents)
        ));

        HBox fileSelection = new HBox(8, filePathField, browseButton);
        fileSelection.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(filePathField, javafx.scene.layout.Priority.ALWAYS);

        VBox root = new VBox(12);
        root.setPadding(new Insets(4));
        root.setAlignment(Pos.TOP_LEFT);
        root.getChildren().addAll(
                heading,
                new Label("SCID data file"),
                fileSelection,
                new Label("Build derived data after import"),
                buildSessionRanges,
                buildFirstHourBreachEvents,
                importButton,
                progressBar,
                statusLabel,
                resultText,
                errorLabel
        );
        return root;
    }

    private File chooseScidFile(Window owner) {
        /*
         * Intent: Let the user select a Sierra Chart SCID file with the system file chooser.
         * Precondition: owner may be null; saved directory may or may not exist.
         * Returns: Selected file, or null when the dialog is canceled.
         * Postcondition: No import is started by file selection alone.
         */
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select SCID Data File");
        configureInitialDirectory(fileChooser);
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Sierra Chart SCID files", "*.scid")
        );
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("All files", "*")
        );
        return fileChooser.showOpenDialog(owner);
    }

    private void importSelectedFile(
            Window owner,
            String scidFilePath,
            Set<DerivedDataBuildOption> derivedDataOptions
    ) {
        /*
         * Intent: Confirm rebuild when needed and launch the import task.
         * Precondition: scidFilePath should identify the selected SCID file.
         * Returns: Nothing.
         * Postcondition: A daemon import thread is started, or the view model reports cancellation/failure.
         */
        try {
            saveScidPath(scidFilePath, true);
            DataImportPlan plan = controller.planImport(scidFilePath);
            if (plan.hasExistingContractTable() && !confirmRebuild(owner, plan)) {
                controller.getViewModel().setStatusMessage("Import canceled.");
                return;
            }

            Task<DataImportResult> task = controller.importDataTask(scidFilePath, true, derivedDataOptions);
            Thread thread = new Thread(task, "forge-gui-import-data");
            thread.setDaemon(true);
            thread.start();
        } catch (RuntimeException exception) {
            controller.getViewModel().markFailed("Could not start import.", exception);
        }
    }

    private Set<DerivedDataBuildOption> selectedDerivedDataOptions(
            CheckBox buildSessionRanges,
            CheckBox buildFirstHourBreachEvents
    ) {
        /*
         * Intent: Convert post-import checkbox state into derived-data build options.
         * Precondition: Checkboxes must be the controls from this import view.
         * Returns: Selected derived-data build options.
         * Postcondition: Checkbox state is unchanged.
         */
        Set<DerivedDataBuildOption> options = EnumSet.noneOf(DerivedDataBuildOption.class);
        if (buildSessionRanges.isSelected()) {
            options.add(DerivedDataBuildOption.SESSION_RANGES);
        }
        if (buildFirstHourBreachEvents.isSelected()) {
            options.add(DerivedDataBuildOption.FIRST_HOUR_BREACH_EVENTS);
        }
        return options;
    }

    private boolean confirmRebuild(Window owner, DataImportPlan plan) {
        /*
         * Intent: Ask the user before wiping/rebuilding existing contract data.
         * Precondition: plan must describe an existing target contract table.
         * Returns: true only when the user confirms the rebuild.
         * Postcondition: No data is modified by the dialog itself.
         */
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(owner);
        alert.setTitle("Confirm Rebuild");
        alert.setHeaderText("Existing data found for " + plan.getContractSymbol());
        alert.setContentText(
                "Table: " + plan.getTableName()
                        + "\nRows: " + plan.getExistingRows()
                        + "\nCurrent source: " + displayValue(plan.getCurrentSourceFileName())
                        + "\nStatus: " + displayValue(plan.getCurrentImportStatus())
                        + "\n\nWipe and rebuild this contract from the selected SCID file?"
        );
        Optional<ButtonType> selection = alert.showAndWait();
        return selection.isPresent() && selection.get() == ButtonType.OK;
    }

    private String displayValue(String value) {
        return value == null || value.trim().isEmpty() ? "None" : value.trim();
    }

    private void configureInitialDirectory(FileChooser fileChooser) {
        String lastDirectory = preferencesStore.load().getLastScidDirectory();
        if (lastDirectory.isEmpty()) {
            return;
        }
        File directory = new File(lastDirectory);
        if (directory.isDirectory()) {
            fileChooser.setInitialDirectory(directory);
        }
    }

    private void saveScidSelection(File selectedFile, boolean importPath) {
        if (selectedFile == null) {
            return;
        }
        saveScidPath(selectedFile.getAbsolutePath(), importPath);
    }

    private void saveScidPath(String scidFilePath, boolean importPath) {
        if (scidFilePath == null || scidFilePath.trim().isEmpty()) {
            return;
        }
        GuiUserPreferences preferences = preferencesStore.load();
        File selectedFile = new File(scidFilePath.trim());
        if (selectedFile.getParentFile() != null) {
            preferences.setLastScidDirectory(selectedFile.getParentFile().getAbsolutePath());
        }
        if (importPath) {
            preferences.setImportScidFilePath(selectedFile.getAbsolutePath());
        }
        preferencesStore.save(preferences);
    }
}
