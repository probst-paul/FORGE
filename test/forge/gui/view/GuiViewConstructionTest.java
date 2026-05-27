package forge.gui.view;

import forge.gui.controller.ImportDataController;
import forge.gui.controller.DatabaseConfigController;
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
            assertNotNull(new BacktestView());
            assertNotNull(new BenchmarkView());
            assertNotNull(new DerivedDataView());
            assertNotNull(new EventStatisticsView());
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
