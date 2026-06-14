package forge.gui.controller;

import forge.app.FacadeForgeApplication;
import forge.benchmark.FacadeForgeBenchmark;
import forge.gui.viewmodel.BenchmarkViewModel;
import forge.gui.view.MainWindowView;
import forge.gui.viewmodel.BacktestViewModel;
import forge.gui.viewmodel.EventStatisticsViewModel;
import forge.gui.viewmodel.ImportDataViewModel;
import forge.gui.viewmodel.MainWindowViewModel;
import forge.gui.viewmodel.SettingsViewModel;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GuiControllerConstructionTest {
    @Nested
    class DefaultConstructors {
        @Test
        void createControllersWithViewModels() {
            assertNotNull(new ImportDataController().getViewModel());
            assertNotNull(new EventStatisticsController().getViewModel());
            assertNotNull(new BacktestController().getViewModel());
            assertNotNull(new SettingsController().getViewModel());
            assertNotNull(new BenchmarkController().getViewModel());
        }
    }

    @Nested
    class DependencyConstructors {
        @Test
        void preserveInjectedViewModels() {
            ImportDataViewModel importDataViewModel = new ImportDataViewModel();
            EventStatisticsViewModel eventStatisticsViewModel = new EventStatisticsViewModel();
            BacktestViewModel backtestViewModel = new BacktestViewModel();
            SettingsViewModel settingsViewModel = new SettingsViewModel();
            BenchmarkViewModel benchmarkViewModel = new BenchmarkViewModel();

            assertSame(importDataViewModel, new ImportDataController(
                    FacadeForgeApplication.getTheInstance(),
                    importDataViewModel
            ).getViewModel());
            assertSame(eventStatisticsViewModel, new EventStatisticsController(
                    FacadeForgeApplication.getTheInstance(),
                    eventStatisticsViewModel
            ).getViewModel());
            assertSame(backtestViewModel, new BacktestController(
                    FacadeForgeApplication.getTheInstance(),
                    backtestViewModel
            ).getViewModel());
            assertSame(settingsViewModel, new SettingsController(
                    FacadeForgeApplication.getTheInstance(),
                    settingsViewModel
            ).getViewModel());
            assertSame(benchmarkViewModel, new BenchmarkController(
                    FacadeForgeBenchmark.getTheInstance(),
                    benchmarkViewModel
            ).getViewModel());
        }

        @Test
        void rejectMissingDependencies() {
            assertThrows(IllegalArgumentException.class, () -> new ImportDataController(null, new ImportDataViewModel()));
            assertThrows(IllegalArgumentException.class, () -> new ImportDataController(
                    FacadeForgeApplication.getTheInstance(),
                    null
            ));
            assertThrows(IllegalArgumentException.class, () -> new MainWindowController(null, new MainWindowView()));
            assertThrows(IllegalArgumentException.class, () -> new MainWindowController(new MainWindowViewModel(), null));
            assertThrows(IllegalArgumentException.class, () -> new SettingsController(null, new SettingsViewModel()));
            assertThrows(IllegalArgumentException.class, () -> new SettingsController(
                    FacadeForgeApplication.getTheInstance(),
                    null
            ));
            assertThrows(IllegalArgumentException.class, () -> new BenchmarkController(null, new BenchmarkViewModel()));
            assertThrows(IllegalArgumentException.class, () -> new BenchmarkController(
                    FacadeForgeBenchmark.getTheInstance(),
                    null
            ));
        }
    }
}
