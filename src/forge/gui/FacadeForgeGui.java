package forge.gui;

import forge.app.FacadeForgeApplication;
import forge.data.FacadeForgeData;
import forge.gui.controller.BacktestController;
import forge.gui.controller.EventStatisticsController;
import forge.gui.controller.ImportDataController;
import forge.gui.controller.MainWindowController;
import forge.gui.concurrency.GuiWorkflowJob;
import forge.gui.concurrency.GuiWorkflowRunner;
import forge.gui.viewmodel.BacktestViewModel;
import forge.gui.viewmodel.EventStatisticsViewModel;
import forge.gui.viewmodel.GuiWorkflowType;
import forge.gui.viewmodel.ImportDataViewModel;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.concurrent.Task;

public class FacadeForgeGui {
    private static final FacadeForgeGui THE_INSTANCE = new FacadeForgeGui();

    private final FacadeForgeApplication forgeApplication;
    private final FacadeForgeData forgeData;
    private final GuiWorkflowRunner workflowRunner;
    private final ForgeGuiAccess access = new ForgeGuiAccess();

    private FacadeForgeGui() {
        this(
                FacadeForgeApplication.getTheInstance(),
                FacadeForgeData.getTheInstance(),
                new GuiWorkflowRunner()
        );
    }

    private FacadeForgeGui(
            FacadeForgeApplication forgeApplication,
            FacadeForgeData forgeData,
            GuiWorkflowRunner workflowRunner
    ) {
        if (forgeApplication == null) {
            throw new IllegalArgumentException("forgeApplication is required");
        }
        if (forgeData == null) {
            throw new IllegalArgumentException("forgeData is required");
        }
        if (workflowRunner == null) {
            throw new IllegalArgumentException("workflowRunner is required");
        }
        this.forgeApplication = forgeApplication;
        this.forgeData = forgeData;
        this.workflowRunner = workflowRunner;
    }

    public static FacadeForgeGui getTheInstance() {
        return THE_INSTANCE;
    }

    /*
     * Intent: Expose the GUI package's simplified access surface.
     * Precondition: The singleton facade has been initialized.
     * Returns: Access object for launching and constructing GUI controllers.
     * Postcondition: Callers do not need to directly wire GUI dependencies.
     */
    public ForgeGuiAccess forgeGuiAccess() {
        return access;
    }

    public class ForgeGuiAccess {
        /*
         * Intent: Launch the JavaFX GUI from a facade call.
         * Precondition: JavaFX runtime must be available and not already launched in this JVM.
         * Returns: Nothing.
         * Postcondition: Control is handed to the JavaFX application lifecycle.
         */
        public void launch(String[] args) {
            ForgeGuiApplication.launchGui(args);
        }

        /*
         * Intent: Create the main window controller used by application startup.
         * Precondition: None.
         * Returns: Controller with a fresh main-window view model and view.
         * Postcondition: The caller receives a ready-to-render main window controller.
         */
        public MainWindowController createMainWindowController() {
            return new MainWindowController();
        }

        /*
         * Intent: Create the import-data controller with application facade access.
         * Precondition: Application facade dependencies must be initialized.
         * Returns: ImportDataController with a fresh view model.
         * Postcondition: The GUI can plan and run SCID imports without wiring app services directly.
         */
        public ImportDataController createImportDataController() {
            return new ImportDataController(forgeApplication, forgeData, new ImportDataViewModel());
        }

        public EventStatisticsController createEventStatisticsController() {
            return new EventStatisticsController(forgeApplication, new EventStatisticsViewModel());
        }

        public BacktestController createBacktestController() {
            return new BacktestController(forgeApplication, new BacktestViewModel());
        }

        public <T> GuiWorkflowJob<T> submitWorkflowTask(GuiWorkflowType workflowType, Task<T> task) {
            /*
             * Intent: Queue a GUI workflow task on the shared background runner.
             * Precondition: workflowType and task must describe a GUI import/statistics/backtest workflow.
             * Returns: Job handle with status and cancellation access.
             * Postcondition: The task is submitted without each view creating its own thread.
             */
            return workflowRunner.submit(workflowType, task);
        }

        public void shutdownWorkflowRunner() {
            /*
             * Intent: Stop queued/running GUI background work during application shutdown.
             * Precondition: JavaFX application is closing or no more GUI work should be accepted.
             * Returns: Nothing.
             * Postcondition: The shared executor is asked to interrupt active work and clear queued work.
             */
            workflowRunner.shutdown();
        }

        public ReadOnlyStringProperty taskStatusTextProperty() {
            return workflowRunner.taskStatusTextProperty();
        }

        public ReadOnlyStringProperty taskQueueTextProperty() {
            return workflowRunner.taskQueueTextProperty();
        }

        public ReadOnlyStringProperty taskPercentTextProperty() {
            return workflowRunner.taskPercentTextProperty();
        }

        public ReadOnlyDoubleProperty currentTaskProgressProperty() {
            return workflowRunner.currentTaskProgressProperty();
        }

        public ReadOnlyBooleanProperty taskIndicatorVisibleProperty() {
            return workflowRunner.taskIndicatorVisibleProperty();
        }
    }
}
