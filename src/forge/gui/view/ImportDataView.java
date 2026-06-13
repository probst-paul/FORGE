package forge.gui.view;

import forge.data.build.DerivedDataBuildOption;
import forge.data.importing.DataImportMode;
import forge.data.importing.DataImportPlan;
import forge.data.importing.DataImportResult;
import forge.gui.FacadeForgeGui;
import forge.gui.controller.ImportDataController;
import forge.gui.preset.GuiPreferencesStore;
import forge.gui.preset.GuiUserPreferences;
import forge.gui.viewmodel.ImportDataViewModel;
import forge.gui.viewmodel.GuiWorkflowType;
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
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
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

        ToggleGroup importModeGroup = new ToggleGroup();
        RadioButton fillMissingMode = new RadioButton("Fill missing trades");
        fillMissingMode.setToggleGroup(importModeGroup);
        fillMissingMode.setSelected(true);
        RadioButton overwriteOverlapMode = new RadioButton("Overwrite overlapping stored data");
        overwriteOverlapMode.setToggleGroup(importModeGroup);
        fillMissingMode.disableProperty().bind(viewModel.runningProperty());
        overwriteOverlapMode.disableProperty().bind(viewModel.runningProperty());

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
                selectedImportMode(fillMissingMode),
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
                new Label("Import mode"),
                fillMissingMode,
                overwriteOverlapMode,
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
            DataImportMode importMode,
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
            if (plan.hasExistingRows() && !confirmImport(owner, plan, importMode)) {
                controller.getViewModel().setStatusMessage("Import canceled.");
                return;
            }

            Task<DataImportResult> task = controller.importDataTask(scidFilePath, importMode, derivedDataOptions);
            FacadeForgeGui.getTheInstance()
                    .forgeGuiAccess()
                    .submitWorkflowTask(GuiWorkflowType.IMPORT_DATA, task);
        } catch (RuntimeException exception) {
            controller.getViewModel().markFailed("Could not start import.", exception);
        }
    }

    private DataImportMode selectedImportMode(RadioButton fillMissingMode) {
        return fillMissingMode.isSelected() ? DataImportMode.FILL_MISSING : DataImportMode.OVERWRITE_OVERLAP;
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

    private boolean confirmImport(Window owner, DataImportPlan plan, DataImportMode importMode) {
        /*
         * Intent: Ask the user before importing into an existing contract table.
         * Precondition: plan must describe existing target contract data.
         * Returns: true only when the user confirms the selected import mode.
         * Postcondition: No data is modified by the dialog itself.
         */
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(owner);
        alert.setTitle("Confirm Import");
        alert.setHeaderText("Existing data found for "
                + plan.getInstrumentSymbol() + " " + plan.getContractCode());
        alert.setContentText(importConfirmationText(plan, importMode));
        Optional<ButtonType> selection = alert.showAndWait();
        return selection.isPresent() && selection.get() == ButtonType.OK;
    }

    private String importConfirmationText(DataImportPlan plan, DataImportMode importMode) {
        String modeDescription = importMode == DataImportMode.OVERWRITE_OVERLAP
                ? "FORGE will delete only stored rows that overlap the selected file's importable time range, then import this file."
                : "FORGE will keep existing rows and add only trades from this file that are not already stored.";
        return "Instrument: " + plan.getInstrumentSymbol()
                + "\nContract: " + plan.getContractCode()
                + "\nStored rows: " + plan.getExistingRows()
                + "\nStored coverage: " + dateTimeRange(plan.getExistingFirstTradeDateTime(), plan.getExistingLastTradeDateTime())
                + "\nSelected file coverage: " + dateTimeRange(plan.getFileFirstTradeDateTime(), plan.getFileLastTradeDateTime())
                + "\nOverlapping stored rows: " + plan.getOverlappingRows()
                + "\n\n" + modeDescription;
    }

    private String dateTimeRange(java.time.Instant start, java.time.Instant end) {
        if (start == null || end == null) {
            return "None";
        }
        return start + " to " + end;
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
