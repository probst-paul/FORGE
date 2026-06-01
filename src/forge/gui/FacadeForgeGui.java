package forge.gui;

import forge.app.FacadeForgeApplication;
import forge.benchmark.FacadeForgeBenchmark;
import forge.data.FacadeForgeData;
import forge.gui.controller.BacktestController;
import forge.gui.controller.BenchmarkController;
import forge.gui.controller.DatabaseConfigController;
import forge.gui.controller.DerivedDataController;
import forge.gui.controller.EventStatisticsController;
import forge.gui.controller.ImportDataController;
import forge.gui.controller.MainWindowController;
import forge.gui.viewmodel.BacktestViewModel;
import forge.gui.viewmodel.BenchmarkViewModel;
import forge.gui.viewmodel.DatabaseConfigViewModel;
import forge.gui.viewmodel.DerivedDataViewModel;
import forge.gui.viewmodel.EventStatisticsViewModel;
import forge.gui.viewmodel.ImportDataViewModel;

public class FacadeForgeGui {
    private static final FacadeForgeGui THE_INSTANCE = new FacadeForgeGui();

    private final FacadeForgeApplication forgeApplication;
    private final FacadeForgeData forgeData;
    private final FacadeForgeBenchmark forgeBenchmark;
    private final ForgeGuiAccess access = new ForgeGuiAccess();

    private FacadeForgeGui() {
        this(
                FacadeForgeApplication.getTheInstance(),
                FacadeForgeData.getTheInstance(),
                FacadeForgeBenchmark.getTheInstance()
        );
    }

    private FacadeForgeGui(
            FacadeForgeApplication forgeApplication,
            FacadeForgeData forgeData,
            FacadeForgeBenchmark forgeBenchmark
    ) {
        if (forgeApplication == null) {
            throw new IllegalArgumentException("forgeApplication is required");
        }
        if (forgeData == null) {
            throw new IllegalArgumentException("forgeData is required");
        }
        if (forgeBenchmark == null) {
            throw new IllegalArgumentException("forgeBenchmark is required");
        }
        this.forgeApplication = forgeApplication;
        this.forgeData = forgeData;
        this.forgeBenchmark = forgeBenchmark;
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

        /*
         * Intent: Create the derived-data controller with data facade access.
         * Precondition: Data facade dependencies must be initialized.
         * Returns: DerivedDataController with a fresh view model.
         * Postcondition: The GUI can plan and build derived data through the data facade.
         */
        public DerivedDataController createDerivedDataController() {
            return new DerivedDataController(forgeData, new DerivedDataViewModel());
        }

        public EventStatisticsController createEventStatisticsController() {
            return new EventStatisticsController(forgeApplication, new EventStatisticsViewModel());
        }

        public BacktestController createBacktestController() {
            return new BacktestController(forgeApplication, new BacktestViewModel());
        }

        public BenchmarkController createBenchmarkController() {
            return new BenchmarkController(forgeBenchmark, new BenchmarkViewModel());
        }

        public DatabaseConfigController createDatabaseConfigController() {
            return new DatabaseConfigController(forgeApplication, new DatabaseConfigViewModel());
        }
    }
}
