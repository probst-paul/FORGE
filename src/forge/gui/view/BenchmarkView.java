package forge.gui.view;

import forge.benchmark.BenchmarkRunResult;
import forge.gui.controller.BenchmarkController;
import forge.gui.preset.GuiPreferencesStore;
import forge.gui.preset.GuiUserPreferences;
import forge.gui.viewmodel.BenchmarkPhaseProgress;
import forge.gui.viewmodel.BenchmarkViewModel;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
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
import javafx.stage.Window;

import java.io.File;
import java.time.Duration;

public class BenchmarkView {
    private final BenchmarkController controller;
    private final GuiPreferencesStore preferencesStore;

    public BenchmarkView(BenchmarkController controller) {
        this(controller, new GuiPreferencesStore());
    }

    public BenchmarkView(BenchmarkController controller, GuiPreferencesStore preferencesStore) {
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
         * Intent: Build the benchmark screen and wire benchmark file/options/actions.
         * Precondition: owner may be null when no parent window is available.
         * Returns: JavaFX parent node for the benchmark workflow.
         * Postcondition: Benchmark controls and phase progress rows are bound to the view model.
         */
        BenchmarkViewModel viewModel = controller.getViewModel();
        GuiUserPreferences preferences = preferencesStore.load();
        StringBuilder benchmarkHistory = new StringBuilder();
        StringProperty totalElapsedMessage = new SimpleStringProperty("total elapsed 0.000s");
        long[] benchmarkStartedAtNanos = {0};

        Label heading = new Label("Benchmark");
        heading.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Label description = new Label(
                "Runs import, derived-data build, event statistics, and backtest workflows with progress timing."
        );
        description.setWrapText(true);

        TextField filePathField = new TextField();
        filePathField.setPromptText("Select a Sierra Chart .scid file");
        filePathField.setEditable(false);
        if (!preferences.getBenchmarkScidFilePath().isEmpty()) {
            filePathField.setText(preferences.getBenchmarkScidFilePath());
            viewModel.setScidFilePath(preferences.getBenchmarkScidFilePath());
        }

        Button browseButton = new Button("Browse...");
        browseButton.disableProperty().bind(viewModel.runningProperty());
        browseButton.setOnAction(event -> {
            File selectedFile = chooseScidFile(owner);
            if (selectedFile != null) {
                filePathField.setText(selectedFile.getAbsolutePath());
                viewModel.setScidFilePath(selectedFile.getAbsolutePath());
                viewModel.setStatusMessage("Selected " + selectedFile.getName() + ".");
                saveBenchmarkPath(selectedFile.getAbsolutePath());
            }
        });

        HBox fileSelection = new HBox(8, filePathField, browseButton);
        fileSelection.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(filePathField, Priority.ALWAYS);

        CheckBox rebuildExistingContractCheckBox = new CheckBox("Rebuild existing contract data");
        rebuildExistingContractCheckBox.setSelected(preferences.shouldBenchmarkRebuildExistingContract());
        rebuildExistingContractCheckBox.disableProperty().bind(viewModel.runningProperty());

        CheckBox rebuildDerivedDataCheckBox = new CheckBox("Rebuild derived data");
        rebuildDerivedDataCheckBox.setSelected(preferences.shouldBenchmarkRebuildDerivedData());
        rebuildDerivedDataCheckBox.disableProperty().bind(viewModel.runningProperty());

        Button runButton = new Button("Run Benchmark");
        runButton.disableProperty().bind(filePathField.textProperty().isEmpty().or(viewModel.runningProperty()));
        runButton.setOnAction(event -> runBenchmark(
                filePathField.getText(),
                rebuildExistingContractCheckBox.isSelected(),
                rebuildDerivedDataCheckBox.isSelected(),
                benchmarkHistory,
                totalElapsedMessage,
                benchmarkStartedAtNanos
        ));

        Label statusLabel = new Label();
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());

        TextArea resultText = new TextArea();
        resultText.setEditable(false);
        resultText.setWrapText(true);
        resultText.setPrefRowCount(10);
        resultText.textProperty().bind(viewModel.resultSummaryProperty());

        Label errorLabel = new Label();
        errorLabel.textProperty().bind(viewModel.errorMessageProperty());
        errorLabel.setStyle("-fx-text-fill: #b00020;");

        VBox options = new VBox(6, rebuildExistingContractCheckBox, rebuildDerivedDataCheckBox);
        options.setPadding(new Insets(4, 0, 4, 0));

        VBox root = new VBox(12);
        root.setPadding(new Insets(4));
        root.setAlignment(Pos.TOP_LEFT);
        root.getChildren().addAll(
                heading,
                description,
                new Label("SCID data file"),
                fileSelection,
                options,
                runButton,
                createProgressSection(viewModel, statusLabel, totalElapsedMessage),
                resultText,
                errorLabel
        );
        startElapsedTimer(viewModel, totalElapsedMessage, benchmarkStartedAtNanos);
        return root;
    }

    private File chooseScidFile(Window owner) {
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

    private VBox createProgressSection(
            BenchmarkViewModel viewModel,
            Label statusLabel,
            StringProperty totalElapsedMessage
    ) {
        Label progressLabel = new Label("Benchmark progress");
        progressLabel.setStyle("-fx-font-weight: bold;");

        Label totalElapsed = new Label();
        totalElapsed.textProperty().bind(totalElapsedMessage);
        totalElapsed.setStyle("-fx-font-weight: bold;");

        HBox heading = new HBox(12, progressLabel, totalElapsed);
        heading.setAlignment(Pos.CENTER_LEFT);

        VBox phaseRows = new VBox(
                8,
                createPhaseProgressRow(viewModel.getImportProgress()),
                createPhaseProgressRow(viewModel.getDerivedDataProgress()),
                createPhaseProgressRow(viewModel.getEventStatisticsProgress()),
                createPhaseProgressRow(viewModel.getBacktestProgress())
        );

        VBox section = new VBox(8, heading, phaseRows, statusLabel);
        section.setPadding(new Insets(8));
        section.setStyle(
                "-fx-background-color: #ffffff;"
                        + "-fx-border-color: #d7dde3;"
                        + "-fx-border-radius: 6;"
                        + "-fx-background-radius: 6;"
        );
        return section;
    }

    private void startElapsedTimer(
            BenchmarkViewModel viewModel,
            StringProperty totalElapsedMessage,
            long[] benchmarkStartedAtNanos
    ) {
        /*
         * Intent: Refresh live elapsed-time labels while a benchmark is running.
         * Precondition: benchmarkStartedAtNanos[0] is set when a run starts.
         * Returns: Nothing.
         * Postcondition: Total and phase elapsed labels update until the run stops.
         */
        Timeline elapsedTimer = new Timeline(new KeyFrame(
                javafx.util.Duration.millis(250),
                event -> {
                    viewModel.refreshPhaseElapsedTimes();
                    if (viewModel.isRunning() && benchmarkStartedAtNanos[0] > 0) {
                        totalElapsedMessage.set("total elapsed "
                                + formatDuration(Duration.ofNanos(System.nanoTime() - benchmarkStartedAtNanos[0])));
                    }
                }
        ));
        elapsedTimer.setCycleCount(Timeline.INDEFINITE);
        viewModel.runningProperty().addListener((observable, wasRunning, running) -> {
            if (running) {
                elapsedTimer.play();
            } else {
                viewModel.refreshPhaseElapsedTimes();
                elapsedTimer.stop();
            }
        });
    }

    private VBox createPhaseProgressRow(BenchmarkPhaseProgress phaseProgress) {
        Label phaseLabel = new Label(phaseProgress.getPhaseName());
        phaseLabel.setMinWidth(115);
        phaseLabel.setStyle("-fx-font-weight: bold;");

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.progressProperty().bind(phaseProgress.progressProperty());
        progressBar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(progressBar, Priority.ALWAYS);

        Label details = new Label();
        details.setMinWidth(160);
        details.setPrefWidth(160);
        details.setMaxWidth(160);
        details.setAlignment(Pos.CENTER_RIGHT);
        details.textProperty().bind(Bindings.createStringBinding(
                () -> String.format(
                        "%.0f%%  %d/%d",
                        phaseProgress.getProgress() * 100.0,
                        phaseProgress.getProcessedUnits(),
                        phaseProgress.getTotalUnits()
                ),
                phaseProgress.progressProperty(),
                phaseProgress.processedUnitsProperty(),
                phaseProgress.totalUnitsProperty()
        ));

        HBox row = new HBox(10, phaseLabel, progressBar, details);
        row.setAlignment(Pos.CENTER_LEFT);

        Label status = new Label();
        status.textProperty().bind(phaseProgress.statusMessageProperty());
        status.setStyle("-fx-text-fill: #555555;");
        Label elapsed = new Label();
        elapsed.textProperty().bind(phaseProgress.elapsedTimeProperty());
        elapsed.setStyle("-fx-text-fill: #555555;");
        HBox subrow = new HBox(12, status, elapsed);
        subrow.setAlignment(Pos.CENTER_LEFT);
        return new VBox(3, row, subrow);
    }

    private void runBenchmark(
            String scidFilePath,
            boolean rebuildExistingContract,
            boolean rebuildDerivedData,
            StringBuilder benchmarkHistory,
            StringProperty totalElapsedMessage,
            long[] benchmarkStartedAtNanos
    ) {
        /*
         * Intent: Persist benchmark options and start the benchmark workflow off the UI thread.
         * Precondition: scidFilePath should identify a selected SCID file.
         * Returns: Nothing.
         * Postcondition: A daemon benchmark thread is started, or the view model reports failure.
         */
        BenchmarkViewModel viewModel = controller.getViewModel();
        try {
            saveBenchmarkPreferences(scidFilePath, rebuildExistingContract, rebuildDerivedData);
            benchmarkHistory.setLength(0);
            viewModel.setResultSummary("");
            benchmarkStartedAtNanos[0] = System.nanoTime();
            totalElapsedMessage.set("total elapsed 0.000s");
            Task<BenchmarkRunResult> task = controller.runBenchmarkTask(
                    scidFilePath,
                    rebuildExistingContract,
                    rebuildDerivedData
            );
            task.addEventHandler(
                    WorkerStateEvent.WORKER_STATE_SUCCEEDED,
                    event -> {
                        totalElapsedMessage.set("total elapsed " + formatDuration(task.getValue().getElapsedTime()));
                        appendResult(benchmarkHistory, viewModel, task.getValue());
                    }
            );
            Thread thread = new Thread(task, "forge-gui-benchmark");
            thread.setDaemon(true);
            thread.start();
        } catch (RuntimeException exception) {
            viewModel.markFailed("Could not run benchmark workflow.", exception);
        }
    }

    private void appendResult(
            StringBuilder benchmarkHistory,
            BenchmarkViewModel viewModel,
            BenchmarkRunResult result
    ) {
        if (!benchmarkHistory.isEmpty()) {
            benchmarkHistory.append(System.lineSeparator()).append(System.lineSeparator());
        }
        benchmarkHistory.append(formatResult(result));
        viewModel.setResultSummary(benchmarkHistory.toString());
    }

    private String formatResult(BenchmarkRunResult result) {
        /*
         * Intent: Format benchmark phase counts and timings for the text output area.
         * Precondition: result must contain all benchmark phase results.
         * Returns: Multi-line benchmark summary.
         * Postcondition: Result objects are not modified.
         */
        StringBuilder builder = new StringBuilder();
        builder.append("Benchmark complete").append(System.lineSeparator());
        builder.append("-------------------------").append(System.lineSeparator());
        builder.append("Contract: ").append(result.getImportResult().getContractSymbol()).append(System.lineSeparator());
        builder.append("Import: ")
                .append(result.getImportResult().getImportedRows())
                .append(" row(s), ")
                .append(formatDuration(result.getImportResult().getElapsedTime()))
                .append(System.lineSeparator());
        builder.append("Derived Data: ")
                .append(result.getDatabaseBuildResult().getTicksRead())
                .append(" tick(s), ")
                .append(formatDuration(result.getDatabaseBuildResult().getElapsedTime()))
                .append(System.lineSeparator());
        builder.append("Event Statistics: ")
                .append(result.getEventStatisticsReport().getInstrumentResults().size())
                .append(" instrument result(s), ")
                .append(result.getEventStatisticsReport().getContractResults().size())
                .append(" contract result(s), ")
                .append(formatDuration(result.getEventStatisticsElapsedTime()))
                .append(System.lineSeparator());
        builder.append("Backtest: ")
                .append(result.getBacktestResult().getTicksProcessed())
                .append(" tick(s), ")
                .append(result.getBacktestResult().getOrderSignalsGenerated())
                .append(" order signal(s), ")
                .append(formatDuration(result.getBacktestElapsedTime()))
                .append(System.lineSeparator());
        builder.append("Total processing time: ").append(formatDuration(result.getElapsedTime()));
        return builder.toString();
    }

    private String formatDuration(Duration duration) {
        return String.format("%.3fs", duration.toMillis() / 1000.0);
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

    private void saveBenchmarkPreferences(
            String scidFilePath,
            boolean rebuildExistingContract,
            boolean rebuildDerivedData
    ) {
        GuiUserPreferences preferences = preferencesStore.load();
        if (scidFilePath != null && !scidFilePath.trim().isEmpty()) {
            File selectedFile = new File(scidFilePath.trim());
            preferences.setBenchmarkScidFilePath(selectedFile.getAbsolutePath());
            if (selectedFile.getParentFile() != null) {
                preferences.setLastScidDirectory(selectedFile.getParentFile().getAbsolutePath());
            }
        }
        preferences.setBenchmarkRebuildExistingContract(rebuildExistingContract);
        preferences.setBenchmarkRebuildDerivedData(rebuildDerivedData);
        preferencesStore.save(preferences);
    }

    private void saveBenchmarkPath(String scidFilePath) {
        if (scidFilePath == null || scidFilePath.trim().isEmpty()) {
            return;
        }
        GuiUserPreferences preferences = preferencesStore.load();
        File selectedFile = new File(scidFilePath.trim());
        preferences.setBenchmarkScidFilePath(selectedFile.getAbsolutePath());
        if (selectedFile.getParentFile() != null) {
            preferences.setLastScidDirectory(selectedFile.getParentFile().getAbsolutePath());
        }
        preferencesStore.save(preferences);
    }
}
