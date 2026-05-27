package forge.gui.controller;

import forge.app.DatabaseConnectionRequest;
import forge.app.FacadeForgeApplication;
import forge.gui.viewmodel.DatabaseConfigViewModel;
import javafx.concurrent.Task;

public class DatabaseConfigController {
    private final FacadeForgeApplication forgeApplication;
    private final DatabaseConfigViewModel viewModel;

    public DatabaseConfigController() {
        this(FacadeForgeApplication.getTheInstance(), new DatabaseConfigViewModel());
    }

    public DatabaseConfigController(FacadeForgeApplication forgeApplication, DatabaseConfigViewModel viewModel) {
        if (forgeApplication == null) {
            throw new IllegalArgumentException("forgeApplication is required");
        }
        if (viewModel == null) {
            throw new IllegalArgumentException("viewModel is required");
        }
        this.forgeApplication = forgeApplication;
        this.viewModel = viewModel;
    }

    public DatabaseConfigViewModel getViewModel() {
        return viewModel;
    }

    public DatabaseConnectionRequest configureDatabase(
            String host,
            int port,
            String databaseName,
            String maintenanceDatabaseName,
            String username,
            String password
    ) {
        viewModel.setHost(host);
        viewModel.setPort(port);
        viewModel.setDatabaseName(databaseName);
        viewModel.setMaintenanceDatabaseName(maintenanceDatabaseName);
        viewModel.setUsername(username);
        viewModel.markStarted("Configuring database...");
        try {
            DatabaseConnectionRequest request = forgeApplication.forgeApplicationAccess().configureDatabase(
                    new DatabaseConnectionRequest(
                            host,
                            port,
                            databaseName,
                            maintenanceDatabaseName,
                            username,
                            password
                    )
            );
            applyDatabaseConnectionRequest(request);
            return request;
        } catch (RuntimeException exception) {
            viewModel.setConfigured(false);
            viewModel.markFailed("Could not configure database.", exception);
            throw exception;
        }
    }

    public Task<DatabaseConnectionRequest> configureDatabaseTask(
            String host,
            int port,
            String databaseName,
            String maintenanceDatabaseName,
            String username,
            String password
    ) {
        viewModel.setHost(host);
        viewModel.setPort(port);
        viewModel.setDatabaseName(databaseName);
        viewModel.setMaintenanceDatabaseName(maintenanceDatabaseName);
        viewModel.setUsername(username);
        return GuiControllerTasks.create(
                viewModel,
                "Configuring database...",
                "Could not configure database.",
                task -> forgeApplication.forgeApplicationAccess().configureDatabase(new DatabaseConnectionRequest(
                        host,
                        port,
                        databaseName,
                        maintenanceDatabaseName,
                        username,
                        password
                )),
                this::applyDatabaseConnectionRequest
        );
    }

    private void applyDatabaseConnectionRequest(DatabaseConnectionRequest request) {
        viewModel.setConfigured(true);
        viewModel.markSucceeded("Database configured.", "Connected to database " + request.getDatabaseName() + ".");
    }
}
