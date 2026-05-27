package forge.gui.view;

import forge.gui.controller.ImportDataController;
import forge.gui.controller.BacktestController;
import forge.gui.controller.DatabaseConfigController;
import forge.gui.controller.DerivedDataController;
import forge.gui.controller.EventStatisticsController;
import forge.gui.viewmodel.MainWindowViewModel;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GuiViewConstructionTest {
    @Nested
    class PlaceholderViews {
        @Test
        void canConstructWorkflowViews() {
            assertNotNull(new BenchmarkView());
        }
    }

    @Nested
    class Backtest {
        @Test
        void requiresController() {
            assertThrows(IllegalArgumentException.class, () -> new BacktestView(null));
        }

        @Test
        void canConstructWithController() {
            assertNotNull(new BacktestView(new BacktestController()));
        }
    }

    @Nested
    class ImportData {
        @Test
        void requiresController() {
            assertThrows(IllegalArgumentException.class, () -> new ImportDataView(null));
        }

        @Test
        void canConstructWithController() {
            assertNotNull(new ImportDataView(new ImportDataController()));
        }
    }

    @Nested
    class EventStatistics {
        @Test
        void requiresController() {
            assertThrows(IllegalArgumentException.class, () -> new EventStatisticsView(null));
        }

        @Test
        void canConstructWithController() {
            assertNotNull(new EventStatisticsView(new EventStatisticsController()));
        }
    }

    @Nested
    class DerivedData {
        @Test
        void requiresController() {
            assertThrows(IllegalArgumentException.class, () -> new DerivedDataView(null));
        }

        @Test
        void canConstructWithController() {
            assertNotNull(new DerivedDataView(new DerivedDataController()));
        }
    }

    @Nested
    class DatabaseConfig {
        @Test
        void requiresController() {
            assertThrows(IllegalArgumentException.class, () -> new DatabaseConfigView(null));
        }

        @Test
        void canConstructWithController() {
            assertNotNull(new DatabaseConfigView(new DatabaseConfigController()));
        }
    }

    @Nested
    class MainWindow {
        @Test
        void legacyRenderPrintsStatusMessage() {
            MainWindowViewModel viewModel = new MainWindowViewModel();
            viewModel.setStatusMessage("Ready.");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;

            try {
                System.setOut(new PrintStream(output));
                new MainWindowView().render(viewModel);
            } finally {
                System.setOut(originalOut);
            }

            assertEquals("Ready." + System.lineSeparator(), output.toString());
        }

        @Test
        void legacyRenderRequiresViewModel() {
            assertThrows(IllegalArgumentException.class, () -> new MainWindowView().render(null));
        }
    }
}
