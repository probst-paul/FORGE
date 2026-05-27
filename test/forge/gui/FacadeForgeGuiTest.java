package forge.gui;

import forge.gui.controller.BacktestController;
import forge.gui.controller.BenchmarkController;
import forge.gui.controller.DatabaseConfigController;
import forge.gui.controller.DerivedDataController;
import forge.gui.controller.EventStatisticsController;
import forge.gui.controller.ImportDataController;
import forge.gui.controller.MainWindowController;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class FacadeForgeGuiTest {
    @Nested
    class Singleton {
        @Test
        void returnsSameInstance() {
            assertSame(FacadeForgeGui.getTheInstance(), FacadeForgeGui.getTheInstance());
        }

        @Test
        void exposesAccessObject() {
            assertNotNull(FacadeForgeGui.getTheInstance().forgeGuiAccess());
        }
    }

    @Nested
    class ControllerFactory {
        @Test
        void createsMainWindowController() {
            MainWindowController controller = FacadeForgeGui.getTheInstance()
                    .forgeGuiAccess()
                    .createMainWindowController();

            assertNotNull(controller);
        }

        @Test
        void createsWorkflowControllers() {
            FacadeForgeGui.ForgeGuiAccess access = FacadeForgeGui.getTheInstance().forgeGuiAccess();

            ImportDataController importDataController = access.createImportDataController();
            DerivedDataController derivedDataController = access.createDerivedDataController();
            EventStatisticsController eventStatisticsController = access.createEventStatisticsController();
            BacktestController backtestController = access.createBacktestController();
            BenchmarkController benchmarkController = access.createBenchmarkController();
            DatabaseConfigController databaseConfigController = access.createDatabaseConfigController();

            assertNotNull(importDataController);
            assertNotNull(derivedDataController);
            assertNotNull(eventStatisticsController);
            assertNotNull(backtestController);
            assertNotNull(benchmarkController);
            assertNotNull(databaseConfigController);
        }
    }
}
