package forge.gui.controller;

import forge.app.FacadeForgeApplication;
import forge.gui.viewmodel.SettingsViewModel;
import javafx.concurrent.Task;

public class SettingsController {
    private final FacadeForgeApplication forgeApplication;
    private final SettingsViewModel viewModel;

    public SettingsController() {
        this(FacadeForgeApplication.getTheInstance(), new SettingsViewModel());
    }

    public SettingsController(FacadeForgeApplication forgeApplication, SettingsViewModel viewModel) {
        if (forgeApplication == null) {
            throw new IllegalArgumentException("forgeApplication is required");
        }
        if (viewModel == null) {
            throw new IllegalArgumentException("viewModel is required");
        }
        this.forgeApplication = forgeApplication;
        this.viewModel = viewModel;
    }

    public SettingsViewModel getViewModel() {
        return viewModel;
    }

    public Task<Integer> wipeDatabaseTask() {
        /*
         * Intent: Create a JavaFX task that drops configured FORGE database tables.
         * Precondition: The caller must have completed destructive-action confirmation.
         * Returns: Task yielding the number of dropped tables.
         * Postcondition: Task completion updates settings workflow state.
         */
        return GuiControllerTasks.create(
                viewModel,
                "Wiping database...",
                "Could not wipe database.",
                task -> {
                    task.publishProgress(0, 1);
                    int droppedTables = forgeApplication.forgeApplicationAccess().wipeDatabase();
                    task.publishProgress(1, 1);
                    return droppedTables;
                },
                this::applyWipeResult
        );
    }

    private void applyWipeResult(int droppedTables) {
        /*
         * Intent: Copy database wipe results into GUI state.
         * Precondition: droppedTables must be non-negative.
         * Returns: Nothing.
         * Postcondition: Settings view model shows completion status and dropped table count.
         */
        viewModel.setDroppedTableCount(droppedTables);
        viewModel.markSucceeded(
                "Database wipe complete.",
                "Dropped " + droppedTables + " database table(s)."
        );
    }
}
