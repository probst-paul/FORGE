package forge.gui.viewmodel;

import forge.data.market.ContractTradeWindow;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkflowSpecificViewModelsTest {
    @Nested
    class MainWindow {
        @Test
        void defaultsToImportDataWorkflowAndForgeTitle() {
            MainWindowViewModel viewModel = new MainWindowViewModel();

            assertEquals(GuiWorkflowType.IMPORT_DATA, viewModel.getActiveWorkflow());
            assertEquals("FORGE", viewModel.getWindowTitle());
        }

        @Test
        void rejectsMissingWorkflowAndNormalizesTitle() {
            MainWindowViewModel viewModel = new MainWindowViewModel();

            assertThrows(IllegalArgumentException.class, () -> viewModel.setActiveWorkflow(null));
            viewModel.setWindowTitle("  Custom FORGE  ");
            assertEquals("Custom FORGE", viewModel.getWindowTitle());
            viewModel.setWindowTitle(" ");
            assertEquals("FORGE", viewModel.getWindowTitle());
        }
    }

    @Nested
    class ImportData {
        @Test
        void storesImportSummaryValues() {
            ImportDataViewModel viewModel = new ImportDataViewModel();

            viewModel.setScidFilePath("/tmp/ESU25.scid");
            viewModel.setContractSymbol("ESU25");
            viewModel.setTableName("ESU25");
            viewModel.setRebuildExistingContract(true);
            viewModel.setRowsImported(10);
            viewModel.setNullSideRowsImported(2);
            viewModel.setSkippedOutsideFrontMonthRows(1);

            assertEquals("/tmp/ESU25.scid", viewModel.getScidFilePath());
            assertEquals("ESU25", viewModel.getContractSymbol());
            assertEquals("ESU25", viewModel.getTableName());
            assertEquals(10, viewModel.getRowsImported());
            assertEquals(2, viewModel.getNullSideRowsImported());
            assertEquals(1, viewModel.getSkippedOutsideFrontMonthRows());
        }
    }

    @Nested
    class ContractWindowWorkflows {
        @Test
        void copyContractWindowsForEventStatisticsAndBacktest() {
            ContractTradeWindow window = new ContractTradeWindow(
                    "ESU25",
                    LocalDate.of(2025, 9, 1),
                    LocalDate.of(2025, 9, 30)
            );
            List<ContractTradeWindow> windows = List.of(window);

            EventStatisticsViewModel eventStatistics = new EventStatisticsViewModel();
            BacktestViewModel backtest = new BacktestViewModel();

            eventStatistics.setContractWindows(windows);
            backtest.setContractWindows(windows);

            assertEquals(windows, eventStatistics.getContractWindows());
            assertEquals(windows, backtest.getContractWindows());
            assertThrows(UnsupportedOperationException.class, () -> eventStatistics.getContractWindows().add(window));
            assertThrows(UnsupportedOperationException.class, () -> backtest.getContractWindows().add(window));
        }
    }

    @Nested
    class NegativeCounters {
        @Test
        void rejectNegativeCounters() {
            assertThrows(IllegalArgumentException.class, () -> new EventStatisticsViewModel().setContractResultCount(-1));
            assertThrows(IllegalArgumentException.class, () -> new BacktestViewModel().setTicksProcessed(-1));
            assertThrows(IllegalArgumentException.class, () -> new ImportDataViewModel().setRowsImported(-1));
        }
    }
}
