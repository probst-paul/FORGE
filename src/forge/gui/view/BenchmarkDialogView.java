package forge.gui.view;

import forge.gui.FacadeForgeGui;
import forge.gui.controller.BenchmarkController;
import forge.gui.preset.GuiPreferencesStore;
import forge.gui.preset.GuiUserPreferences;
import forge.gui.viewmodel.BenchmarkViewModel;
import forge.gui.viewmodel.GuiWorkflowType;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;

public class BenchmarkDialogView {
    private final BenchmarkController controller;
    private final GuiPreferencesStore preferencesStore;

    public BenchmarkDialogView(BenchmarkController controller) {
        this(controller, new GuiPreferencesStore());
    }

    public BenchmarkDialogView(BenchmarkController controller, GuiPreferencesStore preferencesStore) {
        if (controller == null) {
            throw new IllegalArgumentException("controller is required");
        }
        if (preferencesStore == null) {
            throw new IllegalArgumentException("preferencesStore is required");
        }
        this.controller = controller;
        this.preferencesStore = preferencesStore;
    }

    public void show(Window owner) {
        /*
         * Intent: Open the benchmark workflow dialog from Settings.
         * Precondition: owner may be null when no parent window is available.
         * Returns: Nothing.
         * Postcondition: User can select a SCID file and submit a benchmark background task.
         */
        Stage stage = new Stage();
        if (owner != null) {
            stage.initOwner(owner);
            stage.initModality(Modality.NONE);
        }
        stage.setTitle("Benchmark");
        stage.setMinWidth(760);
        stage.setMinHeight(520);
        stage.setScene(new Scene(createContent(stage), 860, 560));
        stage.show();
    }

    private VBox createContent(Window owner) {
        /*
         * Intent: Build benchmark dialog controls and bind them to the benchmark view model.
         * Precondition: owner is the benchmark dialog window.
         * Returns: Root layout for the benchmark dialog.
         * Postcondition: Run button submits benchmark work through the shared GUI workflow runner.
         */
        BenchmarkViewModel viewModel = controller.getViewModel();
        GuiUserPreferences preferences = preferencesStore.load();

        Label heading = new Label("Benchmark");
        heading.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        Label description = new Label(
                "Runs import, derived-data build, event statistics, and backtest workflows with progress timing."
        );
        description.setWrapText(true);

        TextField filePathField = new TextField();
        filePathField.setPromptText("Select a Sierra Chart .scid file");
        filePathField.setEditable(false);
        if (!preferences.getImportScidFilePath().isEmpty()) {
            filePathField.setText(preferences.getImportScidFilePath());
            viewModel.setScidFilePath(preferences.getImportScidFilePath());
        }

        Button browseButton = new Button("Browse...");
        Button runButton = new Button("Run Benchmark");
        runButton.disableProperty().bind(filePathField.textProperty().isEmpty().or(viewModel.runningProperty()));
        browseButton.disableProperty().bind(viewModel.runningProperty());

        CheckBox overwriteOverlap = new CheckBox("Overwrite overlapping stored data");
        overwriteOverlap.setSelected(viewModel.shouldOverwriteOverlappingData());
        overwriteOverlap.disableProperty().bind(viewModel.runningProperty());

        CheckBox rebuildDerivedData = new CheckBox("Rebuild derived data");
        rebuildDerivedData.setSelected(viewModel.shouldRebuildDerivedData());
        rebuildDerivedData.disableProperty().bind(viewModel.runningProperty());

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.progressProperty().bind(viewModel.progressProperty());
        progressBar.setMaxWidth(Double.MAX_VALUE);

        Label statusLabel = new Label();
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());

        TextArea resultText = new TextArea();
        resultText.setEditable(false);
        resultText.setWrapText(true);
        resultText.setPrefRowCount(9);
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
                saveScidSelection(selectedFile);
            }
        });

        runButton.setOnAction(event -> runBenchmark(
                filePathField.getText(),
                overwriteOverlap.isSelected(),
                rebuildDerivedData.isSelected()
        ));

        HBox fileSelection = new HBox(8, filePathField, browseButton);
        fileSelection.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(filePathField, Priority.ALWAYS);

        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.setAlignment(Pos.TOP_LEFT);
        root.getChildren().addAll(
                heading,
                description,
                new Label("SCID data file"),
                fileSelection,
                overwriteOverlap,
                rebuildDerivedData,
                runButton,
                progressBar,
                statusLabel,
                resultText,
                errorLabel
        );
        return root;
    }

    private File chooseScidFile(Window owner) {
        /*
         * Intent: Let the user select a SCID file for benchmark input.
         * Precondition: owner may be null and saved directory may not exist.
         * Returns: Selected file, or null when canceled.
         * Postcondition: No benchmark is started by file selection alone.
         */
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Benchmark SCID Data File");
        configureInitialDirectory(fileChooser);
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Sierra Chart SCID files", "*.scid")
        );
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("All files", "*")
        );
        return fileChooser.showOpenDialog(owner);
    }

    private void runBenchmark(String scidFilePath, boolean overwriteOverlappingData, boolean rebuildDerivedData) {
        /*
         * Intent: Submit the selected SCID file to the benchmark workflow.
         * Precondition: scidFilePath should identify a .scid file.
         * Returns: Nothing.
         * Postcondition: Benchmark task is queued on the shared GUI workflow runner or failure is reported.
         */
        try {
            saveScidPath(scidFilePath);
            Task<?> task = controller.runBenchmarkTask(scidFilePath, overwriteOverlappingData, rebuildDerivedData);
            FacadeForgeGui.getTheInstance()
                    .forgeGuiAccess()
                    .submitWorkflowTask(GuiWorkflowType.SETTINGS, task);
        } catch (RuntimeException exception) {
            controller.getViewModel().markFailed("Could not start benchmark.", exception);
        }
    }

    private void configureInitialDirectory(FileChooser fileChooser) {
        GuiUserPreferences preferences = preferencesStore.load();
        File directory = null;
        if (!preferences.getLastScidDirectory().isEmpty()) {
            directory = new File(preferences.getLastScidDirectory());
        }
        if ((directory == null || !directory.isDirectory()) && !preferences.getImportScidFilePath().isEmpty()) {
            File selectedFile = new File(preferences.getImportScidFilePath());
            directory = selectedFile.getParentFile();
        }
        if (directory != null && directory.isDirectory()) {
            fileChooser.setInitialDirectory(directory);
        }
    }

    private void saveScidSelection(File selectedFile) {
        if (selectedFile == null) {
            return;
        }
        GuiUserPreferences preferences = preferencesStore.load();
        preferences.setImportScidFilePath(selectedFile.getAbsolutePath());
        File parent = selectedFile.getParentFile();
        if (parent != null) {
            preferences.setLastScidDirectory(parent.getAbsolutePath());
        }
        preferencesStore.save(preferences);
    }

    private void saveScidPath(String scidFilePath) {
        if (scidFilePath == null || scidFilePath.trim().isEmpty()) {
            return;
        }
        saveScidSelection(new File(scidFilePath.trim()));
    }
}
