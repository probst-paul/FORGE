package forge.gui.controller;

import forge.app.FacadeForgeApplication;
import forge.gui.view.MainWindowView;
import forge.gui.viewmodel.BacktestViewModel;
import forge.gui.viewmodel.EventStatisticsViewModel;
import forge.gui.viewmodel.ImportDataViewModel;
import forge.gui.viewmodel.MainWindowViewModel;
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
        }
    }

    @Nested
    class DependencyConstructors {
        @Test
        void preserveInjectedViewModels() {
            ImportDataViewModel importDataViewModel = new ImportDataViewModel();
            EventStatisticsViewModel eventStatisticsViewModel = new EventStatisticsViewModel();
            BacktestViewModel backtestViewModel = new BacktestViewModel();

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
        }
    }
}
