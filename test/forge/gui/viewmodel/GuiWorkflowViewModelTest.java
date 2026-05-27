package forge.gui.viewmodel;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiWorkflowViewModelTest {
    @Nested
    class Properties {
        @Test
        void normalizesNullTextValues() {
            GuiWorkflowViewModel viewModel = new GuiWorkflowViewModel();

            viewModel.setStatusMessage(null);
            viewModel.setResultSummary(null);
            viewModel.setErrorMessage(null);

            assertEquals("", viewModel.getStatusMessage());
            assertEquals("", viewModel.getResultSummary());
            assertEquals("", viewModel.getErrorMessage());
        }

        @Test
        void updatesProgressFromProcessedAndTotalUnits() {
            GuiWorkflowViewModel viewModel = new GuiWorkflowViewModel();

            viewModel.updateProgress(25, 100);

            assertEquals(25, viewModel.getProcessedUnits());
            assertEquals(100, viewModel.getTotalUnits());
            assertEquals(0.25, viewModel.getProgress());
        }

        @Test
        void rejectsInvalidProgressValues() {
            GuiWorkflowViewModel viewModel = new GuiWorkflowViewModel();

            assertThrows(IllegalArgumentException.class, () -> viewModel.setProgress(-0.01));
            assertThrows(IllegalArgumentException.class, () -> viewModel.setProgress(1.01));
            assertThrows(IllegalArgumentException.class, () -> viewModel.updateProgress(-1, 1));
            assertThrows(IllegalArgumentException.class, () -> viewModel.updateProgress(1, -1));
            assertThrows(IllegalArgumentException.class, () -> viewModel.updateProgress(2, 1));
        }
    }

    @Nested
    class StateTransitions {
        @Test
        void markStartedResetsWorkflowState() {
            GuiWorkflowViewModel viewModel = new GuiWorkflowViewModel();
            viewModel.setResultSummary("old result");
            viewModel.setErrorMessage("old error");

            viewModel.markStarted("Running...");

            assertTrue(viewModel.isRunning());
            assertEquals(0.0, viewModel.getProgress());
            assertEquals(0, viewModel.getProcessedUnits());
            assertEquals(0, viewModel.getTotalUnits());
            assertEquals("Running...", viewModel.getStatusMessage());
            assertEquals("", viewModel.getResultSummary());
            assertEquals("", viewModel.getErrorMessage());
        }

        @Test
        void markSucceededCompletesWorkflowState() {
            GuiWorkflowViewModel viewModel = new GuiWorkflowViewModel();
            viewModel.updateProgress(1, 4);

            viewModel.markSucceeded("Done.", "result");

            assertFalse(viewModel.isRunning());
            assertEquals(1.0, viewModel.getProgress());
            assertEquals(4, viewModel.getProcessedUnits());
            assertEquals(4, viewModel.getTotalUnits());
            assertEquals("Done.", viewModel.getStatusMessage());
            assertEquals("result", viewModel.getResultSummary());
            assertEquals("", viewModel.getErrorMessage());
        }

        @Test
        void markFailedCapturesErrorMessage() {
            GuiWorkflowViewModel viewModel = new GuiWorkflowViewModel();

            viewModel.markFailed("Failed.", new RuntimeException("boom"));

            assertFalse(viewModel.isRunning());
            assertEquals("Failed.", viewModel.getStatusMessage());
            assertEquals("boom", viewModel.getErrorMessage());
        }
    }
}
