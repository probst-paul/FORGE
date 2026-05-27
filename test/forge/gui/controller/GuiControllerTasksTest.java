package forge.gui.controller;

import forge.gui.viewmodel.GuiWorkflowViewModel;
import javafx.concurrent.Task;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GuiControllerTasksTest {
    @Nested
    class Create {
        @Test
        void createsTaskBoundToViewModel() {
            GuiWorkflowViewModel viewModel = new GuiWorkflowViewModel();

            Task<String> task = GuiControllerTasks.create(
                    viewModel,
                    "Starting...",
                    "Failed.",
                    workflowTask -> {
                        workflowTask.publishProgress(1, 2);
                        workflowTask.publishStatusMessage("Halfway...");
                        return "done";
                    },
                    result -> viewModel.markSucceeded("Complete.", result)
            );

            assertNotNull(task);
            assertEquals(-1.0, viewModel.getProgress());
        }

        @Test
        void rejectsMissingArguments() {
            GuiWorkflowViewModel viewModel = new GuiWorkflowViewModel();

            assertThrows(IllegalArgumentException.class, () -> GuiControllerTasks.<String>create(
                    null,
                    "Starting...",
                    "Failed.",
                    task -> "done",
                    result -> viewModel.markSucceeded("Complete.", result)
            ));
            assertThrows(IllegalArgumentException.class, () -> GuiControllerTasks.<String>create(
                    viewModel,
                    "Starting...",
                    "Failed.",
                    null,
                    result -> viewModel.markSucceeded("Complete.", result)
            ));
            assertThrows(IllegalArgumentException.class, () -> GuiControllerTasks.<String>create(
                    viewModel,
                    "Starting...",
                    "Failed.",
                    task -> "done",
                    null
            ));
        }
    }
}
