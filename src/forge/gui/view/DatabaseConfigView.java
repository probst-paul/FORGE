package forge.gui.view;

import forge.app.DatabaseConnectionRequest;
import forge.gui.controller.DatabaseConfigController;
import forge.gui.viewmodel.DatabaseConfigViewModel;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class DatabaseConfigView {
    private final DatabaseConfigController controller;

    public DatabaseConfigView(DatabaseConfigController controller) {
        if (controller == null) {
            throw new IllegalArgumentException("controller is required");
        }
        this.controller = controller;
    }

    public Parent createView() {
        DatabaseConfigViewModel viewModel = controller.getViewModel();

        Label heading = new Label("Database Config");
        heading.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        TextField hostField = new TextField(defaultValue(viewModel.getHost(), "localhost"));
        TextField portField = new TextField(String.valueOf(viewModel.getPort()));
        TextField databaseNameField = new TextField(viewModel.getDatabaseName());
        TextField maintenanceDatabaseField = new TextField(viewModel.getMaintenanceDatabaseName());
        TextField usernameField = new TextField(viewModel.getUsername());
        PasswordField passwordField = new PasswordField();

        hostField.setPromptText("localhost");
        portField.setPromptText("5432");
        databaseNameField.setPromptText("forge");
        maintenanceDatabaseField.setPromptText("postgres");
        usernameField.setPromptText("postgres");
        passwordField.setPromptText("Password");

        Button configureButton = new Button("Configure Database");
        configureButton.disableProperty().bind(viewModel.runningProperty());

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.visibleProperty().bind(viewModel.runningProperty());
        progressIndicator.managedProperty().bind(viewModel.runningProperty());
        progressIndicator.setMaxWidth(24);
        progressIndicator.setMaxHeight(24);

        Label statusLabel = new Label();
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());

        TextArea resultText = new TextArea();
        resultText.setEditable(false);
        resultText.setWrapText(true);
        resultText.setPrefRowCount(4);
        resultText.textProperty().bind(viewModel.resultSummaryProperty());

        Label errorLabel = new Label();
        errorLabel.textProperty().bind(viewModel.errorMessageProperty());
        errorLabel.setStyle("-fx-text-fill: #b00020;");

        configureButton.setOnAction(event -> configureDatabase(
                hostField.getText(),
                portField.getText(),
                databaseNameField.getText(),
                maintenanceDatabaseField.getText(),
                usernameField.getText(),
                passwordField.getText()
        ));

        HBox actions = new HBox(10, configureButton, progressIndicator);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(12);
        root.setPadding(new Insets(4));
        root.setAlignment(Pos.TOP_LEFT);
        root.getChildren().addAll(
                heading,
                createForm(
                        hostField,
                        portField,
                        databaseNameField,
                        maintenanceDatabaseField,
                        usernameField,
                        passwordField
                ),
                actions,
                statusLabel,
                resultText,
                errorLabel
        );
        return root;
    }

    private GridPane createForm(
            TextField hostField,
            TextField portField,
            TextField databaseNameField,
            TextField maintenanceDatabaseField,
            TextField usernameField,
            PasswordField passwordField
    ) {
        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);

        addRow(form, 0, "Host", hostField);
        addRow(form, 1, "Port", portField);
        addRow(form, 2, "Database", databaseNameField);
        addRow(form, 3, "Maintenance DB", maintenanceDatabaseField);
        addRow(form, 4, "Username", usernameField);
        addRow(form, 5, "Password", passwordField);

        return form;
    }

    private void addRow(GridPane form, int rowIndex, String labelText, TextField field) {
        Label label = new Label(labelText);
        label.setMinWidth(130);
        field.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(field, Priority.ALWAYS);
        form.add(label, 0, rowIndex);
        form.add(field, 1, rowIndex);
    }

    private void configureDatabase(
            String host,
            String portText,
            String databaseName,
            String maintenanceDatabaseName,
            String username,
            String password
    ) {
        DatabaseConfigViewModel viewModel = controller.getViewModel();
        int port;
        try {
            port = Integer.parseInt(portText == null ? "" : portText.trim());
        } catch (NumberFormatException exception) {
            viewModel.markFailed("Could not configure database.", new RuntimeException("Port must be a number."));
            return;
        }

        String resolvedHost = defaultValue(host, "localhost");
        String resolvedDatabaseName = defaultValue(databaseName, "forge");
        String resolvedMaintenanceDatabaseName = defaultValue(maintenanceDatabaseName, "postgres");
        String resolvedUsername = defaultValue(username, "postgres");

        try {
            Task<DatabaseConnectionRequest> task = controller.configureDatabaseTask(
                    resolvedHost,
                    port,
                    resolvedDatabaseName,
                    resolvedMaintenanceDatabaseName,
                    resolvedUsername,
                    password
            );
            Thread thread = new Thread(task, "forge-gui-database-config");
            thread.setDaemon(true);
            thread.start();
        } catch (RuntimeException exception) {
            viewModel.setConfigured(false);
            viewModel.markFailed("Could not configure database.", exception);
        }
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
