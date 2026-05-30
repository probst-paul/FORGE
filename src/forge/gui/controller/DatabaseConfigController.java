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
        /*
         * Intent: Configure database access synchronously for tests or non-task GUI callers.
         * Precondition: Connection fields must identify a reachable PostgreSQL database setup.
         * Returns: Applied database connection request.
         * Postcondition: The view model is marked configured or failed.
         */
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
        /*
         * Intent: Create a JavaFX task for configuring database access off the UI thread.
         * Precondition: Connection fields must identify a reachable PostgreSQL database setup.
         * Returns: Task that yields the applied database connection request.
         * Postcondition: Task completion updates the view model through shared task handling.
         */
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
        /*
         * Intent: Reflect successful database configuration in GUI state.
         * Precondition: request must be the applied connection request.
         * Returns: Nothing.
         * Postcondition: The view model is marked configured and displays the connected database.
         */
        viewModel.setConfigured(true);
        viewModel.markSucceeded("Database configured.", "Connected to database " + request.getDatabaseName() + ".");
    }
}
