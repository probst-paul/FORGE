package forge.gui.controller;

import forge.gui.viewmodel.GuiWorkflowTask;
import forge.gui.viewmodel.GuiWorkflowViewModel;
import javafx.concurrent.Task;

import java.util.function.Consumer;
import java.util.function.Function;

final class GuiControllerTasks {
    private GuiControllerTasks() {
    }

    static <T> Task<T> create(
            GuiWorkflowViewModel viewModel,
            String startingMessage,
            String failureMessage,
            Function<GuiWorkflowTask<T>, T> work,
            Consumer<T> onSucceeded
    ) {
        /*
         * Intent: Standardize JavaFX background task creation for GUI workflows.
         * Precondition: viewModel, work, and success callback must be supplied.
         * Returns: Configured Task<T> that is ready to run on a background thread.
         * Postcondition: The view model is bound to task progress and task failure is reported consistently.
         */
        if (viewModel == null) {
            throw new IllegalArgumentException("viewModel is required");
        }
        if (work == null) {
            throw new IllegalArgumentException("work is required");
        }
        if (onSucceeded == null) {
            throw new IllegalArgumentException("onSucceeded is required");
        }
        GuiWorkflowTask<T> task = new GuiWorkflowTask<>() {
            @Override
            protected T call() {
                publishProgress(0, 1);
                publishStatusMessage(startingMessage);
                return work.apply(this);
            }
        };
        viewModel.bindToTask(task);
        task.setOnSucceeded(event -> onSucceeded.accept(task.getValue()));
        task.setOnFailed(event -> viewModel.markFailed(failureMessage, toRuntimeException(task.getException())));
        return task;
    }

    private static RuntimeException toRuntimeException(Throwable throwable) {
        /*
         * Intent: Normalize JavaFX task failures into runtime exceptions for view model reporting.
         * Precondition: throwable may be null when JavaFX provides no failure cause.
         * Returns: RuntimeException representing the task failure.
         * Postcondition: Failure handling can display a consistent message to the GUI.
         */
        if (throwable instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new RuntimeException(throwable == null ? "Unknown GUI task failure." : throwable.getMessage(), throwable);
    }
}
