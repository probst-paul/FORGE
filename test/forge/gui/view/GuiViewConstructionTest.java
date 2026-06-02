package forge.gui.view;

import forge.gui.controller.ImportDataController;
import forge.gui.controller.BacktestController;
import forge.gui.controller.EventStatisticsController;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GuiViewConstructionTest {
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

}
