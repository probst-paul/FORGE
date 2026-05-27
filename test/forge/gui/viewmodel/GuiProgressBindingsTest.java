package forge.gui.viewmodel;

import forge.app.BacktestProgress;
import forge.app.EventStatisticsProgress;
import forge.app.ImportProgress;
import forge.data.build.DataBuildProgress;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GuiProgressBindingsTest {
    @Nested
    class ImportProgressBinding {
        @Test
        void mapsImportProgressToWorkflowProperties() {
            GuiWorkflowViewModel viewModel = new GuiWorkflowViewModel();

            GuiProgressBindings.importProgress(viewModel, "Importing")
                    .onProgress(new ImportProgress("ESU25", 25, 100));

            assertEquals(25, viewModel.getProcessedUnits());
            assertEquals(100, viewModel.getTotalUnits());
            assertEquals(0.25, viewModel.getProgress());
            assertEquals("Importing ESU25...", viewModel.getStatusMessage());
        }
    }

    @Nested
    class DataBuildProgressBinding {
        @Test
        void mapsDataBuildProgressToWorkflowProperties() {
            GuiWorkflowViewModel viewModel = new GuiWorkflowViewModel();

            GuiProgressBindings.dataBuildProgress(viewModel, "Building derived data...")
                    .onProgress(new DataBuildProgress(50, 200));

            assertEquals(50, viewModel.getProcessedUnits());
            assertEquals(200, viewModel.getTotalUnits());
            assertEquals(0.25, viewModel.getProgress());
            assertEquals("Building derived data...", viewModel.getStatusMessage());
        }
    }

    @Nested
    class EventStatisticsProgressBinding {
        @Test
        void mapsEventStatisticsProgressToWorkflowProperties() {
            GuiWorkflowViewModel viewModel = new GuiWorkflowViewModel();

            GuiProgressBindings.eventStatisticsProgress(viewModel, "Running event statistics...")
                    .onProgress(new EventStatisticsProgress(75, 300));

            assertEquals(75, viewModel.getProcessedUnits());
            assertEquals(300, viewModel.getTotalUnits());
            assertEquals(0.25, viewModel.getProgress());
            assertEquals("Running event statistics...", viewModel.getStatusMessage());
        }
    }

    @Nested
    class BacktestProgressBinding {
        @Test
        void mapsBacktestProgressToWorkflowProperties() {
            GuiWorkflowViewModel viewModel = new GuiWorkflowViewModel();

            GuiProgressBindings.backtestProgress(viewModel, "Running backtest...")
                    .onProgress(new BacktestProgress(100, 400));

            assertEquals(100, viewModel.getProcessedUnits());
            assertEquals(400, viewModel.getTotalUnits());
            assertEquals(0.25, viewModel.getProgress());
            assertEquals("Running backtest...", viewModel.getStatusMessage());
        }
    }
}
